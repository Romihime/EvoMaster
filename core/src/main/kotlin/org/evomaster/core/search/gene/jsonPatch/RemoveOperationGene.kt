package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness

/**
 * JSON Patch "remove" operation (RFC 6902).
 * Signature: { "op": "remove", "path": "..." }
 */
class RemoveOperationGene(
    pathGene: JsonPointerGene,
    resourceSchema: Gene? = null,
    geneName: String = "removeOp"
) : JsonPatchOperationGene(
    geneName, pathGene, resourceSchema,
    listOf(pathGene)
) {

    override fun operationName() = "remove"

    override fun copyContent(): Gene {
        return RemoveOperationGene(
            pathGene.copy() as JsonPointerGene,
            resourceSchema,
            name
        )
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        pathGene.randomize(randomness, tryToForceNewValue)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val pathValue = pathGene.getValueAsRawString()
        return """{"op":"remove","path":"$pathValue"}"""
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is RemoveOperationGene) return false
        return pathGene.containsSameValueAs(other.pathGene)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is RemoveOperationGene) return false
        pathGene.unsafeCopyValueFrom(other.pathGene)
        return true
    }
}