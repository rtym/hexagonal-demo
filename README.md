# Hexagonal Architecture Demo

A runnable Spring Boot 3 project demonstrating **clean hexagonal (ports & adapters)
architecture** with **hot-pluggable plugins** and **local AWS services** via LocalStack.

The headline feature is `plugin-bundle`: a single JAR that ships two versions of ASCII-art
generators (v1 and v2). Dropping v2 into the `plugins/` directory while the application is
running hot-swaps all three plugins in ~200 ms — no restart, no recompile.

---

## Architecture

### What is hexagonal (ports & adapters) architecture?

Hexagonal architecture, coined by Alistair Cockburn, organises a system around a
**central application core** that contains all business logic. The core knows nothing
about the outside world — it only speaks through **ports** (Java interfaces). Concrete
**adapters** plug into those ports to connect the core to databases, message brokers,
HTTP endpoints, and file systems.

```
                          ┌───────────────────────────┐
                          │      APPLICATION CORE      │
                          │                            │
  ┌─────────┐  «uses»     │  ┌──────────────────────┐ │
  │ Driving │ ──────────▶ │  │  ProcessMessage      │ │
  │ Adapter │             │  │  UseCase             │ │
  └─────────┘             │  │                      │ │
  (e.g. REST)             │  │  GetProcessedMessages│ │
                          │  │  Query               │ │
  ┌─────────┐  «uses»     │  └──────────────────────┘ │
  │ Driving │ ──────────▶ │                            │
  │ Adapter │             │  ┌──────────────────────┐ │
  └─────────┘             │  │  ImageGeneratorPort  │ │
  (e.g. SQS)              │  │  MessageStoragePort  │ │
                          │  └──────────┬───────────┘ │
                          └────────────┼──────────────┘
                                       │  «implements»
                          ┌────────────▼──────────────┐
                          │      Driven Adapters       │
                          │  DynamoDB  │  PluginReg.   │
                          └───────────────────────────┘
```

Key rule: **dependency arrows always point inward**. Adapters depend on ports; the core never
depends on adapters. This makes the core independently testable (mock the ports) and lets you
swap any adapter without touching business logic.

### Layered dependency flow

```
plugin-api  ──▶  (no deps)
core        ──▶  plugin-api, Spring Boot, AWS SDK v2
plugin-bundle ──▶  plugin-api (provided scope)
```

`plugin-bundle` declares `plugin-api` as `<scope>provided</scope>` so the interface class
is NOT bundled inside the plugin JAR. At runtime the plugin classloader delegates to the
host application's classloader for interface resolution, which is why
`ServiceLoader.load(ImageGeneratorPlugin.class, pluginClassLoader)` works without a
`ClassCastException`.

### Request flow — end-to-end

```
                           HTTP POST /api/messages
                                    │
                          MessageController (adapter.in.rest)
                                    │  calls port
                          ProcessMessageUseCase (port.in)
                                    │  implemented by
                          MessageProcessingService (application.service)
                          ┌─────────┴──────────────────────┐
                          │                                 │
               ImageGeneratorPort (port.out)   MessageStoragePort (port.out)
                          │                                 │
               PluginRegistry (infrastructure)  DynamoDbMessageStorageAdapter
                          │                         (adapter.out.dynamodb)
               hot-loaded plugin JAR
```

### Hot-loading sequence

```
  plugins/ directory
       │
       │  ENTRY_CREATE (new JAR dropped)
       ▼
  PluginWatcher (NIO WatchService)
       │
       │  URLClassLoader(jarUrl, parent=appClassLoader)
       ▼
  ServiceLoader.load(ImageGeneratorPlugin.class, childLoader)
       │
       │  for each discovered implementation
       ▼
  PluginRegistry.register(plugin)
       │
       │  ConcurrentMap.put(type, plugin)   ← atomic hot-swap
       ▼
  Next generateImage() call returns new art
```

---

## Architecture overview

```
┌──────────────────── HEXAGONAL ARCHITECTURE ─────────────────────────────────┐
│                                                                               │
│  DRIVING (in) ADAPTERS          APPLICATION CORE         DRIVEN (out)        │
│                                                           ADAPTERS            │
│  ┌─────────────┐                ┌───────────────┐                            │
│  │ File Adapter│──────────────▶│  Process      │                            │
│  └─────────────┘                │  Message      │──────▶ ┌──────────────┐   │
│                                 │  Use Case     │        │  DynamoDB    │   │
│  ┌─────────────┐                │               │        │  Adapter     │   │
│  │ SQS Adapter │──────────────▶│  ────────────  │        └──────────────┘   │
│  └─────────────┘                │               │                            │
│                                 │  Image        │──────▶ ┌──────────────┐   │
│  ┌─────────────┐                │  Generator    │        │ Plugin       │   │
│  │ REST Adapter│──────────────▶│  Port         │        │ Registry     │   │
│  │  (browser)  │◀──────────────│               │        │ (hot-load)   │   │
│  └─────────────┘                └───────────────┘        └──────────────┘   │
│                                        ▲                        ▲            │
│                                  ports (interfaces)       plugins/*.jar      │
└───────────────────────────────────────────────────────────────────────────── ┘
```

