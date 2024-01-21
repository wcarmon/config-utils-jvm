package io.github.wcarmon.config;

import static io.github.wcarmon.config.ConfigUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    void testBuildMutableMapForKeyPrefix() {
        // -- Arrange
        final var keyPrefix = "a.b.";

        final var m =
                Map.of(
                        keyPrefix + "c",
                        "foo",
                        // --
                        keyPrefix + "c.d",
                        "bar",
                        // --
                        keyPrefix + "e",
                        "baz",
                        // --
                        keyPrefix + "f.g",
                        "quux",
                        // --
                        "hh",
                        "aaa",
                        // --
                        "yy",
                        "zzz");

        // -- Act
        final var got = buildMutableMapForKeyPrefix(m, keyPrefix);

        // -- Assert
        assertEquals(4, got.size());
        assertFalse(got.containsKey("hh"));
        assertFalse(got.containsKey("yy"));
        assertTrue(got.containsKey(keyPrefix + "c"));
        assertTrue(got.containsKey(keyPrefix + "c.d"));
        assertTrue(got.containsKey(keyPrefix + "e"));
        assertTrue(got.containsKey(keyPrefix + "f.g"));
    }

    @Test
    void testConsumeDelimitedBytes_tooBig() {

        // -- Arrange
        final var k = "a";

        // -- Act

        try {
            consumeDelimitedBytes(Map.of(k, "12, 256"), k, ",", 10);
            fail("must throw");
        } catch (NumberFormatException expected) {
            // expected
        }
    }

    @Test
    void testConsumeDelimitedBytes_tooSmall() {

        // -- Arrange
        final var k = "a";

        // -- Act

        try {
            consumeDelimitedBytes(Map.of(k, "16, -1"), k, ",", 10);
            fail("must throw");
        } catch (NumberFormatException expected) {
            // expected
        }
    }

    @Test
    void testGetDelimitedBytes() {

        // -- Arrange
        final var k = "a";

        // -- Act
        final var got = getDelimitedBytes(Map.of(k, "133,0, 44,67,255 "), k, ",", 10);

        // -- Assert
        assertEquals(5, got.length);
        assertEquals(133, Byte.toUnsignedInt(got[0]));
        assertEquals(0, Byte.toUnsignedInt(got[1]));
        assertEquals(44, Byte.toUnsignedInt(got[2]));
        assertEquals(67, Byte.toUnsignedInt(got[3]));
        assertEquals(255, Byte.toUnsignedInt(got[4]));
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

        final var propsContent =
                """
                        a=one, \
                          two, \
                          three,
                        """;

        final var properties = new Properties();
        properties.load(new StringReader(propsContent));
        final var m = ConfigUtils.toMap(properties);

        assertEquals(List.of("one", "two", "three"), getDelimitedStrings(m, k, ",", false));
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
    void testGetFirstExistingFile_noneExist() throws Exception {

        final var missingFile0 = Paths.get("aaa.txt");
        final var missingFile1 = Paths.get("bbb.txt");

        assertFalse(Files.exists(missingFile0));
        assertFalse(Files.exists(missingFile1));

        // -- Act
        final var got = getFirstExistingFile(List.of(missingFile0, missingFile1));

        // -- Assert
        assertNull(got);
    }

    @Test
    void testGetFirstExistingFile_oneExists() throws Exception {

        final var tmpFile = Files.createTempFile("test.", "");

        final var missingFile0 = Paths.get("aaa.txt");
        final var missingFile1 = Paths.get("bbb.txt");

        assertFalse(Files.exists(missingFile0));
        assertFalse(Files.exists(missingFile1));
        assertTrue(Files.exists(tmpFile));

        // -- Act
        final var got = getFirstExistingFile(List.of(missingFile0, tmpFile, missingFile1));

        // -- Assert
        assertNotNull(got);
        assertEquals(tmpFile, got);
        Files.delete(tmpFile);
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
    void testGetPorts() {
        // -- Arrange
        final var k = "a";

        // -- Act
        final var got = getDelimitedPorts(Map.of(k, "1024,80,80,80,, 8080,443,,,65535,,"), k, ",");

        // -- Assert
        assertEquals(5, got.size());

        assertEquals(1024, got.get(0));
        assertEquals(80, got.get(1));
        assertEquals(8080, got.get(2));
        assertEquals(443, got.get(3));
        assertEquals(65535, got.get(4));
    }

    @Test
    void testGetPorts_nonInt() {
        final var k = "a";

        try {
            getDelimitedPorts(Map.of(k, "zz"), k, ",");
            fail("must throw");
        } catch (NumberFormatException expected) {
            // expected
        }
    }

    @Test
    void testGetPorts_tooHigh() {
        final var k = "a";

        try {
            getDelimitedPorts(Map.of(k, "65536,80"), k, ",");
            fail("must throw");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("65536"));
            assertTrue(expected.getMessage().contains("high"));
        }
    }

    @Test
    void testGetPorts_tooLow() {
        final var k = "a";

        try {
            getDelimitedPorts(Map.of(k, "-1,-2"), k, ",");
            fail("must throw");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("-1"));
            assertTrue(expected.getMessage().contains("low"));
        }
    }

    @Test
    void testIndexBasedArray_badPrefix() throws Exception {
        // -- Arrange
        final var propsContent = """
                a.b[15].c.d=bar
                """;

        final var properties = new Properties();
        properties.load(new StringReader(propsContent));
        final var m = ConfigUtils.toMap(properties);

        List.of(
                        "", " ", " a.b ", " a.b", "\t", "a.b ", "a.b[", "a.b[-1]", "a.b[0] ",
                        "a.b[0]", "a.b[]", "a.b[n] ", "a.b[n] ", "a.b[n]")
                .forEach(
                        keyPrefix -> {

                            // -- Act & Assert
                            try {
                                ConfigUtils.consumeStringList(m, keyPrefix);

                                fail("must throw for keyPrefix=" + keyPrefix);
                            } catch (IllegalArgumentException expected) {
                                // -- expected
                            }
                        });
    }

    @Test
    void testIndexBasedArray_noneMatchPrefix() throws Exception {
        final var keyPrefix = "a.b";

        final var propsContent =
                """
                        a.b.c=v
                        a.b.c[0]=v
                        b[0]=v
                        z.b[0]=v
                        """;

        final var properties = new Properties();
        properties.load(new StringReader(propsContent));
        final var m = ConfigUtils.toMap(properties);

        // -- Act
        final var got = ConfigUtils.consumeStringList(m, keyPrefix);

        // -- Assert
        assertNotNull(got);
        assertEquals(0, got.size());
    }

    @Test
    void testIndexBasedArray_positive_noSuffix() throws Exception {
        final var keyPrefix = "a.b";

        final var propsContent =
                """
                        a.b[8]=baz
                        a.b[4]=quux

                        a.b=y
                        a.b.c=z
                        """;

        final var properties = new Properties();
        properties.load(new StringReader(propsContent));
        final var m = ConfigUtils.toMap(properties);

        // -- Act
        final var got = ConfigUtils.consumeStringList(m, keyPrefix);

        // -- Assert
        assertNotNull(got);
        assertEquals(2, got.size());

        assertEquals("a.b[4]", got.get(0).fullKey());
        assertEquals("", got.get(0).shortKey());
        assertEquals("quux", got.get(0).value());

        assertEquals("a.b[8]", got.get(1).fullKey());
        assertEquals("", got.get(1).shortKey());
        assertEquals("baz", got.get(1).value());
    }

    @Test
    void testIndexBasedArray_positive_withSuffix() throws Exception {
        final var keyPrefix = "a.b";

        final var propsContent =
                """
                        a.b[15].c.d=bar
                        a.b[3].c.d=foo

                        e.g.f=7
                        """;

        final var properties = new Properties();
        properties.load(new StringReader(propsContent));
        final var m = ConfigUtils.toMap(properties);

        // -- Act
        final var got = ConfigUtils.consumeStringList(m, keyPrefix);

        // -- Assert
        assertNotNull(got);
        assertEquals(2, got.size());

        final var first = got.get(0);
        assertEquals("a.b[3].c.d", first.fullKey());
        assertEquals("c.d", first.shortKey());
        assertEquals("foo", first.value());

        final var second = got.get(1);
        assertEquals("a.b[15].c.d", second.fullKey());
        assertEquals("c.d", second.shortKey());
        assertEquals("bar", second.value());
    }

    @Test
    void testIndexBasedArray_rejectPartialConstruction() throws Exception {

        final var keyPrefix = "b";
        final var propsContent =
                """
                        b[15].c=bar
                        b[15].c.d=quux
                        """;

        final var properties = new Properties();
        properties.load(new StringReader(propsContent));
        final var m = ConfigUtils.toMap(properties);

        // -- Act & Assert
        try {
            ConfigUtils.consumeStringList(m, keyPrefix);
            fail("must throw");

        } catch (IllegalArgumentException expected) {
            // -- expected
        }
    }
}
