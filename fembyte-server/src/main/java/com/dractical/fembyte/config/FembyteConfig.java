package com.dractical.fembyte.config;

import io.papermc.paper.configuration.GlobalConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class FembyteConfig {

    public static final Logger LOGGER = LogManager.getLogger("FembyteConfig");

    static final Path CONFIG_DIR = Path.of("config");
    static final String MODULES_PACKAGE = "com.dractical.fembyte.config.modules";
    static final String GLOBAL_CONFIG_FILE = "fembyte-global.yml";

    private static volatile FembyteGlobalConfig globalConfig;

    private static int previousMinorVersion;
    private static int currentMinorVersion;

    private FembyteConfig() {
    }

    public static @NotNull CompletableFuture<Void> reloadAsync(CommandSender sender) {
        return CompletableFuture.runAsync(() -> {
            final long start = System.nanoTime();

            try {
                ConfigModule.clearModules();
                load(false);
                ConfigModule.loadAfterBootstrap();

                final long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                final Component msg = Component.text(
                        "Successfully reloaded Fembyte config in " + elapsedMs + "ms.",
                        NamedTextColor.GREEN
                );
                Command.broadcastCommandMessage(sender, msg);
            } catch (Throwable t) {
                Command.broadcastCommandMessage(
                        sender,
                        Component.text("Failed to reload Fembyte config. See console for details.", NamedTextColor.RED)
                );
                LOGGER.error("Failed to reload config!", t);
            }
        }, Util.ioPool());
    }

    public static void load() {
        final long start = System.nanoTime();
        LOGGER.info("Loading Fembyte configuration...");

        try {
            load(true);
            final long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            LOGGER.info("Successfully loaded Fembyte configuration in {}ms.", elapsedMs);
        } catch (Throwable t) {
            LOGGER.error("Failed to load Fembyte configuration!", t);
        }
    }

    public static FembyteGlobalConfig config() {
        return globalConfig;
    }

    private static void load(boolean initial) throws Exception {
        createDirectory(CONFIG_DIR.toFile());
        globalConfig = new FembyteGlobalConfig(initial);
        ConfigModule.initModules();
    }

    static void createDirectory(File dir) throws IOException {
        try {
            Files.createDirectories(dir.toPath());
        } catch (FileAlreadyExistsException e) {
            if (dir.delete()) {
                createDirectory(dir);
            } else {
                throw e;
            }
        }
    }

    public static @NotNull Set<Class<?>> getClasses(String packageName) {
        final Set<Class<?>> classes = new LinkedHashSet<>();
        final String packagePath = packageName.replace('.', '/');

        try {
            final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);

            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                final String protocol = url.getProtocol();

                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
                    findClassesInFile(packageName, filePath, classes);
                } else if ("jar".equals(protocol)) {
                    try {
                        JarURLConnection connection = (JarURLConnection) url.openConnection();
                        try (JarFile jar = connection.getJarFile()) {
                            findClassesInJar(packageName, jar.entries(), packagePath, classes);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Failed to scan jar for config modules from {}", url, e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan classpath for config modules.", e);
        }

        return classes;
    }

    private static void findClassesInFile(String packageName, String packagePath, Set<Class<?>> classes) {
        File dir = new File(packagePath);
        if (!dir.isDirectory()) return;

        File[] candidates = dir.listFiles(
                file -> file.isDirectory() || file.getName().endsWith(".class")
        );
        if (candidates == null) return;

        for (File file : candidates) {
            if (file.isDirectory()) {
                findClassesInFile(packageName + "." + file.getName(), file.getAbsolutePath(), classes);
                continue;
            }

            final String simpleName = file.getName().substring(0, file.getName().length() - 6);
            final String fqcn = packageName + '.' + simpleName;

            try {
                classes.add(Class.forName(fqcn, false, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Failed to load config module class {}", fqcn, e);
            }
        }
    }

    private static void findClassesInJar(
            String packageName,
            Enumeration<JarEntry> entries,
            String packageDirName,
            Set<Class<?>> classes
    ) {
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.charAt(0) == '/') {
                name = name.substring(1);
            }

            if (!name.startsWith(packageDirName) || !name.endsWith(".class") || entry.isDirectory()) {
                continue;
            }

            int idx = name.lastIndexOf('/');
            if (idx != -1) {
                packageName = name.substring(0, idx).replace('/', '.');
            }

            final String simpleName = name.substring(packageName.length() + 1, name.length() - 6);
            final String fqcn = packageName + '.' + simpleName;

            try {
                classes.add(Class.forName(fqcn, false, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Failed to load config module class {}", fqcn, e);
            }
        }
    }

    private static List<String> buildSparkExtraConfigs() {
        List<String> extraConfigs = new ArrayList<>(List.of("config/" + GLOBAL_CONFIG_FILE));

        @Nullable String existing = System.getProperty("spark.serverconfigs.extra");
        if (existing != null && !existing.isEmpty()) {
            extraConfigs.addAll(Arrays.asList(existing.split(",")));
        }

        return extraConfigs;
    }

    public static void registerSparkExtraConfig() {
        boolean sparkEnabled = GlobalConfiguration.get().spark.enabled
                || Bukkit.getServer().getPluginManager().getPlugin("spark") != null;

        if (!sparkEnabled) {
            return;
        }

        System.setProperty("spark.serverconfigs.extra", String.join(",", buildSparkExtraConfigs()));
    }
}
