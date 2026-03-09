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
 * Gene representing a JSON Pointer (RFC 6901).
 *
 * JSON Pointers are used to reference specific locations within a JSON document.
 * Examples:
 *   "" -> root document
 *   "/foo" -> foo property
 *   "/foo/0" -> first element in foo array
 *   "/foo/bar" -> nested property
 *   "/foo/-" -> append to foo array
 *
 * Special characters need escaping:
 *   "~" becomes "~0"
 *   "/" becomes "~1"
 *
 * @param resourceSchema Optional schema of the target resource for intelligent path generation
 */
class JsonPointerGene(
    name: String,
    initialSegments: List<StringGene> = emptyList(),
    val resourceSchema: Gene? = null
) : CompositeGene(name, initialSegments.toMutableList()) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JsonPointerGene::class.java)

        const val ARRAY_APPEND = "-"

        private const val MAX_SEGMENTS = 4

        fun fromPath(name: String, path: String, resourceSchema: Gene? = null): JsonPointerGene {
            if (path.isEmpty() || path == "/") {
                return JsonPointerGene(name, emptyList(), resourceSchema)
            }

            val segs = path.removePrefix("/")
                .split("/")
                .mapIndexed { index, seg ->
                    StringGene("seg$index", unescapeSegment(seg))
                }

            return JsonPointerGene(name, segs, resourceSchema)
        }

        private fun unescapeSegment(segment: String): String {
            return segment
                .replace("~1", "/")
                .replace("~0", "~")
        }

        fun escapeSegment(segment: String): String {
            return segment
                .replace("~", "~0")
                .replace("/", "~1")
        }
    }

    /**
     * Get all current segments (read-only view)
     */
    val segments: List<StringGene>
        get() = children.filterIsInstance<StringGene>()

    override fun copyContent(): Gene {
        val copiedSegments = segments.map { it.copy() as StringGene }
        return JsonPointerGene(name, copiedSegments, resourceSchema)
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        killAllChildren()

        val numSegments = randomness.nextInt(0, MAX_SEGMENTS)
        for (i in 0 until numSegments) {
            val segment = StringGene("seg$i", randomness.nextWordString(1, 10))
            addChild(segment)
        }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        if (segments.isEmpty()) return true
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
        if (segments.size < MAX_SEGMENTS && (segments.isEmpty() || randomness.nextBoolean())) {
            val newSeg = StringGene("seg${segments.size}", randomness.nextWordString(1, 10))
            addInitializedChild(newSeg, randomness)
        } else if (segments.isNotEmpty()) {
            killChildByIndex(randomness.nextInt(segments.size))
        } else {
            return false
        }
        return true
    }

    override fun isMutable(): Boolean {
        return true
    }

    override fun mutationWeight(): Double {
        return 1.0 + segments.sumOf { it.mutationWeight() }
    }

    /**
     * Add a child gene, initializing it if this gene is already initialized.
     */
    private fun addInitializedChild(gene: Gene, randomness: Randomness? = null) {
        if (this.initialized) {
            if (randomness != null) {
                gene.doInitialize(randomness)
            } else {
                gene.markAllAsInitialized()
            }
        }
        addChild(gene)
    }

    fun addSegment(segment: String) {
        val seg = StringGene("seg${segments.size}", segment)
        addInitializedChild(seg)
    }

    fun removeLastSegment() {
        if (segments.isNotEmpty()) {
            killChildByIndex(segments.size - 1)
        }
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        if (segments.isEmpty()) {
            return "/"
        }

        return segments.joinToString("/", prefix = "/") { segment ->
            val rawValue = segment.getValueAsRawString()
            escapeSegment(rawValue)
        }
    }

    override fun getValueAsRawString(): String {
        return getValueAsPrintableString()
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is JsonPointerGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (segments.size != other.segments.size) {
            return false
        }

        return segments.zip(other.segments).all { (a, b) ->
            a.containsSameValueAs(b)
        }
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is JsonPointerGene) {
            return false
        }

        killAllChildren()
        other.segments.forEach { otherSeg ->
            val newSeg = StringGene(otherSeg.name, otherSeg.value)
            addInitializedChild(newSeg)
        }

        return true
    }

    override fun unsafeSetFromStringValue(value: String): Boolean {
        try {
            killAllChildren()

            if (value.isEmpty() || value == "/") {
                return true
            }

            val path = value.removePrefix("/")
            path.split("/").forEachIndexed { index, seg ->
                addInitializedChild(StringGene("seg$index", unescapeSegment(seg)))
            }

            return true
        } catch (e: Exception) {
            log.warn("Failed to set JsonPointerGene from string: $value", e)
            return false
        }
    }
}