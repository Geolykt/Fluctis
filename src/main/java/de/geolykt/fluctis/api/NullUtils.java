package de.geolykt.fluctis.api;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collection of QoL null safety-related utility methods.
 * Originally created for the Starloader-API but has since then been copied to many
 * of my other projects.
 *
 * @author Geolykt
 */
public final class NullUtils {

    private NullUtils() {
        // Disable constructor
        throw new UnsupportedOperationException();
    }

    public static @NotNull <T> Optional<T> emptyOptional() {
        Optional<T> opt = Optional.empty();
        if (opt == null) {
            throw new InternalError(); // Not possible, but let's have it there either way
        }
        return opt;
    }

    public static @NotNull String format(@NotNull String format, @Nullable Object... args) {
        String ret = String.format(Locale.ROOT, format, args);
        if (ret == null) {
            throw new InternalError(); // Not possible, but let's have it there either way
        }
        return ret;
    }

    public static @NotNull <T> T requireNotNull(@Nullable T object) {
        if (object != null) {
            return object;
        }
        throw new NullPointerException();
    }

    public static @NotNull <T> T requireNotNull(@Nullable T object, @NotNull String message) {
        if (object != null) {
            return object;
        }
        throw new NullPointerException(message);
    }

    public static @NotNull <T> T requireNotNull(@Nullable T object, @NotNull Supplier<String> message) {
        if (object != null) {
            return object;
        }
        throw new NullPointerException(message.get());
    }
}
