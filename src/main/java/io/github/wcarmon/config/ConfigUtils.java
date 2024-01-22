package io.github.wcarmon.config;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/**
 * Simplifies building and retrieving values from java.util.Properties or Map with same structure
 */
public final class ConfigUtils {

    /** https://datatracker.ietf.org/doc/html/rfc1340 */
    public static final int MAX_PORT = 0xffff;

    /** Meaning the Operating System should select a port number. */
    public static final int MIN_PORT = 0;

    private static final Pattern AFTER_LIST_PROPERTY_PREFIX =
            Pattern.compile("\\[(\\d+)\\](\\.)?(.*)");
    private static final java.util.logging.Logger LOG =
            Logger.getLogger(ConfigUtils.class.getName());

    /** For parsing booleans, lower case */
    private static final Set<String> TRUTHY_VALUES;

    static {
        TRUTHY_VALUES = Set.of("1", "on", "t", "true", "y", "yes");
    }

    private ConfigUtils() {}

    /**
     * Copy all entries from properties with given prefix to a new Map
     *
     * <p>See also filterByPrefix
     *
     * @param properties instance to read
     * @param keyPrefix eg. "a.b."
     * @return a new mutable Map with a subset of entries from properties, with given prefix
     */
    public static Map<String, ConfigEntry<?>> buildEntriesForPrefix(
            Map<String, ?> properties, String keyPrefix) {

        requireNonNull(properties, "properties is required and null.");
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("keyPrefix is required");
        }

        if (!Objects.equals(keyPrefix, keyPrefix.strip())) {
            throw new IllegalArgumentException("keyPrefix must be trimmed");
        }

        final var out = new HashMap<String, ConfigEntry<?>>(properties.size());
        for (var entry : properties.entrySet()) {
            if (!entry.getKey().startsWith(keyPrefix)) {
                continue;
            }

            final var shortKey = entry.getKey().substring(keyPrefix.length()).strip();

            final var newEntry =
                    ConfigEntry.builder()
                            .fullKey(entry.getKey())
                            .shortKey(shortKey)
                            .value(entry.getValue())
                            .build();

            out.put(shortKey, newEntry);
        }

