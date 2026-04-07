package org.evomaster.core.search.gene.jsonPatch

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness

/**
 * JSON Patch "test" operation (RFC 6902).
 * Signature: { "op": "test", "path": "...", "value": ... }
 */
class TestOperationGene(
    pathGene: JsonPointerGene,
    val valueGene: Gene,
    resourceSchema: Gene? = null,
    geneName: String = "testOp"
) : JsonPatchOperationGene(
    geneName, pathGene, resourceSchema,
    listOf(pathGene, valueGene)
) {

    override fun operationName() = "test"

    override fun copyContent(): Gene {
        return TestOperationGene(
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
        return """{"op":"test","path":"$pathValue","value":$valueStr}"""
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is TestOperationGene) return false
        return pathGene.containsSameValueAs(other.pathGene) &&
                valueGene.containsSameValueAs(other.valueGene)
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {
        if (other !is TestOperationGene) return false
        pathGene.unsafeCopyValueFrom(other.pathGene)
        valueGene.unsafeCopyValueFrom(other.valueGene)
        return true
    }
}