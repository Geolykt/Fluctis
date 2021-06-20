package de.geolykt.fluctis.api;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

/**
 * An interface that is used to store the prices and fluctuations in the market.
 *
 * @param <T> The type that is traded in the market.
 */
public interface Market<T> {

    /**
     * Obtains a market item that is backed by the given item.
     * This will throw an {@link IllegalArgumentException} for any
     * input value that does not return true for {@link #isTradeable(Object)}.
     * The returning instance should NOT be cloned and it is recommended
     * that it is created lazily.
     *
     * @param item The backing item that is traded on the markets
     * @return The backed market item
     */
    public @NotNull MarketItem<T> getItem(@NotNull T item);

    /**
     * Obtain all tradeable items. References to the items should not be cloned,
     * however the returned collection should be cloned so that modifications
     * to the collection do not impact the actual availability.
     * This operation may not be supported under some implementations, at which
     * point {@link UnsupportedOperationException} should be thrown. The operation
     * is also discouraged, as it it will create all market items should they not
     * have been created yet, which depending on implementation might be
     * resource intensive.
     *
     * @return A {@link Collection} of {@link MarketItem} that are traded on this market.
     */
    public @NotNull Collection<@NotNull MarketItem<T>> getItems();

    /**
     * Obtains whether the item is tradeable within this market.
     * If this returns false, then {@link #getItem(Object)} should
     * throw an exception.
     *
     * @param item The item that is queried.
     * @return True if it is tradeable, otherwise sellable.
     */
    public boolean isTradeable(@NotNull T item);

}
