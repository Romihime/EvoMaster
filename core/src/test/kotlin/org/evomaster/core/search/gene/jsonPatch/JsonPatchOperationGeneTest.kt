package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for all concrete JsonPatchOperationGene subclasses.
 */
class JsonPatchOperationGeneTest {

    // ─── AddOperationGene ───

    @Test
    fun testAddOperation() {
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "a")))
        val valueGene = StringGene("value", "foo")
        val gene = AddOperationGene(pathGene, valueGene)

        assertEquals("add", gene.operationName())
        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"add","path":"/a","value":"foo"}""", json)
    }

    @Test
    fun testAddOperationWithNumber() {
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "count")))
        val valueGene = IntegerGene("value", 42)
        val gene = AddOperationGene(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"add","path":"/count","value":42}""", json)
    }

    @Test
    fun testAddCopy() {
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "foo")))
        val valueGene = StringGene("value", "bar")
        val original = AddOperationGene(pathGene, valueGene)

        val copy = original.copy() as AddOperationGene
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(original, copy)
    }

    @Test
    fun testAddContainsSameValueAs() {
        val gene1 = AddOperationGene(
            JsonPointerGene("p1", listOf(StringGene("s0", "foo"))),
            StringGene("v1", "bar")
        )
        val gene2 = AddOperationGene(
            JsonPointerGene("p2", listOf(StringGene("s0", "foo"))),
            StringGene("v2", "bar")
        )
        assertTrue(gene1.containsSameValueAs(gene2))
    }

    @Test
    fun testAddNestedPath() {
        val pathGene = JsonPointerGene("path", listOf(
            StringGene("s0", "user"),
            StringGene("s1", "address"),
            StringGene("s2", "city")
        ))
        val valueGene = StringGene("value", "Oslo")
        val gene = AddOperationGene(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"path\":\"/user/address/city\""))
    }

    @Test
    fun testAddArrayPath() {
        val pathGene = JsonPointerGene("path", listOf(
            StringGene("s0", "items"),
            StringGene("s1", "0")
        ))
        val valueGene = StringGene("value", "first")
        val gene = AddOperationGene(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"path\":\"/items/0\""))
    }

    @Test
    fun testAddArrayAppendPath() {
        val pathGene = JsonPointerGene("path", listOf(
            StringGene("s0", "items"),
            StringGene("s1", "-")
        ))
        val valueGene = StringGene("value", "new")
        val gene = AddOperationGene(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"path\":\"/items/-\""))
    }

    @Test
    fun testAddEscapingInPath() {
        val pathGene = JsonPointerGene("path", listOf(
            StringGene("s0", "a~b"),
            StringGene("s1", "c/d")
        ))
        val valueGene = StringGene("value", "test")
        val gene = AddOperationGene(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"path\":\"/a~0b/c~1d\""))
    }

    // ─── RemoveOperationGene ───

    @Test
    fun testRemoveOperation() {
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "c")))
        val gene = RemoveOperationGene(pathGene)

        assertEquals("remove", gene.operationName())
        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"remove","path":"/c"}""", json)
    }

    @Test
    fun testRemoveCopy() {
        val original = RemoveOperationGene(
            JsonPointerGene("path", listOf(StringGene("s0", "foo")))
        )
        val copy = original.copy() as RemoveOperationGene
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(original, copy)
    }

    @Test
    fun testRemoveNoDoubleQuotingOnPath() {
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "foo")))
        val gene = RemoveOperationGene(pathGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"path\":\"/foo\""))
        assertFalse(json.contains("\"path\":\"\"/foo\"\""))
    }

    // ─── ReplaceOperationGene ───

    @Test
    fun testReplaceOperation() {
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "d")))
        val valueGene = IntegerGene("value", 99)
        val gene = ReplaceOperationGene(pathGene, valueGene)

        assertEquals("replace", gene.operationName())
        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"replace","path":"/d","value":99}""", json)
    }

    @Test
    fun testReplaceCopy() {
        val original = ReplaceOperationGene(
            JsonPointerGene("path", listOf(StringGene("s0", "x"))),
            StringGene("value", "y")
        )
        val copy = original.copy() as ReplaceOperationGene
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(original, copy)
    }

    // ─── MoveOperationGene ───

    @Test
    fun testMoveOperation() {
        val fromGene = JsonPointerGene("from", listOf(StringGene("s0", "source")))
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "target")))
        val gene = MoveOperationGene(fromGene, pathGene)

        assertEquals("move", gene.operationName())
        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"move","path":"/target","from":"/source"}""", json)
    }

    @Test
    fun testMoveCopy() {
        val original = MoveOperationGene(
            JsonPointerGene("from", listOf(StringGene("s0", "a"))),
            JsonPointerGene("path", listOf(StringGene("s0", "b")))
        )
        val copy = original.copy() as MoveOperationGene
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(original, copy)
    }

    @Test
    fun testMoveNoDoubleQuotingOnFrom() {
        val fromGene = JsonPointerGene("from", listOf(StringGene("s0", "src")))
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "dst")))
        val gene = MoveOperationGene(fromGene, pathGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"from\":\"/src\""))
        assertFalse(json.contains("\"from\":\"\"/src\"\""))
    }

    // ─── CopyOperationGene ───

    @Test
    fun testCopyOperation() {
        val fromGene = JsonPointerGene("from", listOf(StringGene("s0", "source")))
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "target")))
        val gene = CopyOperationGene(fromGene, pathGene)

        assertEquals("copy", gene.operationName())
        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"copy","path":"/target","from":"/source"}""", json)
    }

    @Test
    fun testCopyOperationCopy() {
        val original = CopyOperationGene(
            JsonPointerGene("from", listOf(StringGene("s0", "a"))),
            JsonPointerGene("path", listOf(StringGene("s0", "b")))
        )
        val copy = original.copy() as CopyOperationGene
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(original, copy)
    }

    // ─── TestOperationGene ───

    @Test
    fun testTestOperation() {
        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "status")))
        val valueGene = StringGene("value", "active")
        val gene = TestOperationGene(pathGene, valueGene)

        assertEquals("test", gene.operationName())
        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"test","path":"/status","value":"active"}""", json)
    }

    @Test
    fun testTestOperationCopy() {
        val original = TestOperationGene(
            JsonPointerGene("path", listOf(StringGene("s0", "x"))),
            IntegerGene("value", 42)
        )
        val copy = original.copy() as TestOperationGene
        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(original, copy)
    }

    // ─── Cross-operation tests ───

    @Test
    fun testDifferentOperationTypesAreNotEqual() {
        val addGene = AddOperationGene(
            JsonPointerGene("p1", listOf(StringGene("s0", "foo"))),
            StringGene("v1", "bar")
        )
        val removeGene = RemoveOperationGene(
            JsonPointerGene("p2", listOf(StringGene("s0", "foo")))
        )
        assertFalse(addGene.containsSameValueAs(removeGene))
    }

    @Test
    fun testOperationNameReturnsCorrectValues() {
        val add = AddOperationGene(JsonPointerGene("p", emptyList()), StringGene("v", "x"))
        val remove = RemoveOperationGene(JsonPointerGene("p", emptyList()))
        val replace = ReplaceOperationGene(JsonPointerGene("p", emptyList()), StringGene("v", "x"))
        val move = MoveOperationGene(JsonPointerGene("f", emptyList()), JsonPointerGene("p", emptyList()))
        val copy = CopyOperationGene(JsonPointerGene("f", emptyList()), JsonPointerGene("p", emptyList()))
        val test = TestOperationGene(JsonPointerGene("p", emptyList()), StringGene("v", "x"))

        assertEquals("add", add.operationName())
        assertEquals("remove", remove.operationName())
        assertEquals("replace", replace.operationName())
        assertEquals("move", move.operationName())
        assertEquals("copy", copy.operationName())
        assertEquals("test", test.operationName())
    }
}