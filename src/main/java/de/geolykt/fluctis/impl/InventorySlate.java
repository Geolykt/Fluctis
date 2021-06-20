package de.geolykt.fluctis.impl;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import de.geolykt.fluctis.api.NullUtils;

/**
 * An option of the /shop menu
 * @author Geolykt
 *
 */
public class InventorySlate {

    /**
     * The icon of the menu that will be displayed in the /shop menu.
     */
    private final @NotNull ItemStack icon;

    private final @NotNull Optional<@NotNull Material[]> materials;

    /**
     * Whether the slate is a shop. If false, it will not be clickable.
     */
    private final boolean shop;

    /**
     * Constructor. This slate will then not be clickable.
     */
    public InventorySlate() {
        shop = false;
        materials = NullUtils.emptyOptional();
        ItemStack is = new ItemStack(Material.GRAY_STAINED_GLASS);
        ItemMeta im = NullUtils.requireNotNull(is.getItemMeta());
        im.setDisplayName(" ");
        is.setItemMeta(im);
        icon = is;
    }

    public InventorySlate(@NotNull Optional<@NotNull Material[]> materials, @NotNull ItemStack icon) {
        shop = materials.isPresent();
        this.materials = materials;
        this.icon = icon;
    }

    /**
     * Obtains the icon of the menu that will be displayed in the /shop menu.
     *
     * @return see above
     */
    public @NotNull ItemStack getIcon() {
        return icon;
    }

    public @NotNull Optional<@NotNull Material[]> getMaterials() {
        return materials;
    }

    /**
     * Whether the slate is a shop. If false, it will not be clickable.
     *
     * @return See above
     */
    public boolean isShop() {
        return shop;
    }
}
