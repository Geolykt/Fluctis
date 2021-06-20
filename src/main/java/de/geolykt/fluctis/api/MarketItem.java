package de.geolykt.fluctis.api;

import org.jetbrains.annotations.NotNull;

/**
 * Interface of stuff that is on the open market.
 *
 * @param <T> The type that is traded on the market. This should back this item.
 */
public interface MarketItem<T> {

    /**
     * Obtains the backing type.
     *
     * @return The backing type
     */
    public @NotNull T getBacking();

    /**
     * Obtains the amount of stuff that was traded since the beginning of the records.
     * This delta value affects the price of the item.
     *
     * @return The delta value
     */
    public long getDelta();

    /**
     * Obtains the price of the item.
     * The return value might be cached but might also not be.
     *
     * @return The current price of the item
     */
    public double getPrice();

    /**
     * Obtains a static multiplier that is set on this item.
     * This multiplier is used to make less common stuff more expensive and more common stuff less expensive.
     *
     * @return A static multiplier that is used onto the {@link #getPrice()} method.
     */
    public double getWorthMultiplier();

    /**
     * Changes the delta value, which affects the price of the item. The delta value is the amount
     * of stuff that was traded since the beginning of history.
     *
     * @param delta The new delta value.
     */
    public void setDelta(long delta);
}
