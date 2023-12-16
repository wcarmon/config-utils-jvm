package com.github.wcarmon.properties;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/** Simplifies building and retrieving values from java.util.Properties. */
public final class PropertyFileUtils {

    /** https://datatracker.ietf.org/doc/html/rfc1340 */
    public static final int MAX_PORT = 0xffff;

    /** For parsing booleans */
    private static final Set<String> TRUTHY_VALUES;

    static {
        TRUTHY_VALUES = Set.of("y", "yes", "t", "true", "on", "1");
    }

    private PropertyFileUtils() {}

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
     * @param properties
     * @param key
     * @return a path to an existing directory
     */
    public static Path getExistingDirPath(Properties properties, String key) {
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
     * @param properties
     * @param key
     * @return a path to an existing file
     */
    public static Path getExistingFilePath(Properties properties, String key) {
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
     * @param properties
     * @param key
     * @param defaultValue
     * @return value leniently interpreted as a boolean
     */
    public static boolean getOptionalBoolean(
            Properties properties, String key, boolean defaultValue) {

        requireNonNull(properties, "properties are required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.getProperty(key, "").strip().toLowerCase(Locale.getDefault());
        if (value.isBlank()) {
            return defaultValue;
        }

        return TRUTHY_VALUES.contains(value);
    }

    /**
     * @param properties
     * @param key
     * @param defaultValue
     * @return an int or the default (never null)
     */
    public static int getOptionalInt(Properties properties, String key, int defaultValue) {

        requireNonNull(properties, "properties are required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.getProperty(key, "").strip();
        if (value.isBlank()) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    /**
     * @param properties
     * @param key
     * @param defaultValue
     * @return a long or the default (never null)
     */
    public static long getOptionalLong(Properties properties, String key, long defaultValue) {

        requireNonNull(properties, "properties are required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.getProperty(key, "").strip();
        if (value.isBlank()) {
            return defaultValue;
        }

        return Long.parseLong(value);
    }

    /**
     * @param properties
     * @param key
     * @param defaultValue
     * @return a pattern or the default (never null)
     */
    public static Pattern getOptionalRegexPattern(
            Properties properties, String key, Pattern defaultValue) {
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
     * @param properties
     * @param key
     * @param defaultValue
     * @return a string or the (nullable) default
     */
    @Nullable
    public static String getOptionalString(
            Properties properties, String key, @Nullable String defaultValue) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.getProperty(key, "").strip();
        if (value.isBlank()) {
            return defaultValue;
        }

        return value;
    }

    /**
     * @param properties
     * @param key
     * @param defaultValue
     * @return a URI or the (nullable) default
     */
    @Nullable
    public static URI getOptionalURI(
            Properties properties, String key, @Nullable String defaultValue) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.getProperty(key, "").strip();
        if (!value.isBlank()) {
            return URI.create(value);
        }

        if (defaultValue == null || defaultValue.isBlank()) {
            return null;
        }

        return URI.create(defaultValue);
    }

    /**
     * @param properties
     * @param key
     * @param defaultValue
     * @return a URI or the default, (never null)
     */
    public static URI getOptionalURI(Properties properties, String key, URI defaultValue) {
        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(defaultValue, "defaultValue is required and null.");
        requireNonNull(properties, "properties is required and null.");

        final var value = properties.getProperty(key, "").strip();
        if (value.isBlank()) {
            return defaultValue;
        }

        return URI.create(value);
    }

    /**
     * Path must be non-blank, but need not exist.
     *
     * <p>If the path exists, it must be a directory.
     *
     * @param properties
     * @param key
     * @return a path to an existing directory or to a non-existent potential directory
     */
    public static Path getRequiredDirPath(Properties properties, String key) {
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
     * @param properties
     * @param key
     * @return a path to an existing file or to a non-existent potential file
     */
    public static Path getRequiredFilePath(Properties properties, String key) {
        final var raw = getRequiredString(properties, key);

        final var path = Paths.get(raw).toAbsolutePath().normalize();

        if (Files.exists(path) && !Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must be a regular file: %s", key, path));
        }

        return path;
    }

    /**
     * @param properties
     * @param key
     * @return the int (never null)
     */
    public static int getRequiredInt(Properties properties, String key) {
        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(properties, "properties is required and null.");

        final var value = properties.getProperty(key, "").strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException(String.format("property '%s' is required", key));
        }

        return Integer.parseInt(value);
    }

    /**
     * @param properties
     * @param key
     * @return a long, (never null)
     */
    public static long getRequiredLong(Properties properties, String key) {
        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(properties, "properties is required and null.");

        final var value = properties.getProperty(key, "").strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException(String.format("property '%s' is required", key));
        }

        return Long.parseLong(value);
    }

    /**
     * Path must be non-blank, but need not exist.
     *
     * @param properties
     * @param key
     * @return
     */
    public static Path getRequiredPath(Properties properties, String key) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.getProperty(key, "").strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException(String.format("property '%s' is required", key));
        }

        return Paths.get(value).toAbsolutePath().normalize();
    }

    /**
     * See https://datatracker.ietf.org/doc/html/rfc1340
     *
     * @param properties
     * @param key
     * @return a valid IP port
     */
    public static int getRequiredPort(Properties properties, String key) {
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
     * @param properties
     * @param key
     * @return a compiled Pattern (never null)
     */
    public static Pattern getRequiredRegexPattern(Properties properties, String key) {
        final var raw = getRequiredString(properties, key);

        try {
            return Pattern.compile(raw);

        } catch (Exception ex) {
            throw new RuntimeException("failed to compile regex pattern for key=" + key, ex);
        }
    }

    /**
     * @param properties
     * @param key
     * @return non-blank, non-empty, non-null string
     */
    public static String getRequiredString(Properties properties, String key) {
        requireNonNull(properties, "properties are required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.getProperty(key, "").strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException(String.format("property '%s' is required", key));
        }

        return value;
    }

    /**
     * @param properties
     * @param key
     * @return a URI (never null)
     */
    public static URI getRequiredURI(Properties properties, String key) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        final var value = properties.getProperty(key, "").strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException(String.format("property '%s' is required", key));
        }

        return URI.create(value);
    }

    /**
     * @param properties
     * @param key
     * @return a UUID from the value
     */
    public static UUID getRequiredUUID(Properties properties, String key) {
        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(properties, "properties is required and null.");

        final var value = getRequiredString(properties, key);
        return UUID.fromString(value);
    }

    private static void checkArgument(boolean expr, String msg) {
        if (!expr) {
            throw new IllegalArgumentException(msg);
        }
    }
}
