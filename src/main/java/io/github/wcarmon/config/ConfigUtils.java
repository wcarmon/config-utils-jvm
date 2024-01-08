package io.github.wcarmon.config;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * Simplifies building and retrieving values from java.util.Properties or Map with same structure
 */
public final class ConfigUtils {

    /** https://datatracker.ietf.org/doc/html/rfc1340 */
    public static final int MAX_PORT = 0xffff;

    /** For parsing booleans, lower case */
    private static final Set<String> TRUTHY_VALUES;

    static {
        TRUTHY_VALUES = Set.of("1", "on", "t", "true", "y", "yes");
    }

    private ConfigUtils() {}

    @Nullable
    public static Boolean consumeOptionalBoolean(
            Map<String, Object> m, String key, @Nullable Boolean defaultValue) {

        final var out = getOptionalBoolean(m, key, defaultValue);
        m.remove(key);
        return out;
    }

    @Nullable
    public static Integer consumeOptionalInt(
            Map<String, Object> m, String key, @Nullable Integer defaultValue) {

        final var out = getOptionalInt(m, key, defaultValue);
        m.remove(key);
        return out;
    }

    @Nullable
    public static Long consumeOptionalLong(Map<String, Object> m, String key, Long defaultValue) {

        final var out = getOptionalLong(m, key, defaultValue);
        m.remove(key);
        return out;
    }

    @Nullable
    public static String consumeOptionalString(
            Map<String, Object> m, String key, @Nullable String defaultValue) {

        final var out = getOptionalString(m, key, defaultValue);
        m.remove(key);
        return out;
    }

    public static String consumeRequiredString(Map<String, Object> m, String key) {
        final var out = getRequiredString(m, key);
        m.remove(key);
        return out;
    }

    /**
     * @param confPath to a *.properties file
     * @return new Properties from file at path
     */
    public static Properties from(Path confPath) {
        requireNonNull(confPath, "confPath is required and null.");

        final var properties = new Properties();
        try (var r = Files.newBufferedReader(confPath)) {
            properties.load(r);

        } catch (Exception ex) {
            throw new RuntimeException("failed to read/load properties", ex);
        }

        return properties;
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @return a path to an existing directory
     */
    public static Path getExistingDirPath(Map<String, Object> properties, String key) {
        final var raw = getRequiredString(properties, key);

        final var path = Paths.get(raw).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IllegalArgumentException(
                    String.format("Directory must exist for property '%s'", key));
        }

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must be a directory", key));
        }

