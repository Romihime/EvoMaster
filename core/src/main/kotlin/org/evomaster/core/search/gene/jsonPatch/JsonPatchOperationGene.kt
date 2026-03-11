package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Gene representing a single JSON Patch operation according to RFC 6902.
 *
 * JSON Patch defines 6 operations:
 * - add: Add a value at the specified path
 * - remove: Remove the value at the specified path
 * - replace: Replace the value at the specified path
 * - move: Move a value from one path to another
 * - copy: Copy a value from one path to another
 * - test: Test that a value at the specified path equals a specified value
 *
 * Depending on the operation, different fields are required:
 * - "add", "replace", "test": require "path" and "value"
 * - "remove": requires only "path"
 * - "move", "copy": require "from" and "path"
 *
 * Example:
 *   { "op": "add", "path": "/foo", "value": "bar" }
 */
class JsonPatchOperationGene(
    name: String,
    opGene: EnumGene<String>,
    pathGene: JsonPointerGene,
    fromGene: OptionalGene,
    valueGene: OptionalGene
) : CompositeFixedGene(name, listOf(opGene, pathGene, fromGene, valueGene)) {

    val opGene: EnumGene<String>
        get() = children[0] as EnumGene<String>
    val pathGene: JsonPointerGene
        get() = children[1] as JsonPointerGene
    val fromGene: OptionalGene
        get() = children[2] as OptionalGene
    val valueGene: OptionalGene
        get() = children[3] as OptionalGene

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JsonPatchOperationGene::class.java)

        val VALID_OPS = listOf("add", "remove", "replace", "move", "copy", "test")

        // EnumGene sorts values alphabetically. Compute sorted indices dynamically
        // to avoid breakage if EnumGene internals change.
        private val SORTED_OPS = VALID_OPS.sorted()

        private fun opIndex(opName: String): Int {
            val idx = SORTED_OPS.indexOf(opName)
            if (idx < 0) throw IllegalArgumentException("Unknown op: $opName")
            return idx
        }

        fun createAdd(pathGene: JsonPointerGene, valueGene: Gene): JsonPatchOperationGene {
            val op = EnumGene("op", VALID_OPS, opIndex("add"))
            val from = OptionalGene("from", JsonPointerGene("from"), isActive = false)
            val value = OptionalGene("value", valueGene, isActive = true)

            return JsonPatchOperationGene("addOp", op, pathGene, from, value)
        }

        fun createRemove(pathGene: JsonPointerGene): JsonPatchOperationGene {
            val op = EnumGene("op", VALID_OPS, opIndex("remove"))
            val from = OptionalGene("from", JsonPointerGene("from"), isActive = false)
            val value = OptionalGene("value", StringGene("value"), isActive = false)

            return JsonPatchOperationGene("removeOp", op, pathGene, from, value)
        }

        fun createReplace(pathGene: JsonPointerGene, valueGene: Gene): JsonPatchOperationGene {
            val op = EnumGene("op", VALID_OPS, opIndex("replace"))
            val from = OptionalGene("from", JsonPointerGene("from"), isActive = false)
            val value = OptionalGene("value", valueGene, isActive = true)

            return JsonPatchOperationGene("replaceOp", op, pathGene, from, value)
        }

        fun createMove(fromGene: JsonPointerGene, pathGene: JsonPointerGene): JsonPatchOperationGene {
            val op = EnumGene("op", VALID_OPS, opIndex("move"))
            val from = OptionalGene("from", fromGene, isActive = true)
            val value = OptionalGene("value", StringGene("value"), isActive = false)

            return JsonPatchOperationGene("moveOp", op, pathGene, from, value)
        }

        fun createCopy(fromGene: JsonPointerGene, pathGene: JsonPointerGene): JsonPatchOperationGene {
            val op = EnumGene("op", VALID_OPS, opIndex("copy"))
            val from = OptionalGene("from", fromGene, isActive = true)
            val value = OptionalGene("value", StringGene("value"), isActive = false)

            return JsonPatchOperationGene("copyOp", op, pathGene, from, value)
        }

        fun createTest(pathGene: JsonPointerGene, valueGene: Gene): JsonPatchOperationGene {
            val op = EnumGene("op", VALID_OPS, opIndex("test"))
            val from = OptionalGene("from", JsonPointerGene("from"), isActive = false)
            val value = OptionalGene("value", valueGene, isActive = true)

            return JsonPatchOperationGene("testOp", op, pathGene, from, value)
        }
    }

    init {
        ensureFieldConsistency()
    }

    /**
     * Ensure the correct fields are active based on the current operation
     */
    private fun ensureFieldConsistency() {
        val op = opGene.values[opGene.index]

        when (op) {
            "add", "replace", "test" -> {
                valueGene.isActive = true
                fromGene.isActive = false
            }
            "remove" -> {
                valueGene.isActive = false
                fromGene.isActive = false
            }
            "move", "copy" -> {
                valueGene.isActive = false
                fromGene.isActive = true
            }
        }
    }

    override fun copyContent(): Gene {
        return JsonPatchOperationGene(
            name,
            opGene.copy() as EnumGene<String>,
            pathGene.copy() as JsonPointerGene,
            fromGene.copy() as OptionalGene,
            valueGene.copy() as OptionalGene
        )
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        opGene.randomize(randomness, tryToForceNewValue)
        pathGene.randomize(randomness, false)

        if (fromGene.gene is JsonPointerGene) {
            (fromGene.gene as JsonPointerGene).randomize(randomness, false)
        }

        valueGene.gene.randomize(randomness, false)

        ensureFieldConsistency()
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        // CompositeFixedGene does not have mutable structure,
        // shallow mutation is not applicable
        return false
    }

    override fun isMutable(): Boolean {
        return true
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val parts = mutableListOf<String>()

        val opValue = opGene.values[opGene.index]
        parts.add("\"op\":\"$opValue\"")

        // path is always a raw string (JSON Pointer), we control its quoting here
        val pathValue = pathGene.getValueAsRawString()
        parts.add("\"path\":\"$pathValue\"")

        if (fromGene.isActive && fromGene.gene is JsonPointerGene) {
            val fromValue = (fromGene.gene as JsonPointerGene).getValueAsRawString()
            parts.add("\"from\":\"$fromValue\"")
        }

        if (valueGene.isActive) {
            val valueStr = valueGene.gene.getValueAsPrintableString(previousGenes, mode, targetFormat)
            parts.add("\"value\":$valueStr")
        }

        return "{${parts.joinToString(",")}}"
    }

    override fun getValueAsRawString(): String {
        return getValueAsPrintableString()
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is JsonPatchOperationGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return opGene.containsSameValueAs(other.opGene) &&
                pathGene.containsSameValueAs(other.pathGene) &&
                fromGene.containsSameValueAs(other.fromGene) &&
                valueGene.containsSameValueAs(other.valueGene)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is JsonPatchOperationGene) {
            return false
        }

        opGene.unsafeCopyValueFrom(other.opGene)
        pathGene.unsafeCopyValueFrom(other.pathGene)
        fromGene.unsafeCopyValueFrom(other.fromGene)
        valueGene.unsafeCopyValueFrom(other.valueGene)

        ensureFieldConsistency()

        return true
    }

    override fun unsafeSetFromStringValue(value: String): Boolean {
        log.warn("unsafeSetFromStringValue not supported for JsonPatchOperationGene")
        return false
    }
}