package de.geolykt.fluctis.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import de.geolykt.fluctis.api.Market;
import de.geolykt.fluctis.api.MarketItem;

/**
 * Base implementation of a market that is based on bukkit's material enum.
 */
public record MaterialMarket(@NotNull EnumMap<@NotNull Material, @NotNull MarketItem<Material>> elements) implements Market<Material> {

    @SuppressWarnings("null")
    @Override
    public @NotNull MarketItem<Material> getItem(@NotNull Material item) {
        if (!elements.containsKey(item)) {
            throw new IllegalArgumentException("The material cannot be traded.");
        }
        return elements.get(item);
    }

    @Override
    public @NotNull Collection<@NotNull MarketItem<Material>> getItems() {
        ArrayList<@NotNull MarketItem<Material>> marketItems = new ArrayList<>();
        for (var entry : elements.entrySet()) {
            marketItems.add(entry.getValue());
        }
        return marketItems;
    }

    @Override
    public boolean isTradeable(@NotNull Material item) {
        return elements.containsKey(item);
    }

    public EnumMap<@NotNull Material, @NotNull MarketItem<Material>> getElements() {
        return elements;
    }
}
