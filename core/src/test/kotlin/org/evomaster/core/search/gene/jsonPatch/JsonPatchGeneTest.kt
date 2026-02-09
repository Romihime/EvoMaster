package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPatchGeneTest {

    @Test
    fun testEmptyPatch() {
        val patch = JsonPatchGene("patch", null)
        // Should have at least one operation from init
        assertTrue(patch.operations.isNotEmpty())
    }

    @Test
    fun testSingleOperation() {
        val patch = JsonPatchGene("patch", null)
        // Clear default operations
        patch.getViewOfChildren().toList().forEach { patch.killChild(it) }

        val pathGene = JsonPointerGene("path", mutableListOf(StringGene("s0", "a")))
        val valueGene = StringGene("value", "foo")
        val operation = JsonPatchOperationGene.createAdd(pathGene, valueGene)

        patch.addOperation(operation)

        val json = patch.getValueAsPrintableString()
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"op\":\"add\""))
        assertTrue(json.contains("\"path\":\"/a\""))
        assertTrue(json.contains("\"value\":\"foo\""))
    }

    @Test
    fun testMultipleOperations() {
        val patch = JsonPatchGene("patch", null)
        // Clear default operations
        patch.getViewOfChildren().toList().forEach { patch.killChild(it) }

        val path1 = JsonPointerGene("p1", mutableListOf(StringGene("s", "a")))
        val path2 = JsonPointerGene("p2", mutableListOf(StringGene("s", "b")))
        val path3 = JsonPointerGene("p3", mutableListOf(StringGene("s", "c")))

        patch.addOperation(JsonPatchOperationGene.createAdd(path1, StringGene("v", "1")))
        patch.addOperation(JsonPatchOperationGene.createRemove(path2))
        patch.addOperation(JsonPatchOperationGene.createReplace(path3, IntegerGene("v", 42)))

        val json = patch.getValueAsPrintableString()

        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"op\":\"add\""))
        assertTrue(json.contains("\"op\":\"remove\""))
        assertTrue(json.contains("\"op\":\"replace\""))
        assertEquals(3, patch.operations.size)
    }

    @Test
    fun testRandomize() {
        val patch = JsonPatchGene("patch", null)
        val randomness = Randomness()
        randomness.updateSeed(42) // Fixed seed for reproducibility

        patch.randomize(randomness, false)

        assertTrue(patch.operations.isNotEmpty())
        assertTrue(patch.operations.size >= 1)
        assertTrue(patch.operations.size <= 5)

        val json = patch.getValueAsPrintableString()
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
    }

    @Test
    fun testCopy() {
        val patch = JsonPatchGene("patch", null)
        // Clear default operations
        patch.getViewOfChildren().toList().forEach { patch.killChild(it) }

        val path1 = JsonPointerGene("p1", mutableListOf(StringGene("s", "a")))
        val path2 = JsonPointerGene("p2", mutableListOf(StringGene("s", "b")))

        patch.addOperation(JsonPatchOperationGene.createAdd(path1, StringGene("v", "1")))
        patch.addOperation(JsonPatchOperationGene.createRemove(path2))

        val copy = patch.copy() as JsonPatchGene

        assertEquals(patch.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(patch, copy)
        assertEquals(2, copy.operations.size)
    }

    @Test
    fun testContainsSameValueAs() {
        val patch1 = JsonPatchGene("p1", null)
        patch1.getViewOfChildren().toList().forEach { patch1.killChild(it) }

        val patch2 = JsonPatchGene("p2", null)
        patch2.getViewOfChildren().toList().forEach { patch2.killChild(it) }

        val path = JsonPointerGene("path", mutableListOf(StringGene("s", "a")))
        val value = StringGene("value", "foo")

        patch1.addOperation(JsonPatchOperationGene.createAdd(path.copy() as JsonPointerGene, value.copy() as StringGene))
        patch2.addOperation(JsonPatchOperationGene.createAdd(path.copy() as JsonPointerGene, value.copy() as StringGene))

        assertTrue(patch1.containsSameValueAs(patch2))
    }

    @Test
    fun testContainsSameValueAsDifferentSize() {
        val patch1 = JsonPatchGene("p1", null)
        patch1.getViewOfChildren().toList().forEach { patch1.killChild(it) }

        val patch2 = JsonPatchGene("p2", null)
        patch2.getViewOfChildren().toList().forEach { patch2.killChild(it) }

        val path = JsonPointerGene("path", mutableListOf(StringGene("s", "a")))

        patch1.addOperation(JsonPatchOperationGene.createRemove(path.copy() as JsonPointerGene))
        patch2.addOperation(JsonPatchOperationGene.createRemove(path.copy() as JsonPointerGene))
        patch2.addOperation(JsonPatchOperationGene.createRemove(path.copy() as JsonPointerGene))

        assertFalse(patch1.containsSameValueAs(patch2))
    }

    @Test
    fun testRemoveOperation() {
        val patch = JsonPatchGene("patch", null)
        patch.getViewOfChildren().toList().forEach { patch.killChild(it) }

        val path1 = JsonPointerGene("p1", mutableListOf(StringGene("s", "a")))
        val path2 = JsonPointerGene("p2", mutableListOf(StringGene("s", "b")))
        val path3 = JsonPointerGene("p3", mutableListOf(StringGene("s", "c")))

        patch.addOperation(JsonPatchOperationGene.createRemove(path1))
        patch.addOperation(JsonPatchOperationGene.createRemove(path2))
        patch.addOperation(JsonPatchOperationGene.createRemove(path3))

        assertEquals(3, patch.operations.size)

        patch.removeOperation(1)

        assertEquals(2, patch.operations.size)
    }

    @Test
    fun testComplexPatch() {
        val patch = JsonPatchGene("patch", null)
        patch.getViewOfChildren().toList().forEach { patch.killChild(it) }

        // Add various operation types
        patch.addOperation(JsonPatchOperationGene.createAdd(
            JsonPointerGene("p1", mutableListOf(StringGene("s", "name"))),
            StringGene("v", "John")
        ))

        patch.addOperation(JsonPatchOperationGene.createReplace(
            JsonPointerGene("p2", mutableListOf(StringGene("s", "age"))),
            IntegerGene("v", 30)
        ))

        patch.addOperation(JsonPatchOperationGene.createRemove(
            JsonPointerGene("p3", mutableListOf(StringGene("s", "temp")))
        ))

        patch.addOperation(JsonPatchOperationGene.createMove(
            JsonPointerGene("from", mutableListOf(StringGene("s", "old"))),
            JsonPointerGene("to", mutableListOf(StringGene("s", "new")))
        ))

        val json = patch.getValueAsPrintableString()

        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertEquals(4, patch.operations.size)

        // Verify all operations are present
        assertTrue(json.contains("\"op\":\"add\""))
        assertTrue(json.contains("\"op\":\"replace\""))
        assertTrue(json.contains("\"op\":\"remove\""))
        assertTrue(json.contains("\"op\":\"move\""))
    }

    @Test
    fun testValidJsonStructure() {
        val patch = JsonPatchGene("patch", null)
        patch.getViewOfChildren().toList().forEach { patch.killChild(it) }

        patch.addOperation(JsonPatchOperationGene.createAdd(
            JsonPointerGene("p", mutableListOf(StringGene("s", "test"))),
            StringGene("v", "value")
        ))

        val json = patch.getValueAsPrintableString()

        // Should be valid JSON array syntax
        assertTrue(json.matches(Regex("\\[\\{.*\\}\\]")))
    }
}