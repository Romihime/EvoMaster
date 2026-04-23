package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness

/**
 * JSON Patch "add" operation (RFC 6902).
 * Signature: { "op": "add", "path": "...", "value": ... }
 */
class AddOperationGene(
    pathGene: JsonPointerGene,
    val valueGene: Gene,
    resourceSchema: Gene? = null,
    geneName: String = "addOp"
) : JsonPatchOperationGene(
    geneName, pathGene, resourceSchema,
    listOf(pathGene, valueGene)
) {

    override fun operationName() = "add"

    override fun copyContent(): Gene {
        return AddOperationGene(
            pathGene.copy() as JsonPointerGene,
            valueGene.copy(),
            resourceSchema,
            name
        )
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        pathGene.randomize(randomness, tryToForceNewValue)
        valueGene.randomize(randomness, false)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val pathValue = pathGene.getValueAsRawString()
        val valueStr = valueGene.getValueAsPrintableString(previousGenes, mode, targetFormat)
        return """{"op":"add","path":"$pathValue","value":$valueStr}"""
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is AddOperationGene) return false
        return pathGene.containsSameValueAs(other.pathGene) &&
                valueGene.containsSameValueAs(other.valueGene)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is AddOperationGene) return false
        pathGene.unsafeCopyValueFrom(other.pathGene)
        valueGene.unsafeCopyValueFrom(other.valueGene)
        return true
    }
}