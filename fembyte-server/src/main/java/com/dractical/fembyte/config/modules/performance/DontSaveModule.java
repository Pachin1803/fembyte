package com.dractical.fembyte.config.modules.performance;

import com.dractical.fembyte.config.ConfigCategory;
import com.dractical.fembyte.config.ConfigModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DontSaveModule extends ConfigModule {

    public static boolean ENABLED = false;
    public static boolean SKIP_ALL_ENTITIES = false;
    private static final Set<ResourceLocation> ENTITY_TYPES = new HashSet<>();

    private static String path() {
        return ConfigCategory.PERFORMANCE.getBaseKeyName() + ".dont-save.";
    }

    @Override
    public void onLoaded() {
        ENABLED = config.getBoolean(
                path() + "enabled",
                false,
                "Prevents saving of selected entity types."
        );

        SKIP_ALL_ENTITIES = config.getBoolean(
                path() + "skip-all",
                false,
                "If true, no entities will ever be saved (RISKY)."
        );

        loadEntityTypes();
    }

    private void loadEntityTypes() {
        ENTITY_TYPES.clear();

        List<String> configuredTypes = config.getList(
                path() + "entity-types",
                Collections.emptyList(),
                "List of entity types (e.g. zombie) that should never be saved."
        );

        for (String entry : configuredTypes) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(entry.trim().toLowerCase(Locale.ROOT));
            if (id == null) {
                logger.warn("Invalid entity type '{}' in performance.dont-save.entity-types, skipping.", entry);
                continue;
            }

            ENTITY_TYPES.add(id);
        }
    }

    public static boolean shouldSkipSaving(EntityType<?> type) {
        if (!ENABLED) {
            return false;
        }

        if (SKIP_ALL_ENTITIES) {
            return true;
        }

        if (type == null) {
            return false;
        }

        return ENTITY_TYPES.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }
}