        return path;
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @return a path to an existing file
     */
    public static Path getExistingFilePath(Map<String, Object> properties, String key) {
        final var raw = getRequiredString(properties, key);

        final var path = Paths.get(raw).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IllegalArgumentException(
                    String.format("File must exist for property '%s'", key));
        }

        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must not be a directory", key));
        }

        return path;
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @param defaultValue
     * @return value leniently interpreted as a boolean or null if unset and default is null
     */
    @Nullable
    public static Boolean getOptionalBoolean(
            Map<String, Object> properties, String key, @Nullable Boolean defaultValue) {

        requireNonNull(properties, "properties are required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean) {
            return (boolean) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }

        if (value instanceof CharSequence) {
            final var s = value.toString().strip().toLowerCase(Locale.getDefault());

            if (s.isBlank()) {
                return defaultValue;
            }

            return TRUTHY_VALUES.contains(s);
        }

        throw new IllegalArgumentException(
                "Failed to coerce type to boolean: " + value.getClass().getName());
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @param defaultValue
     * @return an int or the default (which may be null)
     */
    @Nullable
    public static Integer getOptionalInt(
            Map<String, Object> properties, String key, @Nullable Integer defaultValue) {

        requireNonNull(properties, "properties are required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return ((Number) value).intValue();
        }

        if (value instanceof Long) {
            final var l = (long) value;
            if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
                throw new IllegalArgumentException(
                        "value overflow for key=" + key + ", value=" + value);
            }

            return (int) l;
        }

        // TODO: BigInteger

        if (value instanceof CharSequence) {
            final var s = value.toString().strip();

            if (s.isBlank()) {
                return defaultValue;
            }

            return Integer.parseInt(s);
        }

        throw new RuntimeException("Failed to coerce type to int: " + value.getClass().getName());
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @param defaultValue
     * @return a long or the default (which may be null)
     */
    @Nullable
    public static Long getOptionalLong(
            Map<String, Object> properties, String key, @Nullable Long defaultValue) {

        requireNonNull(properties, "properties are required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Long
                || value instanceof Integer
                || value instanceof Short
                || value instanceof Byte) {
            return ((Number) value).longValue();
        }

        // TODO: BigInteger

        if (value instanceof CharSequence) {
            final var s = value.toString().strip();

            if (s.isBlank()) {
                return defaultValue;
            }

            return Long.parseLong(s);
        }

        throw new RuntimeException("Failed to coerce type to long: " + value.getClass().getName());
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @param defaultValue
     * @return a pattern or the default (never null)
     */
    public static Pattern getOptionalRegexPattern(
            Map<String, Object> properties, String key, Pattern defaultValue) {
        requireNonNull(defaultValue, "defaultValue is required and null.");
        final var raw = getRequiredString(properties, key);

        try {
            if (raw.isBlank()) {
                return defaultValue;
            }

            return Pattern.compile(raw);

        } catch (Exception ex) {
            throw new RuntimeException("failed to compile regex pattern for key=" + key, ex);
        }
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @param defaultValue
     * @return a string or the (nullable) default
     */
    @Nullable
    public static String getOptionalString(
            Map<String, Object> properties, String key, @Nullable String defaultValue) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof CharSequence) {
            final var s = value.toString().strip();
            if (s.isBlank()) {
                return defaultValue;
            }

            return s;
        }

        return value.toString();
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @param defaultValue
     * @return a URI or the (nullable) default
     */
    @Nullable
    public static URI getOptionalURI(
            Map<String, Object> properties, String key, @Nullable String defaultValue) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.get(key);
        if (value == null) {
            if (defaultValue != null && !defaultValue.isBlank()) {
                return URI.create(defaultValue);
            }

            return null;
        }

        if (value instanceof CharSequence) {
            final var s = value.toString().strip();
            if (!s.isBlank()) {
                return URI.create(s);
            }

            if (defaultValue == null || defaultValue.isBlank()) {
                return null;
            }
        }

        throw new IllegalArgumentException(
                "Failed to coerce type to uri: " + value.getClass().getName());
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @param defaultValue
     * @return a URI or the default, (never null)
     */
    public static URI getOptionalURI(Map<String, Object> properties, String key, URI defaultValue) {
        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(defaultValue, "defaultValue is required and null.");
        requireNonNull(properties, "properties is required and null.");

        final var value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof CharSequence) {
            final var s = value.toString().strip();

            if (!s.isBlank()) {
                return URI.create(s);
            }

            return defaultValue;
        }

        throw new IllegalArgumentException(
                "Failed to coerce type to uri: " + value.getClass().getName());
    }

    /**
     * Path must be non-blank, but need not exist.
     *
     * <p>If the path exists, it must be a directory.
     *
     * @param properties instance to read
     * @param key property name
     * @return a path to an existing directory or to a non-existent potential directory
     */
    public static Path getRequiredDirPath(Map<String, Object> properties, String key) {
        final var raw = getRequiredString(properties, key);

        final var path = Paths.get(raw).toAbsolutePath().normalize();

        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must be a directory: %s", key, path));
        }

        return path;
    }

    /**
     * Path must be non-blank, but need not exist.
     *
     * <p>If the path exists, it must be a regular file.
     *
     * @param properties instance to read
     * @param key property name
     * @return a path to an existing file or to a non-existent potential file
     */
    public static Path getRequiredFilePath(Map<String, Object> properties, String key) {
        final var raw = getRequiredString(properties, key);

        final var path = Paths.get(raw).toAbsolutePath().normalize();

        if (Files.exists(path) && !Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must be a regular file: %s", key, path));
        }

        return path;
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @return the int (never null)
     */
    public static int getRequiredInt(Map<String, Object> properties, String key) {
        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(properties, "properties is required and null.");

        final var value = properties.get(key);
        if (value == null) {
            throw new IllegalArgumentException("property required: '" + key + "'");
        }

        final var out = getOptionalInt(properties, key, null);
        if (out == null) {
            throw new IllegalArgumentException("property required: '" + key + "'");
        }

        return out;
    }

    /**
     * Parse a long from the value.
     *
     * @param properties instance to read
     * @param key property name
     * @return a long, (never null)
     */
    public static long getRequiredLong(Map<String, Object> properties, String key) {
        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(properties, "properties is required and null.");

        final var value = properties.get(key);
        if (value == null) {
            throw new IllegalArgumentException("property required: '" + key + "'");
        }

        final var out = getOptionalLong(properties, key, null);
        if (out == null) {
            throw new IllegalArgumentException("property required: '" + key + "'");
        }

        return out;
    }

    /**
     * Parse a java.nio.file.Path from the value. Path must be non-blank, but need not exist.
     *
     * @param properties instance to read
     * @param key property name
     * @return a path (which may or may not exist in a file system), never null
     */
    public static Path getRequiredPath(Map<String, Object> properties, String key) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.get(key);
        if (value == null) {
            throw new IllegalArgumentException("property required: '" + key + "'");
        }

        if (value instanceof CharSequence) {
            final var s = value.toString().strip();

            if (s.isBlank()) {
                throw new IllegalArgumentException("property required: '" + key + "'");
            }

            return Paths.get(s).toAbsolutePath().normalize();
        }

        throw new IllegalArgumentException(
                "Failed to coerce type to path: " + value.getClass().getName());
    }

    /**
     * See https://datatracker.ietf.org/doc/html/rfc1340
     *
     * @param properties instance to read
     * @param key property name
     * @return a valid IP port
     */
    public static int getRequiredPort(Map<String, Object> properties, String key) {
        final var value = getRequiredInt(properties, key);
        if (value < 0) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must non-negative", key));
        }

        if (value > MAX_PORT) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must less than %d", key, MAX_PORT));
        }

        return value;
    }

    /**
     * Parse a Regex pattern from the value.
     *
     * @param properties instance to read
     * @param key property name
     * @return a compiled Pattern (never null)
     */
    public static Pattern getRequiredRegexPattern(Map<String, Object> properties, String key) {
        final var raw = getRequiredString(properties, key);

        try {
            return Pattern.compile(raw);

        } catch (Exception ex) {
            throw new RuntimeException("failed to compile regex pattern for key=" + key, ex);
        }
    }

    /**
     * Read a string from the value.
     *
     * @param properties instance to read
     * @param key property name
     * @return non-blank, non-empty, non-null string
     */
    public static String getRequiredString(Map<String, Object> properties, String key) {
        requireNonNull(properties, "properties are required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.get(key);
        if (value == null) {
            throw new IllegalArgumentException("property required: '" + key + "'");
        }

        if (value instanceof CharSequence) {
            final var s = value.toString().strip();

            if (s.isBlank()) {
                throw new IllegalArgumentException("property required: '" + key + "'");
            }

            return s;
        }

        throw new IllegalArgumentException(
                "Failed to coerce type to string: " + value.getClass().getName());
    }

    /**
     * Parse a URI from the value.
     *
     * @param properties instance to read
     * @param key property name
     * @return a URI (never null)
     */
    public static URI getRequiredURI(Map<String, Object> properties, String key) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.get(key);
        if (value == null) {
            throw new IllegalArgumentException("property required: '" + key + "'");
        }

        if (value instanceof CharSequence) {
            final var s = value.toString().strip();

            if (s.isBlank()) {
                throw new IllegalArgumentException("property required: '" + key + "'");
            }

            return URI.create(s);
        }

        throw new IllegalArgumentException(
                "Failed to coerce type to uri: " + value.getClass().getName());
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @return a UUID from the value
     */
    public static UUID getRequiredUUID(Map<String, Object> properties, String key) {
        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(properties, "properties is required and null.");

        final var value = getRequiredString(properties, key);
        return UUID.fromString(value);
    }

    public static void requireFullyConsumed(Map<String, Object> m, Class<?> targetType) {
        requireFullyConsumed(m, targetType.getSimpleName());
    }

    /**
     * Convert a Properties instance to a Map.
     *
     * @param properties
     * @return a mutable map with the same properties as the input
     */
    public static Map<String, Object> toMap(Properties properties) {
        requireNonNull(properties, "properties is required and null.");

        Map<String, Object> out = new HashMap<>(properties.size());
        for (var entry : properties.entrySet()) {
            // -- Assumption: java.util.Properties keys are always Strings
            out.put((String) entry.getKey(), entry.getValue());
        }

        return out;
    }

    static void requireFullyConsumed(Map<String, Object> m, String targetTypeName) {
        requireNonNull(m, "m is required and null.");
        checkArgument(
                targetTypeName != null && !targetTypeName.isBlank(), "targetTypeName is required");

        if (m.isEmpty()) {
            return;
        }

        final var extraProps = new TreeSet<>(m.keySet());
        throw new IllegalArgumentException(
                "Unrecognized/Extra properties for type: " + targetTypeName + " -> " + extraProps);
    }

    private static void checkArgument(boolean expr, String msg) {
        if (!expr) {
            throw new IllegalArgumentException(msg);
        }
    }
}
