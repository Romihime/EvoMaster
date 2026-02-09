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
 * @param segments List of path segments (without leading slashes)
 * @param resourceSchema Optional schema of the target resource for intelligent path generation
 */
class JsonPointerGene(
    name: String,
    val segments: MutableList<StringGene> = mutableListOf(),
    val resourceSchema: Gene? = null
) : CompositeGene(name, segments) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JsonPointerGene::class.java)

        /**
         * Special segment for array append operation
         */
        const val ARRAY_APPEND = "-"

        /**
         * Create a JsonPointerGene from a path string
         */
        fun fromPath(name: String, path: String, resourceSchema: Gene? = null): JsonPointerGene {
            if (path.isEmpty() || path == "/") {
                return JsonPointerGene(name, mutableListOf(), resourceSchema)
            }

            val segments = path.removePrefix("/")
                .split("/")
                .mapIndexed { index, seg ->
                    StringGene("seg$index", unescapeSegment(seg))
                }
                .toMutableList()

            return JsonPointerGene(name, segments, resourceSchema)
        }

        /**
         * Unescape JSON Pointer special characters
         */
        private fun unescapeSegment(segment: String): String {
            return segment
                .replace("~1", "/")
                .replace("~0", "~")
        }

        /**
         * Escape JSON Pointer special characters
         */
        private fun escapeSegment(segment: String): String {
            return segment
                .replace("~", "~0")
                .replace("/", "~1")
        }
    }

    override fun copyContent(): Gene {
        val copiedSegments = segments.map { it.copy() as StringGene }.toMutableList()
        return JsonPointerGene(name, copiedSegments, resourceSchema)
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        // JSON Pointers are always structurally valid
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        // Clear existing segments
        segments.clear()

        // Generate 0-4 segments randomly
        val numSegments = randomness.nextInt(0, 4)

        for (i in 0 until numSegments) {
            val segment = StringGene("seg$i", randomness.nextWordString(1, 10))
            segment.randomize(randomness, false)
            segments.add(segment)
        }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        // Allow shallow mutations to modify the structure
        return randomness.nextBoolean(0.3)
    }

    override fun isMutable(): Boolean {
        return true
    }

    /**
     * Add a segment to the path
     */
    fun addSegment(segment: String) {
        segments.add(StringGene("seg${segments.size}", segment))
    }

    /**
     * Remove the last segment from the path
     */
    fun removeLastSegment() {
        if (segments.isNotEmpty()) {
            segments.removeAt(segments.size - 1)
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

        segments.clear()
        other.segments.forEach { otherSeg ->
            val newSeg = StringGene(otherSeg.name, otherSeg.value)
            segments.add(newSeg)
        }

        return true
    }

    override fun unsafeSetFromStringValue(value: String): Boolean {
        try {
            segments.clear()

            if (value.isEmpty() || value == "/") {
                return true
            }

            val path = value.removePrefix("/")
            path.split("/").forEachIndexed { index, seg ->
                segments.add(StringGene("seg$index", unescapeSegment(seg)))
            }

            return true
        } catch (e: Exception) {
            log.warn("Failed to set JsonPointerGene from string: $value", e)
            return false
        }
    }
}