### Module layout

| Module | Role |
|--------|------|
| `plugin-api` | Shared `ImageGeneratorPlugin` interface — zero runtime deps |
| `core` | Spring Boot app: domain, ports, adapters (File / SQS / REST / DynamoDB), plugin infrastructure |
| `plugin-bundle` | Single JAR with all three plugins in **two versions** (v1 = original art, v2 = enhanced art) |
| `frontend/` | S3-hostable static page; polls `GET /api/messages` every 4 s |
| `scripts/` | `build-plugins.sh` — builds both JARs; `demo-hotswap.sh` — interactive demo |

### Key packages inside `core`

```
com.example.hexdemo
├── domain.model          Message, ProcessedMessage  (pure Java records)
├── application
│   ├── port.in           ProcessMessageUseCase, GetProcessedMessagesQuery
│   ├── port.out          MessageStoragePort, ImageGeneratorPort
│   └── service           MessageProcessingService  (depends only on ports)
├── adapter.in
│   ├── file              FileMessageAdapter        (polls ./input/*.txt)
│   ├── sqs               SqsMessageAdapter         (polls SQS queue)
│   └── rest              MessageController         (GET+POST /api/messages, GET /api/plugins)
├── adapter.out
│   └── dynamodb          DynamoDbMessageStorageAdapter
└── infrastructure
    ├── config            AwsConfig                 (SDK v2 beans, LocalStack endpoint)
    └── plugin            PluginWatcher + PluginRegistry
```

### Plugin bundle layout

```
plugin-bundle/
├── pom.xml                              # two Maven profiles: bundle-v1 (default), bundle-v2
└── src/main/java/.../plugin/bundle/
    ├── v1/  CarrotPluginV1              # original ASCII art
    │        RabbitPluginV1
    │        CabbagePluginV1
    └── v2/  CarrotPluginV2              # enhanced ASCII art  ← dropped for the hot-swap demo
             RabbitPluginV2
             CabbagePluginV2
```

Each version has its own `META-INF/services` file so `ServiceLoader` discovers exactly the
right set of implementations for that JAR.

---

## Quick start

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker + Docker Compose

### 1 — Start LocalStack

```bash
docker compose up -d
```

`localstack-init/init-aws.sh` runs automatically and:
- Creates the `messages-queue` SQS queue
- Creates the `processed-messages` DynamoDB table
- Creates the `hexdemo-frontend` S3 bucket with static-website hosting
- Seeds 5 sample messages into SQS

### 2 — Build all modules

```bash
mvn clean package -DskipTests
```

This also builds `plugin-bundle-v1.jar` (the default profile is `bundle-v1`).

### 3 — Build plugin v2 (needed for the hot-swap demo)

```bash
scripts/build-plugins.sh
```

Produces:
- `plugin-bundle/target/plugin-bundle-v1.jar`
- `plugin-bundle/target/plugin-bundle-v2.jar`

### 4 — Load v1 and start the application

```bash
cp plugin-bundle/target/plugin-bundle-v1.jar plugins/
cd core && mvn spring-boot:run
```

Or run the fat JAR directly:

```bash
cp plugin-bundle/target/plugin-bundle-v1.jar plugins/
java -jar core/target/core-1.0.0-SNAPSHOT.jar
```

### 5 — Open the frontend

```
http://localhost:8080
```

The page polls `GET /api/messages` every 4 seconds and renders each processed
message as a card with its ASCII art.

---

## Hot plug-and-play demo

This is the core showcase of the project. Both the `plugins/` directory scan at startup and
the runtime `WatchService` loop are exercised.

### How it works

1. `PluginWatcher` watches `plugins/` using Java NIO `WatchService`.
2. When a new JAR appears it creates a child `URLClassLoader` (parent = app classloader)
   and calls `ServiceLoader.load(ImageGeneratorPlugin.class, childLoader)`.
3. Each discovered plugin is passed to `PluginRegistry.register()`, which does a
   `ConcurrentMap.put()` — atomically replacing any previous plugin for that type.
4. The next request for that type gets the new art. No restart, no downtime.

Because `plugin-api` is declared `<scope>provided</scope>` in `plugin-bundle`, the
`ImageGeneratorPlugin` interface is resolved from the **parent** classloader, preventing
`ClassCastException` across classloader boundaries.

### Step-by-step

**Option A — automated demo script**

```bash
# In one terminal: start the app with v1 already in plugins/
cp plugin-bundle/target/plugin-bundle-v1.jar plugins/
cd core && mvn spring-boot:run

# In a second terminal: run the interactive demo
scripts/demo-hotswap.sh
```

**Option B — manual**

