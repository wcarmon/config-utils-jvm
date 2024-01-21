package io.github.wcarmon.config;

import static io.github.wcarmon.config.ConfigUtils.consumeOptionalInt;
import static io.github.wcarmon.config.ConfigUtils.getDelimitedDoubles;
import static io.github.wcarmon.config.ConfigUtils.getDelimitedInts;
import static io.github.wcarmon.config.ConfigUtils.getDelimitedStrings;
import static io.github.wcarmon.config.ConfigUtils.getOptionalBoolean;
import static io.github.wcarmon.config.ConfigUtils.getOptionalInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ConfigUtilsTest {

    @Test
    void methodsMustAcceptAllKindsOfMaps() {
        Map<String, Object> m0 = new HashMap<>();
        Map<String, String> m1 = new HashMap<>();
        Map<String, ?> m2 = new HashMap<>();
        Map<String, ? extends CharSequence> m3 = new HashMap<>();

        consumeOptionalInt(m0, "a", 0);
        consumeOptionalInt(m1, "a", 0);
        consumeOptionalInt(m2, "a", 0);
        consumeOptionalInt(m3, "a", 0);
    }

    @Test
    void testGetDelimitedDoubles() {
        final var k = "a";

        assertEquals(List.of(), getDelimitedDoubles(Map.of(k, ""), k, ","));

        assertEquals(List.of(), getDelimitedDoubles(Map.of(k, ",,,"), k, ","));

        assertEquals(List.of(1.1), getDelimitedDoubles(Map.of(k, "1.1"), k, ","));

        assertEquals(List.of(1.1, 2.2), getDelimitedDoubles(Map.of(k, "1.1, 2.2"), k, ","));

        assertEquals(List.of(1.1, 2.2), getDelimitedDoubles(Map.of(k, ",,1.1, ,,  2.2,"), k, ","));

        assertEquals(List.of(1.1, 2.2), getDelimitedDoubles(Map.of(k, ";1.1; 2.2;;"), k, ";"));

        // TODO: test other double representations, like exp syntax

        try {
            getDelimitedDoubles(Map.of(k, "1.1"), k, ".");
            fail("must fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            getDelimitedDoubles(Map.of(k, "a,b"), k, ",");
            fail("must fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    void testGetDelimitedInts() {
        final var k = "a";

        assertEquals(List.of(), getDelimitedInts(Map.of(k, ""), k, ","));

        assertEquals(List.of(), getDelimitedInts(Map.of(k, ",,,"), k, ","));

        assertEquals(List.of(1, 2, 3), getDelimitedInts(Map.of(k, "1,2, 3"), k, ","));

        assertEquals(List.of(1, 2, 3), getDelimitedInts(Map.of(k, ",,1, ,,  2, 3,"), k, ","));

        assertEquals(List.of(1, 2, 3), getDelimitedInts(Map.of(k, ";1; 2; 3;"), k, ";"));

        try {
            getDelimitedInts(Map.of(k, ";1; 2; 3;"), k, ",");
            fail("must fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    void testGetDelimitedStrings_empty() {
        final var k = "a";

        assertEquals(List.of(), getDelimitedStrings(Map.of(k, ""), k, ",", false));

        assertEquals(List.of(), getDelimitedStrings(Map.of(k, ""), k, ",", true));

        assertEquals(List.of(), getDelimitedStrings(Map.of(k, ""), k, ";", true));

        assertEquals(List.of(), getDelimitedStrings(Map.of(k, ",,,"), k, ",", true));
    }

    @Test
    void testGetDelimitedStrings_multi() {
        final var k = "a";

        assertEquals(
                List.of("foo", "bar", "quux"),
                getDelimitedStrings(Map.of(k, "foo,bar, quux"), k, ",", false));

        assertEquals(
                List.of("", "foo", "bar"),
                getDelimitedStrings(Map.of(k, ",foo, bar"), k, ",", false));

        assertEquals(
                List.of("foo", "quux"), // trailing empty strings ignored
                getDelimitedStrings(Map.of(k, "foo, quux,"), k, ",", false));

        assertEquals(
                List.of("foo", "bar", "quux"),
                getDelimitedStrings(Map.of(k, "foo,  bar,,quux,"), k, ",", true));
    }

    @Test
    void testGetDelimitedStrings_multiline() throws Exception {
        final var k = "a";

        final var propsContent = """
                a=one, \
                  two, \
                  three,
                """;

        final var properties = new Properties();
        properties.load(new StringReader(propsContent));
        final var m = ConfigUtils.toMap(properties);

        assertEquals(
                List.of("one", "two", "three"),
                getDelimitedStrings(m, k, ",", false));
    }

    @Test
    void testGetDelimitedStrings_single() {
        final var k = "a";

        assertEquals(List.of("foo"), getDelimitedStrings(Map.of(k, "foo"), k, ",", false));

        assertEquals(List.of("foo"), getDelimitedStrings(Map.of(k, "foo"), k, ",", true));

        assertEquals(List.of("foo"), getDelimitedStrings(Map.of(k, ";;;foo;;"), k, ";", true));

        assertEquals(List.of("foo"), getDelimitedStrings(Map.of(k, "foo;"), k, ";", true));

        assertEquals(List.of("foo"), getDelimitedStrings(Map.of(k, ",foo,"), k, ",", true));
    }

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

    @Test
    void testIndexBasedArray_positive() throws Exception {
        final var keyPrefix = "a.b";

        final var propsContent = """
                a.b[0].c.d=foo
                a.b[1].c.d=bar
                """;

        final var properties = new Properties();
        properties.load(new StringReader(propsContent));
        final var m = ConfigUtils.toMap(properties);

        final var got = ConfigUtils.consumeStringList(m, keyPrefix);
        assertNotNull(got);
        assertEquals(2, got.size());

        assertEquals("a.b[0].c.d", got.get(0).fullKey());
        assertEquals("c.d", got.get(0).shortKey());
        assertEquals("foo", got.get(0).value());

        assertEquals("a.b[1].c.d", got.get(1).fullKey());
        assertEquals("c.d", got.get(1).shortKey());
        assertEquals("bar", got.get(1).value());
    }

    // TODO: positive case for a.b[i]=5
    // TODO: negative cases for prefix
    // TODO: negative case: missing index
    // TODO: negative case: index must start at zero

}
