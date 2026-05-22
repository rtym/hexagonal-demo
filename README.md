# Hexagonal Architecture Demo

A runnable Spring Boot 3 project demonstrating **clean hexagonal (ports & adapters)
architecture** with **hot-pluggable plugins**, served entirely via Docker Compose.

The headline feature is `plugin-bundle`: a single JAR that ships all three generators
(carrot, rabbit, cabbage) in two versions. Dropping **v2** into the `plugins/` directory
while the app is running hot-swaps all three plugins in ~200 ms — no restart, no recompile.

---

## Architecture

### What is hexagonal (ports & adapters) architecture?

Hexagonal architecture, coined by Alistair Cockburn, organises a system around a
**central application core** that contains all business logic. The core knows nothing
about the outside world — it only speaks through **ports** (Java interfaces). Concrete
**adapters** plug into those ports to connect the core to files, HTTP endpoints,
and in-memory storage.

```
                          ┌───────────────────────────┐
                          │      APPLICATION CORE      │
                          │                            │
  ┌─────────┐  «uses»     │  ┌──────────────────────┐ │
  │  File   │ ──────────▶ │  │  ProcessMessage      │ │
  │ Adapter │             │  │  UseCase             │ │
  └─────────┘             │  │                      │ │
  (in)                    │  │  GetProcessedMessages│ │
                          │  │  Query               │ │
  ┌─────────┐  «uses»     │  └──────────────────────┘ │
  │  REST   │ ──────────▶ │                            │
  │ Adapter │◀──────────  │  ┌──────────────────────┐ │
  └─────────┘  (browser)  │  │  ImageGeneratorPort  │ │
  (in/out)                │  │  MessageStoragePort  │ │
                          │  └──────────┬───────────┘ │
                          └────────────┼──────────────┘
                                       │  «implements»
                          ┌────────────▼──────────────┐
                          │      Driven Adapters       │
                          │  In-Memory │  PluginReg.   │
                          └───────────────────────────┘
```

**Key rule:** dependency arrows always point inward. Adapters depend on ports; the core
never depends on adapters. This makes the core independently testable with mock ports and
lets you swap any adapter without touching business logic.

### Layered dependency flow

```
plugin-api    ──▶  (no deps)
core          ──▶  plugin-api, Spring Boot
plugin-bundle ──▶  plugin-api (provided scope — NOT bundled in the JAR)
```

`plugin-bundle` declares `plugin-api` as `<scope>provided</scope>` so the interface class
is resolved from the host application's classloader at runtime, preventing
`ClassCastException` across classloader boundaries.

### Request flow — end-to-end

```
  File dropped into input/
         │
  FileMessageAdapter      (@Scheduled poll every 5 s)
         │  ProcessMessageUseCase.processMessage()
  MessageProcessingService
         ├── ImageGeneratorPort.generateImage(type)
         │       └── PluginRegistry  ──▶  hot-loaded plugin JAR
         └── MessageStoragePort.save(result)
                 └── InMemoryMessageStorageAdapter

  Browser polls GET /api/messages every 4 s
         └── MessageController  ──▶  GetProcessedMessagesQuery
                                         └── InMemoryMessageStorageAdapter
```

### Hot-loading sequence

```
  plugins/ directory
       │  ENTRY_CREATE (new JAR dropped)
       ▼
  PluginWatcher  (NIO WatchService, background thread)
       │  URLClassLoader(jarUrl, parent = appClassLoader)
       ▼
  ServiceLoader.load(ImageGeneratorPlugin.class, childLoader)
       │  for each discovered implementation
       ▼
  PluginRegistry.register(plugin)
       │  ConcurrentMap.put(type, plugin)  ← atomic hot-swap
       ▼
  Next generateImage() call returns the new art immediately
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
│                                 │  Use Case     │        │  In-Memory   │   │
│  ┌─────────────┐                │               │        │  Adapter     │   │
│  │ REST Adapter│──────────────▶│  ────────────  │        └──────────────┘   │
│  │  (browser)  │◀──────────────│               │                            │
│  └─────────────┘                │  Image        │──────▶ ┌──────────────┐   │
│                                 │  Generator    │        │ Plugin       │   │
│                                 │  Port         │        │ Registry     │   │
│                                 └───────────────┘        │ (hot-load)   │   │
│                                        ▲                  └──────────────┘   │
│                                  ports (interfaces)       plugins/*.jar      │
└───────────────────────────────────────────────────────────────────────────── ┘
```

### Module layout

| Module | Role |
|--------|------|
| `plugin-api` | Shared `ImageGeneratorPlugin` interface — zero runtime deps |
| `core` | Spring Boot app: domain, ports, adapters (File / REST / In-Memory), plugin infrastructure |
| `plugin-bundle` | Single JAR with all three plugins in **two versions** (v1 = original art, v2 = enhanced art) |
| `frontend/` | S3-hostable static page (also served by the app at `/`) |
| `scripts/` | `build-plugins.sh` — builds both JARs; `demo-hotswap.sh` — interactive walkthrough |

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
│   └── rest              MessageController         (GET+POST /api/messages, GET /api/plugins)
├── adapter.out
│   └── memory            InMemoryMessageStorageAdapter  (bounded deque, newest-first)
└── infrastructure
    └── plugin            PluginWatcher + PluginRegistry