```bash
# Terminal 1 — start the app
cp plugin-bundle/target/plugin-bundle-v1.jar plugins/
cd core && mvn spring-boot:run

# Terminal 2 — send some messages and observe v1 art
curl -s -X POST http://localhost:8080/api/messages \
     -H 'Content-Type: text/plain' -d 'carrot'
curl -s http://localhost:8080/api/messages | python3 -m json.tool

# Hot-swap: drop v2 while the app is running
cp plugin-bundle/target/plugin-bundle-v2.jar plugins/

# Send the same messages again — the art is now v2
curl -s -X POST http://localhost:8080/api/messages \
     -H 'Content-Type: text/plain' -d 'carrot'
curl -s http://localhost:8080/api/messages | python3 -m json.tool
```

The `PluginWatcher` detects the new JAR within ~200 ms (the watcher sleeps briefly to let
the filesystem finish writing before opening the JAR).

### What changes between v1 and v2

| Plugin | v1 | v2 |
|--------|----|----|
| Carrot | Classic vegetable art | Wider block art with `v2.0` label |
| Rabbit | Simple face | Two-rabbit "fluffy ears" design |
| Cabbage | Leaf swirl | `@`-leaf pattern with `v2.0` label |

---

## How to send messages

### Via the browser UI

`http://localhost:8080` → choose a type → click **Send**.

### Via REST

```bash
curl -X POST http://localhost:8080/api/messages \
     -H "Content-Type: text/plain" \
     -d "rabbit"
```

### Via SQS

```bash
aws --endpoint-url=http://localhost:4566 sqs send-message \
    --queue-url http://localhost:4566/000000000000/messages-queue \
    --message-body "carrot"
```

### Via file drop

```bash
printf "carrot\nrabbit\ncabbage\n" > input/demo.txt
```

The File adapter picks it up within 10 seconds and moves it to `input/processed/`.

---

## REST API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/messages` | All processed messages (newest first) |
| `POST` | `/api/messages` | Submit a message (`Content-Type: text/plain`) |
| `GET` | `/api/plugins` | Currently loaded plugin types and count |

---

## Running tests

```bash
# All modules (unit + slice tests, no Docker required)
mvn test

# Only core tests
mvn test -pl core

# Only plugin-bundle tests
mvn test -pl plugin-bundle
```

### Test coverage

| Test class | What is covered |
|------------|-----------------|
| `BundlePluginTest` | All 6 implementations: `supports()` contract, non-empty art, label presence, v1≠v2 difference, `getPluginName()` version tag |
| `PluginRegistryTest` | Register/lookup/replace, empty-registry behaviour, `getSupportedTypes()` immutability, hot-swap (put replaces) |
| `MessageProcessingServiceTest` | Happy path, unknown-type placeholder, lowercase normalisation, ID preservation, timestamp freshness, `getAll()` delegation |
| `MessageControllerTest` | `GET /api/messages`, `POST /api/messages` (202, id, status, delegation, whitespace trim), `GET /api/plugins` (count, types) |

---

## Writing a custom plugin

1. Create a Maven module (or standalone project) that depends on `plugin-api` with `<scope>provided</scope>`.
2. Implement `ImageGeneratorPlugin` — implement `supports()`, `generate()`, and optionally `getPluginName()`.
3. Register your class in `META-INF/services/com.example.hexdemo.plugin.ImageGeneratorPlugin`.
4. Build a fat-JAR (`mvn package`) or plain JAR — `plugin-api` must **not** be bundled.
5. Copy the JAR to `plugins/` while the app is running. `PluginWatcher` hot-loads it.
6. Call `GET /api/plugins` to verify the new type is registered.
7. Send a message of your type and see the art appear in the UI.

> Note: `PluginRegistry.register()` currently probes the fixed set `{carrot, rabbit, cabbage}`.
> To support a custom type, update the candidate list in `PluginRegistry.java`.

---

## S3 static-website hosting (optional)

```bash
aws --endpoint-url=http://localhost:4566 s3 cp frontend/index.html \
    s3://hexdemo-frontend/index.html --content-type text/html
```

Access at: `http://hexdemo-frontend.s3-website.localhost.localstack.cloud:4566`

The frontend `API_BASE` constant points to `http://localhost:8080/api` by default.

---

## Design principles demonstrated

| Principle | Where |
|-----------|-------|
| Dependency Inversion | `MessageProcessingService` depends only on port interfaces, never on adapters |
| Single Responsibility | Each adapter handles exactly one I/O technology |
| Open/Closed | New message sources → new adapter; core never changes |
| Plugin extensibility | New image types → drop a JAR; no restart, no recompile of core |
| Classloader isolation | Child `URLClassLoader` per JAR prevents symbol leakage across plugins |
| Thread safety | `ConcurrentHashMap` in `PluginRegistry`; hot-swap is atomic per key |
| Testability | Core service tested with mock ports — no AWS, no Docker required |
