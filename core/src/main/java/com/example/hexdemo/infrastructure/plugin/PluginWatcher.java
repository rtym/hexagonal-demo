package com.example.hexdemo.infrastructure.plugin;

import com.example.hexdemo.plugin.ImageGeneratorPlugin;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.ServiceLoader;

/**
 * Watches the {@code plugins/} directory for new JAR files and hot-loads them
 * without restarting the application.
 *
 * Loading sequence for each new JAR:
 *   1. Create a child URLClassLoader (parent = application classloader).
 *   2. ServiceLoader discovers {@link ImageGeneratorPlugin} implementations via
 *      META-INF/services entries inside the JAR.
 *   3. Each discovered plugin is registered in {@link PluginRegistry}.
 *
 * Because plugin-api is a "provided" dependency in plugin JARs, the
 * ImageGeneratorPlugin interface is resolved from the parent classloader —
 * preventing ClassCastException.
 *
 * On startup, all existing JARs in the directory are loaded first.
 */
@Component
public class PluginWatcher {

    private static final Logger log = LoggerFactory.getLogger(PluginWatcher.class);

    private final PluginRegistry pluginRegistry;
    private final Path pluginsDir;
    private volatile WatchService watchService;
    private volatile Thread watchThread;

    public PluginWatcher(PluginRegistry pluginRegistry,
                         @Value("${infrastructure.plugins.directory:./plugins}") String pluginsDirectory) {
        this.pluginRegistry = pluginRegistry;
        this.pluginsDir = Paths.get(pluginsDirectory);
    }

    @PostConstruct
    public void start() throws IOException {
        Files.createDirectories(pluginsDir);

        // Load any JARs already present in the directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.jar")) {
            for (Path jar : stream) {
                loadJar(jar);
            }
        }

        // Start background watcher for new JARs dropped at runtime
        watchService = FileSystems.getDefault().newWatchService();
        pluginsDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        watchThread = new Thread(this::watchLoop, "plugin-watcher");
        watchThread.setDaemon(true);
        watchThread.start();

        log.info("PluginWatcher started — monitoring {}", pluginsDir.toAbsolutePath());
    }

    @PreDestroy
    public void stop() {
        if (watchService != null) {
            try { watchService.close(); } catch (IOException ignored) {}
        }
    }

    private void watchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    Path filename = (Path) event.context();
                    if (filename.toString().endsWith(".jar")) {
                        Path fullPath = pluginsDir.resolve(filename);
                        // Give the file system a moment to finish writing before loading
                        try { Thread.sleep(200); } catch (InterruptedException e) { break; }
                        loadJar(fullPath);
                    }
                }
            }
            key.reset();
        }
        log.info("PluginWatcher stopped.");
    }

    private void loadJar(Path jarPath) {
        log.info("Loading plugin JAR: {}", jarPath.getFileName());
        try {
            URL jarUrl = jarPath.toUri().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarUrl},
                    Thread.currentThread().getContextClassLoader()
            );

            ServiceLoader<ImageGeneratorPlugin> loader =
                    ServiceLoader.load(ImageGeneratorPlugin.class, classLoader);

            int count = 0;
            for (ImageGeneratorPlugin plugin : loader) {
                pluginRegistry.register(plugin);
                count++;
            }

            if (count == 0) {
                log.warn("JAR {} contained no ImageGeneratorPlugin implementations", jarPath.getFileName());
            } else {
                log.info("Loaded {} plugin(s) from {}", count, jarPath.getFileName());
            }
        } catch (Exception e) {
            log.error("Failed to load plugin JAR {}: {}", jarPath.getFileName(), e.getMessage(), e);
        }
    }
}