```

---

## Quick start

### Prerequisites

- Java 17+ and Maven 3.9+ (to build plugin JARs on the host)
- Docker + Docker Compose

### 1 — Build plugin JARs on the host

```bash
scripts/build-plugins.sh
```

Produces:
- `plugin-bundle/target/plugin-bundle-v1.jar` — original ASCII art
- `plugin-bundle/target/plugin-bundle-v2.jar` — enhanced ASCII art

### 2 — Start the application

```bash
docker compose up --build
```

The image is built once; subsequent starts use the cached image (`docker compose up`).

### 3 — Load the v1 plugin

```bash
cp plugin-bundle/target/plugin-bundle-v1.jar plugins/
```

The PluginWatcher detects the new JAR within ~200 ms and registers all three plugins.
Verify: `curl -s http://localhost:8080/api/plugins | python3 -m json.tool`

### 4 — Drop an input file

```bash
echo "carrot"  > input/demo.txt
# or multiple types:
printf "carrot\nrabbit\ncabbage\n" > input/demo.txt
```

The FileAdapter picks it up within 5 seconds, generates ASCII art, and stores it.

### 5 — View the result

Open **http://localhost:8080** — cards appear for each processed word.

---

## Hot plug-and-play demo

### Why it works

`PluginRegistry` stores plugins in a `ConcurrentHashMap<type, plugin>`. Calling
`register()` with a new plugin for the same type atomically replaces the old entry.
Because v1 and v2 JARs have **different filenames**, dropping v2 triggers a new
`ENTRY_CREATE` event and a fresh `URLClassLoader` — the app never reloads v1.

### Step-by-step

```bash
# Terminal 1 — app already running via docker compose up

# Terminal 2
cp plugin-bundle/target/plugin-bundle-v1.jar plugins/
echo "carrot" > input/step1.txt
# Open http://localhost:8080 — you should see the v1 carrot art

# Hot-swap: drop v2 (no restart)
cp plugin-bundle/target/plugin-bundle-v2.jar plugins/
echo "carrot" > input/step2.txt
# Refresh http://localhost:8080 — the new card shows v2 art
```

Or run the interactive script:

```bash
scripts/demo-hotswap.sh
```

### What changes between v1 and v2

| Plugin | v1 | v2 |
|--------|----|----|
| Carrot | Classic vegetable art | Wider block art with `v2.0` label |
| Rabbit | Simple face | Two-rabbit "fluffy ears" design |
| Cabbage | Leaf swirl | `@`-leaf pattern with `v2.0` label |

---

## How to send messages

### Via file drop (main demo path)

```bash
printf "carrot\nrabbit\ncabbage\n" > input/demo.txt
```

One word per line. The FileAdapter processes the file and moves it to `input/processed/`.

### Via the browser UI

`http://localhost:8080` → type a word in the input box → click **Send**.

### Via REST

```bash
curl -X POST http://localhost:8080/api/messages \
     -H "Content-Type: text/plain" \
     -d "rabbit"
```

---

## REST API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/messages` | All processed messages, newest first (max 200) |
| `POST` | `/api/messages` | Submit a message (`Content-Type: text/plain`) |
| `GET` | `/api/plugins` | Currently loaded plugin types and count |

---

## Running tests

```bash
# All modules — no Docker or AWS required
mvn test

# Only core
mvn test -pl core

# Only plugin-bundle
mvn test -pl plugin-bundle
```

### Test coverage

| Test class | What is covered |
|------------|-----------------|
| `InMemoryMessageStorageAdapterTest` | Save/find, newest-first ordering, bounded cap (200), snapshot immutability |
| `BundlePluginTest` | All 6 implementations: `supports()`, non-empty art, label presence, v1≠v2 |
| `PluginRegistryTest` | Register/lookup/replace, empty-registry behaviour, hot-swap (atomic put) |
| `MessageProcessingServiceTest` | Happy path, unknown-type placeholder, lowercase normalisation, ID preservation |
| `MessageControllerTest` | All three REST endpoints — status codes, delegation, whitespace trimming |

---

## Writing a custom plugin

1. Create a Maven project that depends on `plugin-api` with `<scope>provided</scope>`.
2. Implement `ImageGeneratorPlugin` — `supports()`, `generate()`, optionally `getPluginName()`.
3. Register your class in `META-INF/services/com.example.hexdemo.plugin.ImageGeneratorPlugin`.
4. Build a JAR (`mvn package`).
5. Copy it into `plugins/` while the app is running. PluginWatcher hot-loads it instantly.
6. Call `GET /api/plugins` to verify the new type appeared.
7. Drop a file with your type name and watch the card render in the UI.

> Note: `PluginRegistry.register()` currently probes `{carrot, rabbit, cabbage}`.
> To support a new type, add it to that list in `PluginRegistry.java`.

---

## Design principles demonstrated

| Principle | Where |
|-----------|-------|
| Dependency Inversion | `MessageProcessingService` depends only on port interfaces |
| Single Responsibility | Each adapter handles exactly one I/O concern |
| Open/Closed | New message sources → new adapter; core never changes |
| Plugin extensibility | New image types → drop a JAR; no restart, no recompile |
| Classloader isolation | Child `URLClassLoader` per JAR prevents symbol leakage |
| Thread safety | `ConcurrentHashMap` in registry; `ConcurrentLinkedDeque` in storage |
| Testability | Core tested with mock ports — no Docker required |
