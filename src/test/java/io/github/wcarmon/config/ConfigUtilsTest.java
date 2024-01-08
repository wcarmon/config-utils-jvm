package io.github.wcarmon.config;

import static io.github.wcarmon.config.ConfigUtils.getOptionalBoolean;
import static io.github.wcarmon.config.ConfigUtils.getOptionalInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ConfigUtilsTest {

    @Test
    void testGetOptionalBoolean() {
        final var k = "b";

        assertFalse(getOptionalBoolean(Map.of(), k, false));
        assertFalse(getOptionalBoolean(Map.of(k, false), k, false));
        assertFalse(getOptionalBoolean(Map.of(k, false), k, true));

        assertTrue(getOptionalBoolean(Map.of(), k, true));
        assertTrue(getOptionalBoolean(Map.of(k, true), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, true), k, true));

        // -- non-primitive
        assertFalse(getOptionalBoolean(Map.of(k, Boolean.FALSE), k, false));
        assertFalse(getOptionalBoolean(Map.of(k, Boolean.FALSE), k, true));
        assertTrue(getOptionalBoolean(Map.of(k, Boolean.TRUE), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, Boolean.TRUE), k, true));

        // -- Number
        assertTrue(getOptionalBoolean(Map.of(k, 1), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, 1L), k, false));

        // -- Strings
        assertTrue(getOptionalBoolean(Map.of(k, "1"), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, "on"), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, "true"), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, "tRue"), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, "TRUE"), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, "True"), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, "yes"), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, "YES"), k, false));
        assertTrue(getOptionalBoolean(Map.of(k, "yEs"), k, false));

        // -- null default
        assertNull(getOptionalBoolean(Map.of(), k, null));
    }

    @Test
    void testGetOptionalInt() {

        final var k = "i";

        final var tmp = new HashMap<String, Object>(2);
        tmp.put(k, null);

        // -- null check
        assertEquals(1, getOptionalInt(tmp, k, 1));

        // -- missing k check
        assertEquals(2, getOptionalInt(Map.of(), k, 2));

        // -- primitive
        assertEquals(3, getOptionalInt(Map.of(k, 3), k, -1));

        // -- non-primitive
        assertEquals(4, getOptionalInt(Map.of(k, Integer.valueOf(4)), k, -1));

        // -- narrower type (primitive)
        assertEquals(5, getOptionalInt(Map.of(k, (short) 5), k, -1));

        // -- narrower type (non-primitive)
        assertEquals(6, getOptionalInt(Map.of(k, Short.valueOf((short) 6)), k, -1));

        // -- wider type, but still fits (primitive)
        assertEquals(7, getOptionalInt(Map.of(k, 7L), k, -1));

        // -- wider type, but still fits (non-primitive)
        assertEquals(8, getOptionalInt(Map.of(k, Long.valueOf(8)), k, -1));

        // -- wider type, too big
        try {
            getOptionalInt(Map.of(k, Long.MAX_VALUE), k, -1);
            fail("must throw");
        } catch (IllegalArgumentException ex) {
            // -- expected
        }

        // -- wider type, too big
        try {
            getOptionalInt(Map.of(k, Long.MAX_VALUE), k, -1);
            fail("must throw");
        } catch (IllegalArgumentException ex) {
            // -- expected
        }

        // -- null default
        assertNull(getOptionalInt(Map.of(), k, null));
    }
}
