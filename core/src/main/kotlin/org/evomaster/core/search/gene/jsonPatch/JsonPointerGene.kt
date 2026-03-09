package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.OptionalGene
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

        /**
         * Extract field names from a resource schema Gene recursively.
         * Unwraps OptionalGene wrappers and traverses ObjectGene fields
         * and ArrayGene templates to collect all reachable field names.
         */
        fun extractFieldNames(schema: Gene?): List<String> {
            if (schema == null) return emptyList()

            val names = mutableSetOf<String>()
            collectFieldNames(schema, names, depth = 0)
            return names.toList()
        }

        private fun collectFieldNames(gene: Gene, names: MutableSet<String>, depth: Int) {
            if (depth > 5) return

            val unwrapped = unwrapGene(gene)

            when (unwrapped) {
                is ObjectGene -> {
                    for (field in unwrapped.fixedFields) {
                        names.add(field.name)
                        collectFieldNames(field, names, depth + 1)
                    }
                }
                is ArrayGene<*> -> {
                    collectFieldNames(unwrapped.template, names, depth + 1)
                }
            }
        }

        private fun unwrapGene(gene: Gene): Gene {
            var current = gene
            while (current is OptionalGene) {
                current = current.gene
            }
            return current
        }

        /**
         * Resolve a JSON Pointer path against a resource schema to find
         * the Gene type at that location. Returns null if the path cannot
         * be resolved.
         */
        fun resolveGeneAtPath(schema: Gene?, segments: List<StringGene>): Gene? {
            if (schema == null || segments.isEmpty()) return schema

            var current = unwrapGene(schema)

            for (seg in segments) {
                val segValue = seg.getValueAsRawString()

                when (current) {
                    is ObjectGene -> {
                        val field = current.fixedFields.find { it.name == segValue }
                            ?: return null
                        current = unwrapGene(field)
                    }
                    is ArrayGene<*> -> {
                        current = unwrapGene(current.template)
                        if (current is ObjectGene) {
                            val field = current.fixedFields.find { it.name == segValue }
                            if (field != null) {
                                current = unwrapGene(field)
                            }
                        }
                    }
                    else -> return null
                }
            }

            return current
        }
    }

    /**
     * Get all current segments (read-only view)
     */
    val segments: List<StringGene>
        get() = children.filterIsInstance<StringGene>()

    override fun copyContent(): Gene {
        val copiedSegments = segments.map { it.copy() as StringGene }
        // resourceSchema is shared by reference intentionally: it is a read-only template
        // used only to extract field names, never mutated
        return JsonPointerGene(name, copiedSegments, resourceSchema?.copy())
    }

    override fun checkForLocallyValidIgnoringChildren(): Boolean {
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        killAllChildren()

        val fieldNames = extractFieldNames(resourceSchema)
        val numSegments = randomness.nextInt(1, MAX_SEGMENTS)
        for (i in 0 until numSegments) {
            val segValue = if (fieldNames.isNotEmpty() && randomness.nextBoolean(0.8)) {
                randomness.choose(fieldNames)
            } else {
                randomness.nextWordString(1, 10)
            }
            addChild(StringGene("seg$i", segValue))
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
        val fieldNames = extractFieldNames(resourceSchema)
        if (segments.size < MAX_SEGMENTS && (segments.isEmpty() || randomness.nextBoolean())) {
            val segValue = if (fieldNames.isNotEmpty() && randomness.nextBoolean(0.8)) {
                randomness.choose(fieldNames)
            } else {
                randomness.nextWordString(1, 10)
            }
            val newSeg = StringGene("seg${segments.size}", segValue)
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