        return out;
    }

    /**
     * Consumes/Removes a delimited string of bytes from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @param radix radix to use for parsing (eg. 10, 16)
     * @return possibly empty, never null, and zero null items in the output list
     */
    public static byte[] consumeDelimitedBytes(
            Map<String, ?> properties, String key, String delim, int radix) {

        final var out = getDelimitedBytes(properties, key, delim, radix);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a delimited string of doubles from a Map with the given key.
     *
     * <p>Splits and parses a string of delimited doubles to a list
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @return possibly empty, never null, and zero null items in the output list
     */
    public static List<Double> consumeDelimitedDoubles(
            Map<String, ?> properties, String key, String delim) {

        final var out = getDelimitedDoubles(properties, key, delim);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a delimited string of ints from a Map with the given key.
     *
     * <p>Splits and parses a string of delimited ints to a list
     *
     * <p>NOTE: trailing empty strings are ignored See
     * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#split(java.lang.String)
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @return possibly empty, never null, and zero null items in the output list
     */
    public static List<Integer> consumeDelimitedInts(
            Map<String, ?> properties, String key, String delim) {

        final var out = getDelimitedInts(properties, key, delim);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a delimited string of longs from a Map with the given key.
     *
     * <p>Splits and parses a string of delimited longs to a list
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @return possibly empty, never null, and zero null items in the output list
     */
    public static List<Long> consumeDelimitedLongs(
            Map<String, ?> properties, String key, String delim) {

        final var out = getDelimitedLongs(properties, key, delim);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a delimited string of ports from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @return zero or more IP address ports [0, 65535], deduped, (never null)
     */
    public static List<Integer> consumeDelimitedPorts(
            Map<String, ?> properties, String key, String delim) {

        final var out = getDelimitedPorts(properties, key, delim);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a delimited string from a Map with the given key.
     *
     * <p>NOTE: trailing empty strings are ignored See
     * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#split(java.lang.String)
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @param removeBlanks drop/filter out any blank items
     * @return possibly empty, never null
     */
    public static List<String> consumeDelimitedStrings(
            Map<String, ?> properties, String key, String delim, boolean removeBlanks) {

        final var out = getDelimitedStrings(properties, key, delim, removeBlanks);
        properties.remove(key);
        return out;
    }

    /**
     * Convenience for the common case
     *
     * @param properties instance to read and modify
     * @param key property name
     * @return possibly empty, never null
     */
    public static List<String> consumeDelimitedStrings(Map<String, ?> properties, String key) {

        final var out = getDelimitedStrings(properties, key, ",", true);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes an optional boolean value from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param defaultValue used when value is absent
     * @return value leniently interpreted as a boolean or null if unset and default is null
     */
    @Nullable
    public static Boolean consumeOptionalBoolean(
            Map<String, ?> properties, String key, @Nullable Boolean defaultValue) {

        final var out = getOptionalBoolean(properties, key, defaultValue);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes an optional int value from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param defaultValue used when value is absent
     * @return an int or the default (which may be null)
     */
    @Nullable
    public static Integer consumeOptionalInt(
            Map<String, ?> properties, String key, @Nullable Integer defaultValue) {

        final var out = getOptionalInt(properties, key, defaultValue);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes an optional long value from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param defaultValue used when value is absent
     * @return a long or the default (which may be null)
     */
    @Nullable
    public static Long consumeOptionalLong(
            Map<String, ?> properties, String key, Long defaultValue) {

        final var out = getOptionalLong(properties, key, defaultValue);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes an optional port value from a Map with the given key.
     *
     * <p>See https://datatracker.ietf.org/doc/html/rfc1340
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param defaultValue default port
     * @return a valid IP port
     */
    public static int consumeOptionalPort(Map<String, ?> properties, String key, int defaultValue) {

        final var out = getOptionalPort(properties, key, defaultValue);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes an optional regex/pattern value from a Map with the given key. If present,
     * the regex/pattern must compile
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param defaultValue used when value is absent
     * @return a pattern or the default (never null)
     */
    @Nullable
    public static Pattern consumeOptionalRegexPattern(
            Map<String, ?> properties, String key, @Nullable Pattern defaultValue) {

        final var out = getOptionalRegexPattern(properties, key, defaultValue);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes an optional string value from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param defaultValue used when value is absent
     * @return a string or the (nullable) default
     */
    @Nullable
    public static String consumeOptionalString(
            Map<String, ?> properties, String key, @Nullable String defaultValue) {

        final var out = getOptionalString(properties, key, defaultValue);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes an optional URI from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param defaultValue used when value is absent
     * @return a URI or the (nullable) default
     */
    @Nullable
    public static URI consumeOptionalURI(
            Map<String, ?> properties, String key, @Nullable String defaultValue) {

        final var out = getOptionalURI(properties, key, defaultValue);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes an optional URI from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param defaultValue used when value is absent
     * @return a URI or the default, (never null)
     */
    public static URI consumeOptionalURI(Map<String, ?> properties, String key, URI defaultValue) {
        final var out = getOptionalURI(properties, key, defaultValue);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a required path to a directory from a Map with the given key. Path must be
     * non-blank, but need not exist.
     *
     * <p>If the path exists, it must be a directory.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @return a path to an existing directory or to a non-existent potential directory
     */
    public static Path consumeRequiredDirPath(Map<String, ?> properties, String key) {
        final var out = getRequiredDirPath(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a required path to an existing directory from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @return a path to an existing directory
     */
    public static Path consumeRequiredExistingDirPath(Map<String, ?> properties, String key) {
        final var out = getRequiredExistingDirPath(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * TODO
     *
     * @param properties instance to read and modify
     * @param key property name
     * @return TODO
     */
    public static Path consumeRequiredExistingFilePath(Map<String, ?> properties, String key) {
        final var out = getRequiredExistingFilePath(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * TODO Path must be non-blank, but need not exist.
     *
     * <p>If the path exists, it must be a regular file.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @return a path to an existing file or to a non-existent potential file
     */
    public static Path consumeRequiredFilePath(Map<String, ?> properties, String key) {
        final var out = getRequiredFilePath(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a required int value from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name*
     * @return an int
     */
    public static int consumeRequiredInt(Map<String, ?> properties, String key) {
        final var out = getRequiredInt(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a required long value from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name*
     * @return a long or throw when absent
     */
    public static long consumeRequiredLong(Map<String, ?> properties, String key) {
        final var out = getRequiredLong(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a required path value from a Map with the given key.
     *
     * <p>Parse a java.nio.file.Path from the value. Path must be non-blank, but need not exist.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @return a path (which may or may not exist in a file system), never null
     */
    public static Path consumeRequiredPath(Map<String, ?> properties, String key) {
        final var out = getRequiredPath(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes an int TCP port value from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @return a port [MIN_PORT, MAX_PORT], never null
     */
    public static int consumeRequiredPort(Map<String, ?> properties, String key) {
        final var out = getRequiredPort(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a required regex value from a Map with the given key.
     *
     * <p>Parse and compile Regex pattern from the value
     *
     * @param properties instance to read and modify
     * @param key property name
     * @return a compiled Pattern (never null)
     */
    public static Pattern consumeRequiredRegexPattern(Map<String, ?> properties, String key) {
        final var out = getRequiredRegexPattern(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * Consumes/Removes a required string value from a Map with the given key.
     *
     * @param properties instance to read and modify
     * @param key property name
     * @return non-blank, non-empty, non-null string
     */
    public static String consumeRequiredString(Map<String, ?> properties, String key) {
        final var out = getRequiredString(properties, key);
        properties.remove(key);
        return out;
    }

    /**
     * See docs on ConfigUtils.getStringList
     *
     * @param properties instance to read and modify
     * @param keyPrefix part before the [0], [1], ... see tests
     * @return record with key and value info
     */
    public static List<ConfigEntry<?>> consumeStringList(
            Map<String, ?> properties, String keyPrefix) {
        final var out = getStringList(properties, keyPrefix);
        properties.keySet().removeIf(key -> key.startsWith(keyPrefix));
        return out;
    }

    /**
     * Copy all entries from properties with given prefix to a new Map
     *
     * @param properties instance to read
     * @param keyPrefix eg. "a.b."
     * @return a new mutable Map with a subset of entries from properties, with given prefix
     */
    public static Map<String, ?> filterByPrefix(Map<String, ?> properties, String keyPrefix) {

        requireNonNull(properties, "properties is required and null.");
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("keyPrefix is required");
        }

        if (!Objects.equals(keyPrefix, keyPrefix.strip())) {
            throw new IllegalArgumentException("keyPrefix must be trimmed");
        }

        //        if (keyPrefix.endsWith(".")) {
        //            throw new IllegalArgumentException("keyPrefix must not end with a
        // dot/period");
        //        }

        final var out = new HashMap<String, Object>(properties.size());
        for (var entry : properties.entrySet()) {
            if (!entry.getKey().startsWith(keyPrefix)) {
                continue;
            }

            out.put(entry.getKey(), entry.getValue());
        }

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
     * See
     * https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.files
     *
     * @return typical list of config file paths, in priority order
     */
    public static List<Path> getCandidateConfigFiles() {
        final var out = new LinkedHashSet<Path>(16);

        final var cwd = FileSystems.getDefault().getPath("").toAbsolutePath().normalize();

        // -- In priority order
        // -- See https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html

        out.add(cwd.resolve("application.properties"));
        out.add(Paths.get("src/main/resources/application.properties"));
        out.add(Paths.get(System.getProperty("user.dir") + "/application.properties"));
        //        out.add(Paths.get(System.getProperty("user.home") + "/application.properties"));

        return out.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(Collectors.toList());
    }

    /**
     * Consumes/Removes a delimited string of bytes from a Map with the given key. Values form 0 to
     * 255 inclusive
     *
     * @param properties instance to read and modify
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @param radix radix to use for parsing (eg. 10, 16)
     * @return possibly empty, never null, and zero null items in the output array
     */
    public static byte[] getDelimitedBytes(
            Map<String, ?> properties, String key, String delim, int radix) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(key != null && !key.isBlank(), "key is required");

        if (delim == null || delim.isBlank()) {
            throw new IllegalArgumentException("delim is required");
        }

        if (delim.contains(".")) {
            throw new IllegalArgumentException("delim must not contain a decimal point");
        }

        final var value = getOptionalString(properties, key, "");
        if (value == null || value.isBlank()) {
            return new byte[0];
        }

        final var tmp =
                Arrays.stream(value.split(delim))
                        .map(String::strip)
                        .filter(s -> !s.isBlank())
                        .map(s -> Short.parseShort(s, radix))
                        .collect(Collectors.toList());

        final var out = new byte[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            final var b = tmp.get(i);
            if (b < 0 || b > 255) {
                throw new NumberFormatException("Byte value out of range. Value:\"" + b + "\"");
            }

            out[i] = b.byteValue();
        }

        return out;
    }

    /**
     * Converts a string of delimited doubles to a list
     *
     * <p>NOTE: trailing empty strings are ignored (supports tailing delimiter). See
     * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#split(java.lang.String)
     *
     * @param properties instance to read
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @return possibly empty, never null, and zero null items in the output list
     */
    public static List<Double> getDelimitedDoubles(
            Map<String, ?> properties, String key, String delim) {

        if (delim == null || delim.isBlank()) {
            throw new IllegalArgumentException("delim is required");
        }

        if (delim.contains(".")) {
            throw new IllegalArgumentException("delim must not contain a decimal point");
        }

        final var value = getOptionalString(properties, key, "");
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(delim))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }

    /**
     * Converts a string of delimited ints to a list
     *
     * <p>NOTE: trailing empty strings are ignored (supports tailing delimiter). See
     * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#split(java.lang.String)
     *
     * @param properties instance to read
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @return possibly empty, never null, and zero null items in the output list
     */
    public static List<Integer> getDelimitedInts(
            Map<String, ?> properties, String key, String delim) {

        if (delim == null || delim.isBlank()) {
            throw new IllegalArgumentException("delim is required");
        }

        final var value = getOptionalString(properties, key, "");
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(delim))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    /**
     * Converts a string of delimited longs to a list
     *
     * <p>NOTE: trailing empty strings are ignored (supports tailing delimiter). See
     * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#split(java.lang.String)
     *
     * @param properties instance to read
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @return possibly empty, never null, and zero null items in the output list
     */
    public static List<Long> getDelimitedLongs(
            Map<String, ?> properties, String key, String delim) {

        if (delim == null || delim.isBlank()) {
            throw new IllegalArgumentException("delim is required");
        }

        final var value = getOptionalString(properties, key, "");
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(delim))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * Converts a string of delimited ports to a list
     *
     * @param properties instance to read
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @return zero or more IP address ports [0, 65535], deduped, (never null)
     */
    public static List<Integer> getDelimitedPorts(
            Map<String, ?> properties, String key, String delim) {

        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(properties, "properties is required and null.");

        if (delim == null || delim.isBlank()) {
            throw new IllegalArgumentException("delim is required");
        }

        if (delim.contains(".")) {
            throw new IllegalArgumentException("delim must not contain a decimal point");
        }

        final var value = getOptionalString(properties, key, "");
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(delim))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .distinct()
                .peek(
                        port -> {
                            if (port > MAX_PORT) {
                                throw new IllegalArgumentException(
                                        "port too high: key=" + key + ", port=" + port);
                            }

                            if (port < MIN_PORT) {
                                throw new IllegalArgumentException(
                                        "port too low: key=" + key + ", port=" + port);
                            }
                        })
                .collect(Collectors.toList());
    }

    /**
     * Converts delimited strings to a list
     *
     * <p>NOTE: trailing empty strings are ignored (supports tailing delimiter). See
     * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#split(java.lang.String)
     *
     * @param properties instance to read
     * @param key property name
     * @param delim eg. comma or semicolon or pipe
     * @param removeBlanks drop/filter out any blank items
     * @return possibly empty, never null
     */
    public static List<String> getDelimitedStrings(
            Map<String, ?> properties, String key, String delim, boolean removeBlanks) {

        if (delim == null || delim.isBlank()) {
            throw new IllegalArgumentException("delim is required");
        }

        final var value = getOptionalString(properties, key, "");
        if (value == null || value.isBlank()) {
            return List.of();
        }

        final var st = Arrays.stream(value.split(delim)).map(String::strip);

        if (removeBlanks) {
            return st.filter(s -> !s.isBlank()).collect(Collectors.toList());
        }

        return st.collect(Collectors.toList());
    }

    /**
     * @param candidates paths to files which may or may not exist
     * @return first existing file or null
     */
    @Nullable
    public static Path getFirstExistingFile(Collection<Path> candidates) {
        requireNonNull(candidates, "candidates is required and null.");
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("at least one candidate path is required");
        }

        for (var candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }

            return candidate.toAbsolutePath().normalize();
        }

        return null;
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @param defaultValue used when value is absent
     * @return value leniently interpreted as a boolean or null if unset and default is null
     */
    @Nullable
    public static Boolean getOptionalBoolean(
            Map<String, ?> properties, String key, @Nullable Boolean defaultValue) {

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
     * @param defaultValue used when value is absent
     * @return an int or the default (which may be null)
     */
    @Nullable
    public static Integer getOptionalInt(
            Map<String, ?> properties, String key, @Nullable Integer defaultValue) {

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
     * @param defaultValue used when value is absent
     * @return a long or the default (which may be null)
     */
    @Nullable
    public static Long getOptionalLong(
            Map<String, ?> properties, String key, @Nullable Long defaultValue) {

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
     * See https://datatracker.ietf.org/doc/html/rfc1340
     *
     * @param properties instance to read
     * @param key property name
     * @return a valid IP port
     */
    public static int getOptionalPort(Map<String, ?> properties, String key, int defaultValue) {
        final var value = getOptionalInt(properties, key, defaultValue);
        if (value == null) {
            throw new IllegalStateException("broken invariant: int value should never be null");
        }

        if (value < MIN_PORT) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must be at least %d", key, MIN_PORT));
        }

        if (value > MAX_PORT) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must be at most %d", key, MAX_PORT));
        }

        // -- This check is redundant only when MIN_PORT >= 0
        if (value < 0) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must non-negative", key));
        }

        return value;
    }

    /**
     * If present, the regex/pattern must compile
     *
     * @param properties instance to read
     * @param key property name
     * @param defaultValue used when value is absent
     * @return a pattern or the default (possibly null)
     */
    @Nullable
    public static Pattern getOptionalRegexPattern(
            Map<String, ?> properties, String key, @Nullable Pattern defaultValue) {
        requireNonNull(defaultValue, "defaultValue is required and null.");
        final var raw = getRequiredString(properties, key);

        try {
            if (raw.isBlank()) {
                return defaultValue;
            }

            return java.util.regex.Pattern.compile(raw);

        } catch (Exception ex) {
            throw new RuntimeException("failed to compile regex pattern for key=" + key, ex);
        }
    }

    /**
     * @param properties instance to read
     * @param key property name
     * @param defaultValue used when value is absent
     * @return a string or the (nullable) default
     */
    @Nullable
    public static String getOptionalString(
            Map<String, ?> properties, String key, @Nullable String defaultValue) {
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
     * @param defaultValue used when value is absent
     * @return a URI or the (nullable) default
     */
    @Nullable
    public static URI getOptionalURI(
            Map<String, ?> properties, String key, @Nullable String defaultValue) {
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
     * @param defaultValue used when value is absent
     * @return a URI or the default, (never null)
     */
    public static URI getOptionalURI(Map<String, ?> properties, String key, URI defaultValue) {
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
    public static Path getRequiredDirPath(Map<String, ?> properties, String key) {
        final var raw = getRequiredString(properties, key);

        final var path = Paths.get(raw).toAbsolutePath().normalize();

        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must be a directory: %s", key, path));
        }

        return path;
    }

    /**
     * Throws when the directory doesn't exist or the path is blank.
     *
     * @param properties instance to read
     * @param key property name
     * @return a path to an existing directory
     */
    public static Path getRequiredExistingDirPath(Map<String, ?> properties, String key) {
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
    public static Path getRequiredExistingFilePath(Map<String, ?> properties, String key) {
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
     * Path must be non-blank, but need not exist.
     *
     * <p>If the path exists, it must be a regular file.
     *
     * @param properties instance to read
     * @param key property name
     * @return a path to an existing file or to a non-existent potential file
     */
    public static Path getRequiredFilePath(Map<String, ?> properties, String key) {
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
    public static int getRequiredInt(Map<String, ?> properties, String key) {
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
    public static long getRequiredLong(Map<String, ?> properties, String key) {
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
    public static Path getRequiredPath(Map<String, ?> properties, String key) {
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
    public static int getRequiredPort(Map<String, ?> properties, String key) {
        final var value = getRequiredInt(properties, key);

        if (value < MIN_PORT) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must be at least %d", key, MIN_PORT));
        }

        if (value > MAX_PORT) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must be at most %d", key, MAX_PORT));
        }

        // -- This check is redundant only when MIN_PORT >= 0
        if (value < 0) {
            throw new IllegalArgumentException(
                    String.format("property '%s' must non-negative", key));
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
    public static Pattern getRequiredRegexPattern(Map<String, ?> properties, String key) {
        final var raw = getRequiredString(properties, key);

        try {
            return java.util.regex.Pattern.compile(raw);

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
    public static String getRequiredString(Map<String, ?> properties, String key) {
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
    public static URI getRequiredURI(Map<String, ?> properties, String key) {
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
    public static UUID getRequiredUUID(Map<String, ?> properties, String key) {
        checkArgument(key != null && !key.isBlank(), "key is required");
        requireNonNull(properties, "properties is required and null.");

        final var value = getRequiredString(properties, key);
        return UUID.fromString(value);
    }

    /**
     * Example: a.b[0].c.d=foo a.b[1].c.d=bar
     *
     * <p>Warns on gaps in the list indexes. Warns if list indexes don't start at zero. Property
     * order in property file is ignored, results returned in ascending index order. Last value wins
     * if there are duplicate keys
     *
     * @param properties instance to read
     * @param keyPrefix part before the [0], [1], ... see tests
     * @return matching entries, prefix removed from the key, values parsed as strings
     * @throws IllegalArgumentException on duplicate index
     */
    public static List<ConfigEntry<?>> getStringList(Map<String, ?> properties, String keyPrefix) {

        requireNonNull(properties, "properties are required and null.");
        checkArgument(keyPrefix != null && !keyPrefix.isBlank(), "keyPrefix is required");
        checkArgument(keyPrefix.equals(keyPrefix.strip()), "keyPrefix must be trimmed");
        checkArgument(!keyPrefix.endsWith("]"), "keyPrefix must not end with ']'");
        checkArgument(!keyPrefix.endsWith("["), "keyPrefix must not end with '['");

        final var entriesByIndex = new TreeMap<Integer, ConfigEntry<?>>();
        int maxIndex = Integer.MIN_VALUE;
        int minIndex = Integer.MAX_VALUE;

        for (var entry : properties.entrySet()) {
            if (!entry.getKey().startsWith(keyPrefix)) {
                continue;
            }

            final var afterPrefix = entry.getKey().substring(keyPrefix.length());
            final var m = AFTER_LIST_PROPERTY_PREFIX.matcher(afterPrefix);
            if (!m.matches()) {
                continue;
            }

            final var listIndex = Integer.parseInt(m.group(1));
            if (listIndex < minIndex) {
                minIndex = listIndex;
            }
            if (listIndex > maxIndex) {
                maxIndex = listIndex;
            }

            // -- Reject partial construction (see tests)
            // -- java.util.Properties only retains the last value for duplicate key
            checkArgument(
                    !entriesByIndex.containsKey(listIndex),
                    "duplicate index for list property: index="
                            + listIndex
                            + ", keyPrefix='"
                            + keyPrefix
                            + "'");

            final var outEntry =
                    ConfigEntry.builder()
                            .fullKey(entry.getKey())
                            .shortKey(m.group(3).strip())
                            .value(entry.getValue())
                            .build();

            entriesByIndex.put(listIndex, outEntry);
        }

        if (entriesByIndex.isEmpty()) {
            return List.of();
        }

        // Invariant: at least one match

        if (minIndex != 0) {
            LOG.warning(
                    "list index should start at zero: minIndex="
                            + minIndex
                            + ", keyPrefix="
                            + keyPrefix);
        }

        if (maxIndex != 0) {
            LOG.warning(
                    "max list index doesn't match list length: size="
                            + entriesByIndex.size()
                            + ", maxIndex="
                            + maxIndex
                            + ", keyPrefix="
                            + keyPrefix);
        }

        return List.copyOf(entriesByIndex.values());
    }

    /**
     * Read from typical paths for config files, parse to a Map
     *
     * @return a mutable map with the properties
     */
    public static Map<String, Object> parseProperties() {
        final var candidates = getCandidateConfigFiles();

        final var config = getFirstExistingFile(candidates);
        requireNonNull(config, "failed to find config file: checked: " + candidates);

        return parseProperties(config);
    }

    /**
     * @param config existing property file path
     * @return a mutable map with the properties
     */
    public static Map<String, Object> parseProperties(Path config) {
        requireNonNull(config, "config is required and null.");
        if (!Files.exists(config)) {
            throw new IllegalArgumentException("failed to find config file: " + config);
        }

        final var properties = new Properties();
        try (final var br = Files.newBufferedReader(config)) {
            properties.load(br);

        } catch (IOException ex) {
            throw new RuntimeException("Failed to read existing config file: " + config, ex);
        }

        return toMap(properties);
    }

    /**
     * throw IllegalArgumentException if any vaules remain in the map
     *
     * <p>presumably all the other values were already consumed
     *
     * @param properties instance to read
     * @param targetType to include in failure exception
     */
    public static void requireFullyConsumed(Map<String, ?> properties, Class<?> targetType) {
        requireFullyConsumed(properties, targetType.getSimpleName());
    }

    /**
     * Convert a Properties instance to a Map.
     *
     * @param properties instance to read
     * @return a mutable map with the same properties as the input
     */
    public static Map<String, Object> toMap(Properties properties) {
        requireNonNull(properties, "properties is required and null.");

        final var out = new HashMap<String, Object>(properties.size());
        for (var entry : properties.entrySet()) {
            // -- Assumption: java.util.Properties keys are always Strings
            out.put((String) entry.getKey(), entry.getValue());
        }

        return out;
    }

    static void requireFullyConsumed(Map<String, ?> properties, String targetTypeName) {
        requireNonNull(properties, "properties is required and null.");
        checkArgument(
                targetTypeName != null && !targetTypeName.isBlank(), "targetTypeName is required");

        if (properties.isEmpty()) {
            return;
        }

        final var extraProps = new TreeSet<>(properties.keySet());
        throw new IllegalArgumentException(
                "Unrecognized/Extra properties for type: " + targetTypeName + " -> " + extraProps);
    }

    private static void checkArgument(boolean expr, String msg) {
        if (!expr) {
            throw new IllegalArgumentException(msg);
        }
    }

    // TODO: getDelimitedBooleans
    // TODO: getDelimitedFloats

    // TODO: consumeDelimitedBooleans
    // TODO: consumeDelimitedFloats

    // TODO:   public static X consumeRequiredFilePath(Map<String, ?> properties, String key, Y y){}
    // TODO:   public static X consumeRequiredURI(Map<String, ?> properties, String key, Y y){}
    // TODO:   public static X consumeRequiredUUID(Map<String, ?> properties, String key, Y y){}
}
