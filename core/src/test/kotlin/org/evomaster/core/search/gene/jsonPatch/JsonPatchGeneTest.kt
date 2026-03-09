package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPatchGeneTest {

    @Test
    fun testNewPatchIsEmpty() {
        val patch = JsonPatchGene("patch", null)
        // Without init side-effects, a new patch starts empty
        assertTrue(patch.operations.isEmpty())
    }

    @Test
    fun testSingleOperation() {
        val patch = JsonPatchGene("patch", null)

        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "a")))
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

        val path1 = JsonPointerGene("p1", listOf(StringGene("s", "a")))
        val path2 = JsonPointerGene("p2", listOf(StringGene("s", "b")))
        val path3 = JsonPointerGene("p3", listOf(StringGene("s", "c")))

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
        randomness.updateSeed(42)

        patch.randomize(randomness, false)

        assertTrue(patch.operations.isNotEmpty())
        assertTrue(patch.operations.size >= JsonPatchGene.MIN_OPERATIONS)
        assertTrue(patch.operations.size <= JsonPatchGene.MAX_OPERATIONS)

        val json = patch.getValueAsPrintableString()
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
    }

    @Test
    fun testCopy() {
        val patch = JsonPatchGene("patch", null)

        val path1 = JsonPointerGene("p1", listOf(StringGene("s", "a")))
        val path2 = JsonPointerGene("p2", listOf(StringGene("s", "b")))

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
        val patch2 = JsonPatchGene("p2", null)

        val path = JsonPointerGene("path", listOf(StringGene("s", "a")))
        val value = StringGene("value", "foo")

        patch1.addOperation(JsonPatchOperationGene.createAdd(path.copy() as JsonPointerGene, value.copy() as StringGene))
        patch2.addOperation(JsonPatchOperationGene.createAdd(path.copy() as JsonPointerGene, value.copy() as StringGene))

        assertTrue(patch1.containsSameValueAs(patch2))
    }

    @Test
    fun testContainsSameValueAsDifferentSize() {
        val patch1 = JsonPatchGene("p1", null)
        val patch2 = JsonPatchGene("p2", null)

        val path = JsonPointerGene("path", listOf(StringGene("s", "a")))

        patch1.addOperation(JsonPatchOperationGene.createRemove(path.copy() as JsonPointerGene))
        patch2.addOperation(JsonPatchOperationGene.createRemove(path.copy() as JsonPointerGene))
        patch2.addOperation(JsonPatchOperationGene.createRemove(path.copy() as JsonPointerGene))

        assertFalse(patch1.containsSameValueAs(patch2))
    }

    @Test
    fun testRemoveOperation() {
        val patch = JsonPatchGene("patch", null)

        val path1 = JsonPointerGene("p1", listOf(StringGene("s", "a")))
        val path2 = JsonPointerGene("p2", listOf(StringGene("s", "b")))
        val path3 = JsonPointerGene("p3", listOf(StringGene("s", "c")))

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

        patch.addOperation(JsonPatchOperationGene.createAdd(
            JsonPointerGene("p1", listOf(StringGene("s", "name"))),
            StringGene("v", "John")
        ))

        patch.addOperation(JsonPatchOperationGene.createReplace(
            JsonPointerGene("p2", listOf(StringGene("s", "age"))),
            IntegerGene("v", 30)
        ))

        patch.addOperation(JsonPatchOperationGene.createRemove(
            JsonPointerGene("p3", listOf(StringGene("s", "temp")))
        ))

        patch.addOperation(JsonPatchOperationGene.createMove(
            JsonPointerGene("from", listOf(StringGene("s", "old"))),
            JsonPointerGene("to", listOf(StringGene("s", "new")))
        ))

        val json = patch.getValueAsPrintableString()

        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertEquals(4, patch.operations.size)

        assertTrue(json.contains("\"op\":\"add\""))
        assertTrue(json.contains("\"op\":\"replace\""))
        assertTrue(json.contains("\"op\":\"remove\""))
        assertTrue(json.contains("\"op\":\"move\""))
    }

    @Test
    fun testValidJsonStructure() {
        val patch = JsonPatchGene("patch", null)

        patch.addOperation(JsonPatchOperationGene.createAdd(
            JsonPointerGene("p", listOf(StringGene("s", "test"))),
            StringGene("v", "value")
        ))

        val json = patch.getValueAsPrintableString()

        // Should be valid JSON array syntax
        assertTrue(json.matches(Regex("\\[\\{.*\\}\\]")))
    }

    @Test
    fun testMutationWeightAtLeastOne() {
        val patch = JsonPatchGene("patch", null)
        // Even with no operations, weight should be at least 1.0
        assertTrue(patch.mutationWeight() >= 1.0)
    }

    @Test
    fun testEmptyPatchPrintsEmptyArray() {
        val patch = JsonPatchGene("patch", null)
        assertEquals("[]", patch.getValueAsPrintableString())
    }

    @Test
    fun testCopyPreservesOperations() {
        val patch = JsonPatchGene("patch", null)

        val randomness = Randomness()
        randomness.updateSeed(123)
        patch.randomize(randomness, false)

        val originalJson = patch.getValueAsPrintableString()
        val copy = patch.copy() as JsonPatchGene
        val copyJson = copy.getValueAsPrintableString()

        assertEquals(originalJson, copyJson)
        assertEquals(patch.operations.size, copy.operations.size)
    }

    @Test
    fun testUnsafeCopyValueFrom() {
        val patch1 = JsonPatchGene("p1", null)
        val patch2 = JsonPatchGene("p2", null)

        val path = JsonPointerGene("path", listOf(StringGene("s", "x")))
        patch1.addOperation(JsonPatchOperationGene.createAdd(path, StringGene("v", "y")))

        patch2.unsafeCopyValueFrom(patch1)

        assertTrue(patch2.containsSameValueAs(patch1))
    }
}