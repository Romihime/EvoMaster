package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Gene representing a complete JSON Patch document according to RFC 6902.
 *
 * A JSON Patch document is an array of JSON Patch operations.
 * Example:
 * [
 *   { "op": "add", "path": "/foo", "value": "bar" },
 *   { "op": "remove", "path": "/baz" },
 *   { "op": "replace", "path": "/qux", "value": 42 }
 * ]
 *
 * @param resourceSchema Optional schema of the target resource for intelligent path generation
 */
class JsonPatchGene(
    name: String,
    val resourceSchema: Gene? = null
) : CompositeGene(name, mutableListOf()) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JsonPatchGene::class.java)

        // Minimum and maximum number of operations to generate
        private const val MIN_OPERATIONS = 1
        private const val MAX_OPERATIONS = 5
    }

    /**
     * Get all patch operations
     */
    val operations: List<JsonPatchOperationGene>
        get() = children.filterIsInstance<JsonPatchOperationGene>()

    init {
        // Start with at least one operation
        if (children.isEmpty()) {
            addOperation(createRandomOperation(Randomness(), 0))
        }
    }

    /**
     * Create a random JSON Patch operation
     */
    private fun createRandomOperation(randomness: Randomness, index: Int): JsonPatchOperationGene {
        val opType = randomness.choose(JsonPatchOperationGene.VALID_OPS)
        val pathGene = JsonPointerGene("path", mutableListOf(), resourceSchema)
        pathGene.randomize(randomness, false)

        return when (opType) {
            "add" -> {
                val value = StringGene("value", randomness.nextWordString(1, 20))
                JsonPatchOperationGene.createAdd(pathGene, value)
            }
            "remove" -> {
                JsonPatchOperationGene.createRemove(pathGene)
            }
            "replace" -> {
                val value = StringGene("value", randomness.nextWordString(1, 20))
                JsonPatchOperationGene.createReplace(pathGene, value)
            }
            "move" -> {
                val fromGene = JsonPointerGene("from", mutableListOf(), resourceSchema)
                fromGene.randomize(randomness, false)
                JsonPatchOperationGene.createMove(fromGene, pathGene)
            }
            "copy" -> {
                val fromGene = JsonPointerGene("from", mutableListOf(), resourceSchema)
                fromGene.randomize(randomness, false)
                JsonPatchOperationGene.createCopy(fromGene, pathGene)
            }
            "test" -> {
                val value = StringGene("value", randomness.nextWordString(1, 20))
                JsonPatchOperationGene.createTest(pathGene, value)
            }
            else -> {
                // Fallback to add
                val value = StringGene("value", "default")
                JsonPatchOperationGene.createAdd(pathGene, value)
            }
        }
    }

    /**
     * Add an operation to this patch
     */
    fun addOperation(operation: JsonPatchOperationGene) {
        addChild(operation)
    }

    /**
     * Remove an operation at the specified index
     */
    fun removeOperation(index: Int) {
        if (index >= 0 && index < operations.size) {
            killChild(operations[index])
        }
    }

    override fun copyContent(): Gene {
        val copy = JsonPatchGene(name, resourceSchema)
        // Clear the default operation added in init
        copy.getViewOfChildren().toList().forEach { copy.killChild(it) }

        // Copy all operations
        operations.forEach { op ->
            copy.addOperation(op.copy() as JsonPatchOperationGene)
        }

        return copy
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        // A JSON Patch must have at least one operation
        return operations.isNotEmpty()
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        // Clear existing operations
        getViewOfChildren().toList().forEach { killChild(it) }

        // Generate 1-5 operations
        val numOps = randomness.nextInt(MIN_OPERATIONS, MAX_OPERATIONS)

        repeat(numOps) { index ->
            val operation = createRandomOperation(randomness, index)
            addOperation(operation)
        }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        // Allow mutations to add/remove operations
        return randomness.nextBoolean(0.3)
    }

    override fun isMutable(): Boolean {
        return true
    }

    override fun mutationWeight(): Double {
        // Weight based on number of operations
        return operations.size.toDouble()
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        if (operations.isEmpty()) {
            return "[]"
        }

        val operationsJson = operations.joinToString(",") { op ->
            op.getValueAsPrintableString(previousGenes, mode, targetFormat)
        }

        return "[$operationsJson]"
    }

    override fun getValueAsRawString(): String {
        return getValueAsPrintableString()
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is JsonPatchGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (operations.size != other.operations.size) {
            return false
        }

        return operations.zip(other.operations).all { (a, b) ->
            a.containsSameValueAs(b)
        }
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is JsonPatchGene) {
            return false
        }

        // Clear existing operations
        getViewOfChildren().toList().forEach { killChild(it) }

        // Copy operations from other
        other.operations.forEach { otherOp ->
            val copiedOp = otherOp.copy() as JsonPatchOperationGene
            addOperation(copiedOp)
        }

        return true
    }

    override fun unsafeSetFromStringValue(value: String): Boolean {
        // This would require JSON parsing, which is complex
        // For now, we don't support it
        log.warn("unsafeSetFromStringValue not supported for JsonPatchGene")
        return false
    }
}