package de.geolykt.fluctis.impl;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import de.geolykt.fluctis.api.MarketItem;

/**
 * Default implementation of the Market Item interface using bukkit's material enum.
 */
public final class MaterialMarketItem implements MarketItem<Material> {

    private long cacheDelta;
    private double cachePrice;
    private long delta;
    private final @NotNull Material material;
    private final double worthMultiplier;

    /**
     * The constructor.
     *
     * @param material The material backing this market item
     * @param delta The current delta of this item, see {@link #getDelta()}
     * @param worthMultiplier The worth multiplier of this item, see {@link #getWorthMultiplier()}
     */
    public MaterialMarketItem(@NotNull Material material, long delta, double worthMultiplier) {
        this.material = material;
        this.worthMultiplier = worthMultiplier;
        this.delta = delta;
        this.cacheDelta = this.delta;
        this.cachePrice = getPrice0();
    }

    @Override
    public @NotNull Material getBacking() {
        return material;
    }

    @Override
    public long getDelta() {
        return delta;
    }

    @Override
    public double getPrice() {
        if (cacheDelta == delta) {
            return cachePrice;
        }
        cachePrice = getPrice0();
        cacheDelta = delta;
        return cachePrice;
    }

    /**
     * The implementation of the {@link #getPrice()} algorithm. Does not perform caching and as such this method
     * should not be invoked directly.
     *
     * @return The price that corresponds to the current delta.
     */
    private double getPrice0() {
        if (delta == 0) {
            // The formula we use does not define the field 0.0 explicitly, but it will be around 1.0.
            return worthMultiplier;
        }
        /*
         * We will use this graph as our formula:
         * \frac{10^{\frac{\left|x\right|}{x}\cdot\log_{e}\left(\left(\left|x\right|+1\right)\right)}}{7^{\frac{\left|x\right|}{x}\cdot\log_{e}\left(\left(\left|x\right|+1\right)\right)}}
         */
        double log = Math.log(Math.abs(delta) + 1) * (delta < 0 ? -1 : 1);
        return Math.pow(10, log) / Math.pow(7, log) * worthMultiplier;
    }

    @Override
    public double getWorthMultiplier() {
        return worthMultiplier;
    }

    @Override
    public void setDelta(long delta) {
        this.delta = delta;
    }
}
