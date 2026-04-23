package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness

/**
 * JSON Patch "copy" operation (RFC 6902).
 * Signature: { "op": "copy", "path": "...", "from": "..." }
 */
class CopyOperationGene(
    val fromGene: JsonPointerGene,
    pathGene: JsonPointerGene,
    resourceSchema: Gene? = null,
    geneName: String = "copyOp"
) : JsonPatchOperationGene(
    geneName, pathGene, resourceSchema,
    listOf(pathGene, fromGene)
) {

    override fun operationName() = "copy"

    override fun copyContent(): Gene {
        return CopyOperationGene(
            fromGene.copy() as JsonPointerGene,
            pathGene.copy() as JsonPointerGene,
            resourceSchema,
            name
        )
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        pathGene.randomize(randomness, tryToForceNewValue)
        fromGene.randomize(randomness, false)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val pathValue = pathGene.getValueAsRawString()
        val fromValue = fromGene.getValueAsRawString()
        return """{"op":"copy","path":"$pathValue","from":"$fromValue"}"""
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is CopyOperationGene) return false
        return pathGene.containsSameValueAs(other.pathGene) &&
                fromGene.containsSameValueAs(other.fromGene)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is CopyOperationGene) return false
        pathGene.unsafeCopyValueFrom(other.pathGene)
        fromGene.unsafeCopyValueFrom(other.fromGene)
        return true
    }
}