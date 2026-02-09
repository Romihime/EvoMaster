package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPatchOperationGeneTest {

    @Test
    fun testAddOperation() {
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "a")))
        val valueGene = StringGene("value", "foo")
        val gene = JsonPatchOperationGene.createAdd(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"add","path":"/a","value":"foo"}""", json)
    }

    @Test
    fun testAddOperationWithNumber() {
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "count")))
        val valueGene = IntegerGene("value", 42)
        val gene = JsonPatchOperationGene.createAdd(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"op\":\"add\""))
        assertTrue(json.contains("\"path\":\"/count\""))
        assertTrue(json.contains("\"value\":42"))
    }

    @Test
    fun testRemoveOperation() {
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "c")))
        val gene = JsonPatchOperationGene.createRemove(pathGene)

        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"remove","path":"/c"}""", json)
    }

    @Test
    fun testReplaceOperation() {
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "d")))
        val valueGene = IntegerGene("value", 99)
        val gene = JsonPatchOperationGene.createReplace(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"op\":\"replace\""))
        assertTrue(json.contains("\"path\":\"/d\""))
        assertTrue(json.contains("\"value\":99"))
    }

    @Test
    fun testMoveOperation() {
        val fromGene = JsonPointerGene("from", mutableListOf(StringGene("s0", "source")))
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "target")))
        val gene = JsonPatchOperationGene.createMove(fromGene, pathGene)

        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"move","path":"/target","from":"/source"}""", json)
    }

    @Test
    fun testCopyOperation() {
        val fromGene = JsonPointerGene("from", mutableListOf(StringGene("s0", "source")))
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "target")))
        val gene = JsonPatchOperationGene.createCopy(fromGene, pathGene)

        val json = gene.getValueAsPrintableString()
        assertEquals("""{"op":"copy","path":"/target","from":"/source"}""", json)
    }

    @Test
    fun testTestOperation() {
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "status")))
        val valueGene = StringGene("value", "active")
        val gene = JsonPatchOperationGene.createTest(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"op\":\"test\""))
        assertTrue(json.contains("\"path\":\"/status\""))
        assertTrue(json.contains("\"value\":\"active\""))
    }

    @Test
    fun testNestedPath() {
        val pathGene = JsonPointerGene("path", mutableListOf(
            StringGene("s0", "user"),
            StringGene("s1", "address"),
            StringGene("s2", "city")
        ))
        val valueGene = StringGene("value", "Oslo")
        val gene = JsonPatchOperationGene.createAdd(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"path\":\"/user/address/city\""))
    }

    @Test
    fun testArrayPath() {
        val pathGene = JsonPointerGene("path", mutableListOf(
            StringGene("s0", "items"),
            StringGene("s1", "0")
        ))
        val valueGene = StringGene("value", "first")
        val gene = JsonPatchOperationGene.createAdd(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"path\":\"/items/0\""))
    }

    @Test
    fun testArrayAppendPath() {
        val pathGene = JsonPointerGene("path", mutableListOf(
            StringGene("s0", "items"),
            StringGene("s1", "-")
        ))
        val valueGene = StringGene("value", "new")
        val gene = JsonPatchOperationGene.createAdd(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"path\":\"/items/-\""))
    }

    @Test
    fun testCopy() {
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "foo")))
        val valueGene = StringGene("value", "bar")
        val original = JsonPatchOperationGene.createAdd(pathGene, valueGene)

        val copy = original.copy() as JsonPatchOperationGene

        assertEquals(original.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(original, copy)
    }

    @Test
    fun testContainsSameValueAs() {
        val path1 = JsonPointerGene("p1", mutableListOf(StringGene("s0", "foo")))
        val value1 = StringGene("v1", "bar")
        val gene1 = JsonPatchOperationGene.createAdd(path1, value1)

        val path2 = JsonPointerGene("p2", mutableListOf(StringGene("s0", "foo")))
        val value2 = StringGene("v2", "bar")
        val gene2 = JsonPatchOperationGene.createAdd(path2, value2)

        assertTrue(gene1.containsSameValueAs(gene2))
    }

    @Test
    fun testContainsSameValueAsDifferentOp() {
        val path1 = JsonPointerGene("p1", mutableListOf(StringGene("s0", "foo")))
        val value1 = StringGene("v1", "bar")
        val gene1 = JsonPatchOperationGene.createAdd(path1, value1)

        val path2 = JsonPointerGene("p2", mutableListOf(StringGene("s0", "foo")))
        val gene2 = JsonPatchOperationGene.createRemove(path2)

        assertFalse(gene1.containsSameValueAs(gene2))
    }

    @Test
    fun testFieldConsistencyForAdd() {
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "a")))
        val valueGene = StringGene("value", "foo")
        val gene = JsonPatchOperationGene.createAdd(pathGene, valueGene)

        assertTrue(gene.valueGene.isActive)
        assertFalse(gene.fromGene.isActive)
    }

    @Test
    fun testFieldConsistencyForRemove() {
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "a")))
        val gene = JsonPatchOperationGene.createRemove(pathGene)

        assertFalse(gene.valueGene.isActive)
        assertFalse(gene.fromGene.isActive)
    }

    @Test
    fun testFieldConsistencyForMove() {
        val fromGene = JsonPointerGene("from", mutableListOf(StringGene("s0", "source")))
        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "target")))
        val gene = JsonPatchOperationGene.createMove(fromGene, pathGene)

        assertFalse(gene.valueGene.isActive)
        assertTrue(gene.fromGene.isActive)
    }

    @Test
    fun testEscapingInPath() {
        val pathGene = JsonPointerGene("path", mutableListOf(
            StringGene("s0", "a~b"),
            StringGene("s1", "c/d")
        ))
        val valueGene = StringGene("value", "test")
        val gene = JsonPatchOperationGene.createAdd(pathGene, valueGene)

        val json = gene.getValueAsPrintableString()
        assertTrue(json.contains("\"path\":\"/a~0b/c~1d\""))
    }
}