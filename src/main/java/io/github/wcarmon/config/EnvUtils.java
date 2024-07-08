package io.github.wcarmon.config;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;

/** Utilities related to environment variables */
public final class EnvUtils {

    private EnvUtils() {}

    /**
     * Builds a pretty string for the current environment variables
     *
     * @return delimited string of environment variables, with sorted keys and values abbreviated
     */
    public static String prettyPrintEnvVars() {
        return prettyPrintEnvVars("\n", 80);
    }

    /**
     * Builds a pretty string for the current environment variables
     *
     * @param delim     delimiter between environment variables
     * @param maxLength maximum length of environment variable values
     * @return delimited string of environment variables, with sorted keys and values abbreviated
     */
    public static String prettyPrintEnvVars(String delim, int maxLength) {
        if (maxLength < 1) {
            throw new IllegalArgumentException("maxLength must be greater than 0");
        }

        if (delim == null) {
            delim = "";
        }

        final Map<String, String> envs = new TreeMap<>(System.getenv());

        final StringBuilder sb = new StringBuilder(2048);

        for (final var entry : envs.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).contains("pass")) {
                continue;
            }

            sb.append(entry.getKey());
            sb.append(" = ");
            sb.append(abbreviate(entry.getValue(), maxLength));
            sb.append(delim);
        }

        return sb.toString();
    }

    private static String abbreviate(@Nullable String value, int maxLength) {
        if (maxLength < 1) {
            throw new IllegalArgumentException("maxLength must be greater than 0");
        }

        if (value == null || value.isEmpty()) {
            return "<null>";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }
}
