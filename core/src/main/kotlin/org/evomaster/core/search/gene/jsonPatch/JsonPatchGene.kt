package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
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

        const val MIN_OPERATIONS = 1
        const val DEFAULT_MAX_OPERATIONS = 10
    }

    val operations: List<JsonPatchOperationGene>
        get() = children.filterIsInstance<JsonPatchOperationGene>()

    /**
     * Create a random JSON Patch operation using the given randomness.
     * If a resourceSchema is available, value types are chosen to match field types.
     */
    private fun createRandomOperation(randomness: Randomness): JsonPatchOperationGene {
        val opType = randomness.choose(JsonPatchOperationGene.VALID_OPS)
        val pathGene = JsonPointerGene("path", emptyList(), resourceSchema)
        pathGene.randomize(randomness, false)

        return when (opType) {
            "add" -> {
                val value = createValueForPath(pathGene, randomness)
                JsonPatchOperationGene.createAdd(pathGene, value, resourceSchema)
            }
            "remove" -> {
                JsonPatchOperationGene.createRemove(pathGene, resourceSchema)
            }
            "replace" -> {
                val value = createValueForPath(pathGene, randomness)
                JsonPatchOperationGene.createReplace(pathGene, value, resourceSchema)
            }
            "move" -> {
                val fromGene = JsonPointerGene("from", emptyList(), resourceSchema)
                fromGene.randomize(randomness, false)
                JsonPatchOperationGene.createMove(fromGene, pathGene, resourceSchema)
            }
            "copy" -> {
                val fromGene = JsonPointerGene("from", emptyList(), resourceSchema)
                fromGene.randomize(randomness, false)
                JsonPatchOperationGene.createCopy(fromGene, pathGene, resourceSchema)
            }
            "test" -> {
                val value = createValueForPath(pathGene, randomness)
                JsonPatchOperationGene.createTest(pathGene, value, resourceSchema)
            }
            else -> {
                val value = StringGene("value", "default")
                JsonPatchOperationGene.createAdd(pathGene, value, resourceSchema)
            }
        }
    }

    /**
     * Create a value gene that matches the type of the field pointed to by the path.
     * Delegates to the shared helper in JsonPatchOperationGene.
     */
    private fun createValueForPath(pathGene: JsonPointerGene, randomness: Randomness): Gene {
        return JsonPatchOperationGene.createValueForPath(pathGene, resourceSchema, randomness)
    }

    fun addOperation(operation: JsonPatchOperationGene) {
        addChild(operation)
    }

    /**
     * Add an operation, initializing it if this gene is already initialized.
     */
    private fun addInitializedOperation(operation: JsonPatchOperationGene, randomness: Randomness) {
        if (this.initialized) {
            operation.doInitialize(randomness)
        }
        addChild(operation)
    }

    fun removeOperation(index: Int) {
        if (index >= 0 && index < operations.size) {
            killChild(operations[index])
        }
    }

    override fun copyContent(): Gene {
        val copy = JsonPatchGene(name, resourceSchema)
        operations.forEach { op ->
            copy.addOperation(op.copy() as JsonPatchOperationGene)
        }
        return copy
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return operations.isNotEmpty()
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        killAllChildren()

        val numOps = randomness.nextInt(MIN_OPERATIONS, DEFAULT_MAX_OPERATIONS)
        repeat(numOps) {
            val operation = createRandomOperation(randomness)
            addInitializedOperation(operation, randomness)
        }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        if (operations.isEmpty()) return true
        return randomness.nextBoolean(0.3)
    }

    override fun shallowMutate(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        mwc: MutationWeightControl,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        if (operations.isEmpty() || operations.size <= MIN_OPERATIONS) {
            val op = createRandomOperation(randomness)
            addInitializedOperation(op, randomness)
        } else if (operations.size >= DEFAULT_MAX_OPERATIONS) {
            removeOperation(randomness.nextInt(operations.size))
        } else if (randomness.nextBoolean()) {
            val op = createRandomOperation(randomness)
            addInitializedOperation(op, randomness)
        } else {
            removeOperation(randomness.nextInt(operations.size))
        }
        return true
    }

    override fun isMutable(): Boolean {
        return true
    }

    override fun mutationWeight(): Double {
        return 1.0 + operations.sumOf { it.mutationWeight() }
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

        killAllChildren()

        other.operations.forEach { otherOp ->
            val copiedOp = otherOp.copy() as JsonPatchOperationGene
            if (this.initialized && !copiedOp.initialized) {
                copiedOp.markAllAsInitialized()
            }
            addOperation(copiedOp)
        }

        return true
    }

    override fun unsafeSetFromStringValue(value: String): Boolean {
        log.warn("unsafeSetFromStringValue not supported for JsonPatchGene")
        return false
    }
}