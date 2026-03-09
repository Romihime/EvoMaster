package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPointerGeneTest {

    @Test
    fun testBasicPath() {
        val pointer = JsonPointerGene("ptr", listOf(
            StringGene("s0", "user"),
            StringGene("s1", "name")
        ))
        assertEquals("/user/name", pointer.getValueAsPrintableString())
    }

    @Test
    fun testRootPath() {
        val pointer = JsonPointerGene("ptr", emptyList())
        assertEquals("/", pointer.getValueAsPrintableString())
    }

    @Test
    fun testEscapingTilde() {
        val pointer = JsonPointerGene("ptr", listOf(
            StringGene("s0", "a~b")
        ))
        assertEquals("/a~0b", pointer.getValueAsPrintableString())
    }

    @Test
    fun testEscapingSlash() {
        val pointer = JsonPointerGene("ptr", listOf(
            StringGene("s0", "c/d")
        ))
        assertEquals("/c~1d", pointer.getValueAsPrintableString())
    }

    @Test
    fun testEscapingBoth() {
        val pointer = JsonPointerGene("ptr", listOf(
            StringGene("s0", "a~b"),
            StringGene("s1", "c/d")
        ))
        assertEquals("/a~0b/c~1d", pointer.getValueAsPrintableString())
    }

    @Test
    fun testArrayIndex() {
        val pointer = JsonPointerGene("ptr", listOf(
            StringGene("s0", "items"),
            StringGene("s1", "0")
        ))
        assertEquals("/items/0", pointer.getValueAsPrintableString())
    }

    @Test
    fun testArrayAppend() {
        val pointer = JsonPointerGene("ptr", listOf(
            StringGene("s0", "items"),
            StringGene("s1", "-")
        ))
        assertEquals("/items/-", pointer.getValueAsPrintableString())
    }

    @Test
    fun testFromPath() {
        val pointer = JsonPointerGene.fromPath("ptr", "/user/name")
        assertEquals("/user/name", pointer.getValueAsPrintableString())
        assertEquals(2, pointer.segments.size)
        assertEquals("user", pointer.segments[0].value)
        assertEquals("name", pointer.segments[1].value)
    }

    @Test
    fun testFromPathWithEscaping() {
        val pointer = JsonPointerGene.fromPath("ptr", "/a~0b/c~1d")
        assertEquals("/a~0b/c~1d", pointer.getValueAsPrintableString())
        assertEquals("a~b", pointer.segments[0].value)
        assertEquals("c/d", pointer.segments[1].value)
    }

    @Test
    fun testFromEmptyPath() {
        val pointer = JsonPointerGene.fromPath("ptr", "")
        assertEquals("/", pointer.getValueAsPrintableString())
        assertEquals(0, pointer.segments.size)
    }

    @Test
    fun testFromRootPath() {
        val pointer = JsonPointerGene.fromPath("ptr", "/")
        assertEquals("/", pointer.getValueAsPrintableString())
        assertEquals(0, pointer.segments.size)
    }

    @Test
    fun testCopy() {
        val original = JsonPointerGene("ptr", listOf(
            StringGene("s0", "foo"),
            StringGene("s1", "bar")
        ))

        val copy = original.copy() as JsonPointerGene

        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(original, copy)
        assertEquals(original.segments.size, copy.segments.size)
    }

    @Test
    fun testContainsSameValueAs() {
        val pointer1 = JsonPointerGene("p1", listOf(
            StringGene("s0", "user"),
            StringGene("s1", "name")
        ))

        val pointer2 = JsonPointerGene("p2", listOf(
            StringGene("s0", "user"),
            StringGene("s1", "name")
        ))

        assertTrue(pointer1.containsSameValueAs(pointer2))
    }

    @Test
    fun testContainsSameValueAsDifferent() {
        val pointer1 = JsonPointerGene("p1", listOf(
            StringGene("s0", "user")
        ))

        val pointer2 = JsonPointerGene("p2", listOf(
            StringGene("s0", "admin")
        ))

        assertFalse(pointer1.containsSameValueAs(pointer2))
    }

    @Test
    fun testAddSegment() {
        val pointer = JsonPointerGene("ptr", listOf(
            StringGene("s0", "user")
        ))

        pointer.addSegment("name")

        assertEquals("/user/name", pointer.getValueAsPrintableString())
        assertEquals(2, pointer.segments.size)
    }

    @Test
    fun testRemoveLastSegment() {
        val pointer = JsonPointerGene("ptr", listOf(
            StringGene("s0", "user"),
            StringGene("s1", "name")
        ))

        pointer.removeLastSegment()

        assertEquals("/user", pointer.getValueAsPrintableString())
        assertEquals(1, pointer.segments.size)
    }

    @Test
    fun testRemoveLastSegmentFromEmpty() {
        val pointer = JsonPointerGene("ptr", emptyList())

        pointer.removeLastSegment()

        assertEquals("/", pointer.getValueAsPrintableString())
        assertEquals(0, pointer.segments.size)
    }

    @Test
    fun testUnsafeSetFromStringValue() {
        val pointer = JsonPointerGene("ptr")

        assertTrue(pointer.unsafeSetFromStringValue("/user/name"))
        assertEquals("/user/name", pointer.getValueAsPrintableString())
        assertEquals(2, pointer.segments.size)
    }

    @Test
    fun testUnsafeSetFromStringValueWithEscaping() {
        val pointer = JsonPointerGene("ptr")

        assertTrue(pointer.unsafeSetFromStringValue("/a~0b/c~1d"))
        assertEquals("a~b", pointer.segments[0].value)
        assertEquals("c/d", pointer.segments[1].value)
    }

    @Test
    fun testRandomize() {
        val pointer = JsonPointerGene("ptr")
        val randomness = Randomness()
        randomness.updateSeed(42)

        pointer.randomize(randomness, false)

        // After randomize, segments should be within bounds
        assertTrue(pointer.segments.size in 0..4)
    }

    @Test
    fun testMutationWeightAtLeastOne() {
        val pointer = JsonPointerGene("ptr", emptyList())
        assertTrue(pointer.mutationWeight() >= 1.0)
    }

    @Test
    fun testUnsafeCopyValueFrom() {
        val p1 = JsonPointerGene("p1", listOf(StringGene("s0", "a"), StringGene("s1", "b")))
        val p2 = JsonPointerGene("p2", emptyList())

        p2.unsafeCopyValueFrom(p1)

        assertTrue(p2.containsSameValueAs(p1))
        assertEquals(2, p2.segments.size)
    }
}