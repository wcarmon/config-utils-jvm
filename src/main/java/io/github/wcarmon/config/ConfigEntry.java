package io.github.wcarmon.config;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Like a Map.Entry, with extra conveniences
 *
 * @param <V> type of the entry value
 */
public final class ConfigEntry<V> {

    /** Original key from Property or Map */
    private final String fullKey;

    /** subset of fullKey Part after the array index or after some prefix */
    private final String shortKey;

    /** Anything acceptable for Map value */
    @Nullable private final V value;

    private ConfigEntry(Builder<V> builder) {
        requireNonNull(builder, "builder is required and null.");

        fullKey = builder.fullKey;
        shortKey = builder.shortKey == null ? "" : builder.shortKey.strip();
        value = builder.value;

        if (fullKey == null || fullKey.isBlank()) {
            throw new IllegalArgumentException("fullKey is required");
        }

        if (!fullKey.endsWith(shortKey)) {
            throw new IllegalArgumentException(
                    "fullKey ('" + fullKey + "') must end with shortKey ('" + shortKey + "')");
        }
    }

    /**
     * Factory for builder.
     *
     * @param <T> type of the entry's value
     * @return new Builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigEntry<?> listEntry = (ConfigEntry<?>) o;
        return Objects.equals(fullKey, listEntry.fullKey)
                && Objects.equals(shortKey, listEntry.shortKey)
                && Objects.equals(value, listEntry.value);
    }

    /**
     * accessor for fullKey property
     *
     * @return validated fullKey property
     */
    public String fullKey() {
        return fullKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullKey, shortKey, value);
    }

    /**
     * accessor for shortKey property
     *
     * @return validated shortKey property
     */
    public String shortKey() {
        return shortKey;
    }

    @Override
    public String toString() {
        return "ListEntry{"
                + "fullKey='"
                + fullKey
                + '\''
                + ", shortKey='"
                + shortKey
                + '\''
                + ", value="
                + value
                + '}';
    }

    /**
     * accessor for value property
     *
     * @return entry value
     */
    @Nullable
    public V value() {
        return value;
    }

    /**
     * Builder pattern
     *
     * @param <V> type of the entry's value
     */
    public static final class Builder<V> {

        private String fullKey;
        private String shortKey;
        @Nullable private V value;

        private Builder() {}

        /**
         * Factory for builder.
         *
         * @param <V> type of the entry value
         * @return a new Builder for ConfigEntry
         */
        public static <V> Builder<V> builder() {
            return new Builder<>();
        }

        /**
         * Builder pattern
         *
         * @return a new validated ConfigEntry
         */
        public ConfigEntry<V> build() {
            return new ConfigEntry<>(this);
        }

        /**
         * Set value for fullKey
         *
         * @param val new value
         * @return this builder
         */
        public Builder<V> fullKey(String val) {
            fullKey = val;
            return this;
        }

        /**
         * Set value for shortKey
         *
         * @param val new value
         * @return this builder
         */
        public Builder<V> shortKey(String val) {
            shortKey = val;
            return this;
        }

        /**
         * Set value for entry.value
         *
         * @param val new value
         * @return this builder
         */
        public Builder<V> value(@Nullable V val) {
            value = val;
            return this;
        }
    }
}
