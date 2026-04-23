package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Abstract base class representing a single JSON Patch operation according to RFC 6902.
 *
 * JSON Patch defines 6 operations, each with a specific signature:
 * - add: path + value
 * - remove: path only
 * - replace: path + value
 * - move: from + path
 * - copy: from + path
 * - test: path + value
 *
 * Each concrete subclass carries only the fields required by its operation,
 * making invalid states unrepresentable by construction.
 *
 * @param pathGene The target JSON Pointer path for this operation
 * @param resourceSchema Optional schema of the target resource for intelligent path/value generation
 */
abstract class JsonPatchOperationGene(
    name: String,
    val pathGene: JsonPointerGene,
    val resourceSchema: Gene? = null,
    children: List<Gene>
) : CompositeFixedGene(name, children) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JsonPatchOperationGene::class.java)

        val VALID_OPS = listOf("add", "remove", "replace", "move", "copy", "test")

        /**
         * Create a value gene matching the type at the given path in the schema.
         * Falls back to a random type if schema is unavailable or path cannot be resolved.
         */
        fun createValueForPath(pathGene: JsonPointerGene, resourceSchema: Gene?, randomness: Randomness): Gene {
            val resolvedGene = JsonPointerGene.resolveGeneAtPath(resourceSchema, pathGene.segments)
            if (resolvedGene != null) {
                return createValueMatchingType(resolvedGene, randomness)
            }
            return createRandomValueGene(randomness)
        }

        fun createValueMatchingType(schemaGene: Gene, randomness: Randomness): Gene {
            return when (schemaGene) {
                is StringGene -> StringGene("value", randomness.nextWordString(1, 20))
                is IntegerGene -> IntegerGene("value", randomness.nextInt(0, 1000))
                is LongGene -> LongGene("value", randomness.nextInt(0, 1000).toLong())
                is DoubleGene -> DoubleGene("value", randomness.nextDouble())
                is FloatGene -> FloatGene("value", randomness.nextDouble().toFloat())
                is BooleanGene -> BooleanGene("value", randomness.nextBoolean())
                is ObjectGene -> {
                    val copy = schemaGene.copy() as ObjectGene
                    copy.randomize(randomness, false)
                    copy
                }
                is ArrayGene<*> -> {
                    val copy = schemaGene.copy() as ArrayGene<*>
                    copy.randomize(randomness, false)
                    copy
                }
                else -> createRandomValueGene(randomness)
            }
        }

        fun createRandomValueGene(randomness: Randomness): Gene {
            return when (randomness.nextInt(0, 3)) {
                0 -> StringGene("value", randomness.nextWordString(1, 20))
                1 -> IntegerGene("value", randomness.nextInt(0, 1000))
                2 -> BooleanGene("value", randomness.nextBoolean())
                else -> DoubleGene("value", randomness.nextDouble())
            }
        }
    }

    /** Returns the RFC 6902 operation name (e.g. "add", "remove", "replace") */
    abstract fun operationName(): String

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

    override fun isMutable(): Boolean {
        return true
    }

    override fun getValueAsRawString(): String {
        return getValueAsPrintableString()
    }

    override fun unsafeSetFromStringValue(value: String): Boolean {
        log.warn("unsafeSetFromStringValue not supported for ${this::class.simpleName}")
        return false
    }
}