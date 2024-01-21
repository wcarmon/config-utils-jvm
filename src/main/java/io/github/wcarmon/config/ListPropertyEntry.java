package io.github.wcarmon.config;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

/**
 * Like a Map.Entry, with extra conveniences
 *
 * @param <V> type of the entry value
 */
public final class ListPropertyEntry<V> {

    /** Original key from Property or Map */
    private final String fullKey;

    /** After the array index */
    private final String shortKey;

    /** Anything acceptable for Map value */
    @Nullable
    private final V value;

    private ListPropertyEntry(Builder<V> builder) {
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

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListPropertyEntry<?> listEntry = (ListPropertyEntry<?>) o;
        return Objects.equals(fullKey, listEntry.fullKey)
                && Objects.equals(shortKey, listEntry.shortKey)
                && Objects.equals(value, listEntry.value);
    }

    public String fullKey() {
        return fullKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullKey, shortKey, value);
    }

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

    @Nullable
    public V value() {
        return value;
    }

    public static final class Builder<V> {

        private String fullKey;
        private String shortKey;
        @Nullable
        private V value;

        private Builder() {
        }

        public static <V> Builder<V> builder() {
            return new Builder<>();
        }

        public ListPropertyEntry<V> build() {
            return new ListPropertyEntry<>(this);
        }

        public Builder<V> fullKey(String val) {
            fullKey = val;
            return this;
        }

        public Builder<V> shortKey(String val) {
            shortKey = val;
            return this;
        }

        public Builder<V> value(@Nullable V val) {
            value = val;
            return this;
        }
    }
}
