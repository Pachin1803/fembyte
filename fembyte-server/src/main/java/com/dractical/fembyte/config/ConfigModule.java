package com.dractical.fembyte.config;

import com.dractical.fembyte.config.annotations.Experimental;
import com.dractical.fembyte.config.annotations.DontLoad;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public abstract class ConfigModule {

    private static final Set<ConfigModule> MODULES = new LinkedHashSet<>();

    protected final FembyteGlobalConfig config;

    protected ConfigModule() {
        this.config = FembyteConfig.config();
    }

    public static void initModules() throws NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {

        List<Field> enabledExperimentalModules = new ArrayList<>();
        List<Field> deprecatedModules = new ArrayList<>();

        Class<?>[] classes = FembyteConfig
                .getClasses(FembyteConfig.MODULES_PACKAGE)
                .stream()
                .filter(ConfigModule::isConfigModuleClass)
                .toArray(Class[]::new);

        Arrays.sort(classes, Comparator.comparing(Class::getSimpleName));

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(DontLoad.class)) {
                continue;
            }

            ConfigModule module = (ConfigModule) clazz.getConstructor().newInstance();
            module.onLoaded();
            MODULES.add(module);

            for (Field field : getAnnotatedStaticFields(clazz, Experimental.class)) {
                Object value = field.get(null);
                if (value instanceof Boolean enabled && enabled) {
                    enabledExperimentalModules.add(field);
                }
            }

            for (Field field : getAnnotatedStaticFields(clazz, Deprecated.class)) {
                Object value = field.get(null);
                if (value instanceof Boolean enabled && enabled) {
                    deprecatedModules.add(field);
                }
            }
        }

        if (!enabledExperimentalModules.isEmpty()) {
            FembyteConfig.LOGGER.warn(
                    "You have the following experimental module(s) enabled: {}. Proceed with caution!",
                    formatModules(enabledExperimentalModules)
            );
        }

        if (!deprecatedModules.isEmpty()) {
            FembyteConfig.LOGGER.warn(
                    "The following enabled module(s) are deprecated: {}. Proceed with caution!",
                    formatModules(deprecatedModules)
            );
        }
    }

    public static void loadAfterBootstrap() {
        for (ConfigModule module : MODULES) {
            module.onPostLoaded();
        }

        try {
            FembyteConfig.config().saveConfig();
        } catch (Exception e) {
            FembyteConfig.LOGGER.error("Failed to save Fembyte global config file!", e);
        }
    }

    public static void clearModules() {
        MODULES.clear();
    }

    public abstract void onLoaded();

    public void onPostLoaded() {
    }

    private static boolean isConfigModuleClass(Class<?> clazz) {
        return ConfigModule.class.isAssignableFrom(clazz)
                && !clazz.isInterface()
                && !Modifier.isAbstract(clazz.getModifiers());
    }

    private static List<Field> getAnnotatedStaticFields(
            Class<?> clazz,
            Class<? extends Annotation> annotation
    ) {
        List<Field> fields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(annotation)) continue;
            if (!Modifier.isStatic(field.getModifiers())) continue;

            field.setAccessible(true);
            fields.add(field);
        }

        return fields;
    }

    private static List<String> formatModules(List<Field> fields) {
        return fields.stream()
                .map(f -> f.getDeclaringClass().getSimpleName() + "." + f.getName())
                .toList();
    }
}
