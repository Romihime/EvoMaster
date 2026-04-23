package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonPatchDocumentGeneTest {

    @Test
    fun testNewPatchIsEmpty() {
        val patch = JsonPatchDocumentGene("patch", null)
        assertTrue(patch.operations.isEmpty())
    }

    @Test
    fun testSingleOperation() {
        val patch = JsonPatchDocumentGene("patch", null)

        val pathGene = JsonPointerGene("path", listOf(StringGene("s0", "a")))
        val valueGene = StringGene("value", "foo")
        val operation = AddOperationGene(pathGene, valueGene)

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
        val patch = JsonPatchDocumentGene("patch", null)

        val path1 = JsonPointerGene("p1", listOf(StringGene("s", "a")))
        val path2 = JsonPointerGene("p2", listOf(StringGene("s", "b")))
        val path3 = JsonPointerGene("p3", listOf(StringGene("s", "c")))

        patch.addOperation(AddOperationGene(path1, StringGene("v", "1")))
        patch.addOperation(RemoveOperationGene(path2))
        patch.addOperation(ReplaceOperationGene(path3, IntegerGene("v", 42)))

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
        val patch = JsonPatchDocumentGene("patch", null)
        val randomness = Randomness()
        randomness.updateSeed(42)

        patch.randomize(randomness, false)

        assertTrue(patch.operations.isNotEmpty())
        assertTrue(patch.operations.size >= JsonPatchDocumentGene.MIN_OPERATIONS)
        assertTrue(patch.operations.size <= JsonPatchDocumentGene.DEFAULT_MAX_OPERATIONS)

        val json = patch.getValueAsPrintableString()
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
    }

    @Test
    fun testCopy() {
        val patch = JsonPatchDocumentGene("patch", null)

        val path1 = JsonPointerGene("p1", listOf(StringGene("s", "a")))
        val path2 = JsonPointerGene("p2", listOf(StringGene("s", "b")))

        patch.addOperation(AddOperationGene(path1, StringGene("v", "1")))
        patch.addOperation(RemoveOperationGene(path2))

        val copy = patch.copy() as JsonPatchDocumentGene

        assertEquals(patch.getValueAsPrintableString(), copy.getValueAsPrintableString())
        assertNotSame(patch, copy)
        assertEquals(2, copy.operations.size)
    }

    @Test
    fun testContainsSameValueAs() {
        val patch1 = JsonPatchDocumentGene("p1", null)
        val patch2 = JsonPatchDocumentGene("p2", null)

        val path = JsonPointerGene("path", listOf(StringGene("s", "a")))
        val value = StringGene("value", "foo")

        patch1.addOperation(AddOperationGene(path.copy() as JsonPointerGene, value.copy() as StringGene))
        patch2.addOperation(AddOperationGene(path.copy() as JsonPointerGene, value.copy() as StringGene))

        assertTrue(patch1.containsSameValueAs(patch2))
    }

    @Test
    fun testContainsSameValueAsDifferentSize() {
        val patch1 = JsonPatchDocumentGene("p1", null)
        val patch2 = JsonPatchDocumentGene("p2", null)

        val path = JsonPointerGene("path", listOf(StringGene("s", "a")))

        patch1.addOperation(RemoveOperationGene(path.copy() as JsonPointerGene))
        patch2.addOperation(RemoveOperationGene(path.copy() as JsonPointerGene))
        patch2.addOperation(RemoveOperationGene(path.copy() as JsonPointerGene))

        assertFalse(patch1.containsSameValueAs(patch2))
    }

    @Test
    fun testRemoveOperation() {
        val patch = JsonPatchDocumentGene("patch", null)

        val path1 = JsonPointerGene("p1", listOf(StringGene("s", "a")))
        val path2 = JsonPointerGene("p2", listOf(StringGene("s", "b")))
        val path3 = JsonPointerGene("p3", listOf(StringGene("s", "c")))

        patch.addOperation(RemoveOperationGene(path1))
        patch.addOperation(RemoveOperationGene(path2))
        patch.addOperation(RemoveOperationGene(path3))

        assertEquals(3, patch.operations.size)

        patch.removeOperation(1)

        assertEquals(2, patch.operations.size)
    }

    @Test
    fun testComplexPatch() {
        val patch = JsonPatchDocumentGene("patch", null)

        patch.addOperation(AddOperationGene(
            JsonPointerGene("p1", listOf(StringGene("s", "name"))),
            StringGene("v", "John")
        ))

        patch.addOperation(ReplaceOperationGene(
            JsonPointerGene("p2", listOf(StringGene("s", "age"))),
            IntegerGene("v", 30)
        ))

        patch.addOperation(RemoveOperationGene(
            JsonPointerGene("p3", listOf(StringGene("s", "temp")))
        ))

        patch.addOperation(MoveOperationGene(
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
        val patch = JsonPatchDocumentGene("patch", null)

        patch.addOperation(AddOperationGene(
            JsonPointerGene("p", listOf(StringGene("s", "test"))),
            StringGene("v", "value")
        ))

        val json = patch.getValueAsPrintableString()
        assertTrue(json.matches(Regex("\\[\\{.*\\}\\]")))
    }

    @Test
    fun testMutationWeightAtLeastOne() {
        val patch = JsonPatchDocumentGene("patch", null)
        assertTrue(patch.mutationWeight() >= 1.0)
    }

    @Test
    fun testEmptyPatchPrintsEmptyArray() {
        val patch = JsonPatchDocumentGene("patch", null)
        assertEquals("[]", patch.getValueAsPrintableString())
    }

    @Test
    fun testCopyPreservesOperations() {
        val patch = JsonPatchDocumentGene("patch", null)

        val randomness = Randomness()
        randomness.updateSeed(123)
        patch.randomize(randomness, false)

        val originalJson = patch.getValueAsPrintableString()
        val copy = patch.copy() as JsonPatchDocumentGene
        val copyJson = copy.getValueAsPrintableString()

        assertEquals(originalJson, copyJson)
        assertEquals(patch.operations.size, copy.operations.size)
    }

    @Test
    fun testUnsafeCopyValueFrom() {
        val patch1 = JsonPatchDocumentGene("p1", null)
        val patch2 = JsonPatchDocumentGene("p2", null)

        val path = JsonPointerGene("path", listOf(StringGene("s", "x")))
        patch1.addOperation(AddOperationGene(path, StringGene("v", "y")))

        patch2.unsafeCopyValueFrom(patch1)

        assertTrue(patch2.containsSameValueAs(patch1))
    }

    // --- Schema-aware tests ---

    private fun createSampleSchema(): ObjectGene {
        return ObjectGene(
            "resource",
            listOf(
                StringGene("name"),
                IntegerGene("age"),
                BooleanGene("active")
            ),
            refType = null,
            isFixed = true,
            template = null,
            additionalFields = null
        )
    }

    @Test
    fun testRandomizeWithSchemaUsesFieldNames() {
        val schema = createSampleSchema()
        val patch = JsonPatchDocumentGene("patch", schema)
        val randomness = Randomness()
        randomness.updateSeed(42)

        val allPathSegments = mutableSetOf<String>()
        repeat(20) {
            patch.randomize(randomness, false)
            for (op in patch.operations) {
                for (seg in op.pathGene.segments) {
                    allPathSegments.add(seg.getValueAsRawString())
                }
            }
        }

        val schemaFields = setOf("name", "age", "active")
        val schemaFieldsUsed = allPathSegments.intersect(schemaFields)
        assertTrue(schemaFieldsUsed.isNotEmpty(),
            "Expected schema field names in generated paths, got: $allPathSegments")
    }

    @Test
    fun testRandomizeCanProduceUpToMaxOperations() {
        val patch = JsonPatchDocumentGene("patch", null)
        val randomness = Randomness()

        var maxFound = 0
        for (seed in 0L..100L) {
            randomness.updateSeed(seed)
            patch.randomize(randomness, false)
            if (patch.operations.size > maxFound) {
                maxFound = patch.operations.size
            }
        }

        assertTrue(maxFound > 1,
            "Expected randomize to produce varying operation counts up to ${JsonPatchDocumentGene.DEFAULT_MAX_OPERATIONS}, " +
                    "but max found was $maxFound")
        assertTrue(maxFound <= JsonPatchDocumentGene.DEFAULT_MAX_OPERATIONS,
            "Expected at most ${JsonPatchDocumentGene.DEFAULT_MAX_OPERATIONS} operations, but found $maxFound")
    }

    @Test
    fun testRandomizeWithSchemaProducesNonEmptyOperations() {
        val schema = createSampleSchema()
        val patch = JsonPatchDocumentGene("patch", schema)
        val randomness = Randomness()
        randomness.updateSeed(99)

        patch.randomize(randomness, false)

        assertTrue(patch.operations.isNotEmpty())
        val json = patch.getValueAsPrintableString()
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
    }

    @Test
    fun testRandomizeProducesAllOperationTypes() {
        val patch = JsonPatchDocumentGene("patch", null)
        val randomness = Randomness()

        val operationTypes = mutableSetOf<String>()
        for (seed in 0L..200L) {
            randomness.updateSeed(seed)
            patch.randomize(randomness, false)
            for (op in patch.operations) {
                operationTypes.add(op.operationName())
            }
        }

        assertTrue(operationTypes.contains("add"), "Expected add operations")
        assertTrue(operationTypes.contains("remove"), "Expected remove operations")
        assertTrue(operationTypes.contains("replace"), "Expected replace operations")
        assertTrue(operationTypes.contains("move"), "Expected move operations")
        assertTrue(operationTypes.contains("copy"), "Expected copy operations")
        assertTrue(operationTypes.contains("test"), "Expected test operations")
    }
}