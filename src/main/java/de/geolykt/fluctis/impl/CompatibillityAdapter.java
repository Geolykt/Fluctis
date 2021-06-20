package de.geolykt.fluctis.impl;

import java.util.EnumSet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Excerpts from the "CompatibillityAdapter" file from the EnchantmentsPlus plugin. This file provides several utility-like
 * methods that were originally intended to be used for diverging minecraft versions.
 * I copied it to this plugin as I am somewhat lazy and already did something like this before
 *
 * @version 4.0.0
 * @author Geolykt
 */
@SuppressWarnings("null")
public class CompatibillityAdapter {

    private final Plugin plugin;

    public CompatibillityAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Obtains all the materials that are registered by the tag that has the given name.
     * Looks them up in the block registry. Returns empty and logs a warning if the tag was not found.
     *
     * @param name The tag, may contain a `#`. Uses minecraft as default namespace
     * @param category Used for error logging
     * @return The materials in the tag as an EnumSet
     * @since 4.0.0
     */
    public @NotNull EnumSet<@NotNull Material> getBlockTag(@NotNull String name, @NotNull String category) {
        if (name.charAt(0) == '#') {
            name = name.substring(1);
        }
        NamespacedKey key = NamespacedKey.fromString(name);
        if (key == null) {
            plugin.getLogger().warning(String.format("Tag %s in category %s is not a valid namespacedKey!", name, category));
            return EnumSet.noneOf(Material.class);
        }
        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
        if (tag == null) {
            plugin.getLogger().warning(String.format("Tag %s in category %s is not a valid tag!", name, category));
            return EnumSet.noneOf(Material.class);
        }
        return EnumSet.copyOf(tag.getValues());
    }

    public @NotNull EnumSet<@NotNull Material> getMaterialSet(@NotNull FileConfiguration config, @NotNull String path) {
        EnumSet<Material> es = EnumSet.noneOf(Material.class);
        for (String materialName : config.getStringList(path)) {
            if (materialName.charAt(0) == '#') {
                es.addAll(getBlockTag(materialName, path));
            } else {
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                   es.add(material);
                }
            }
        }
        return es;
    }
}
