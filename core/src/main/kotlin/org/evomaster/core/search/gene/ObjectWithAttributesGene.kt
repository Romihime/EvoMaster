package org.evomaster.core.search.gene

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.evomaster.core.utils.CollectionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ObjectWithAttributesGene(
    name: String,
    fixedFields: List<Gene>,
    refType: String? = null,
    isFixed: Boolean,
    template: PairGene<StringGene, Gene>? = null,
    additionalFields: MutableList<PairGene<StringGene, Gene>>? = null,
    val attributeNames: Set<String> = emptySet()
) : ObjectGene(name, fixedFields, refType, isFixed, template, additionalFields) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ObjectWithAttributesGene::class.java)
    }

    init {

        val includedFields = fixedFields
            .filter { it !is CycleObjectGene }
            .filter { it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene) }
            .filter { it.isPrintable() }

        val attributeFields = includedFields.filter { attributeNames.contains(it.name) }

        //"#text" CANNOT be an attribute
        if (attributeFields.any { it.name == "#text" }) {
            throw IllegalArgumentException("#text cannot be used as an attribute in XML")
        }
    }

    constructor(name: String, fields: List<Gene>, refType: String? = null) : this(
        name, fixedFields = fields, refType = refType, isFixed = true, template = null, additionalFields = null, attributeNames = emptySet()
    )

    override fun copyContent(): Gene {
        val copiedAdditional = additionalFields
            ?.map { it.copy() }
            ?.filterIsInstance<PairGene<StringGene, Gene>>()
            ?.toMutableList()

        return ObjectWithAttributesGene(
            name,
            fixedFields.map { it.copy() },
            refType,
            isFixed,
            template,
            copiedAdditional,
            attributeNames.toMutableSet()
        )
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {

        if (other !is ObjectGene) {
            return false
        }

        var ok = true

        for (field in fixedFields) {
            val toCopy = other.fixedFields.find { it.name == field.name }
                ?: continue

            val copied = field.unsafeCopyValueFrom(toCopy)

            if (!copied && field.containsSameValueAs(toCopy)) {
                continue
            }

            ok = ok && copied
        }

        return ok
    }

    override fun containsSameValueAs(other: Gene): Boolean {

        // Permitimos comparar contra ObjectGene también
        if (other !is ObjectGene) {
            return false
        }

        // Delegar completamente al valor de los fields
        return super.containsSameValueAs(other)
    }

    private fun printAttribute(
        previousGenes: List<Gene>,
        targetFormat: OutputFormat?,
        field: Gene
    ): String {
        val raw = field.getValueAsPrintableString(
            previousGenes,
            GeneUtils.EscapeMode.XML,
            targetFormat
        )

        val clean = cleanXmlValueString(raw)
        return "${field.name}=\"$clean\""
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        if (mode != GeneUtils.EscapeMode.XML) {
            return super.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
        }

        val includedFields = fixedFields
            .filter { it !is CycleObjectGene }
            .filter { it !is OptionalGene || (it.isActive && it.gene !is CycleObjectGene) }
            .filter { it.isPrintable() }

        val attributeFields = includedFields.filter { attributeNames.contains(it.name) }
        val childFields = includedFields.filter { !attributeNames.contains(it.name) }

        val attributesString = attributeFields.joinToString(" ") { printAttribute(previousGenes, targetFormat, it) }
        val sb = StringBuilder()

        if (attributesString.isEmpty()) { //No childs
            sb.append("<$name>")
        } else {
            sb.append("<$name $attributesString>")
        }

        for (child in childFields) { //Childs

            val childXml = child.getValueAsPrintableString(
                previousGenes,
                GeneUtils.EscapeMode.XML,
                targetFormat
            )

            val isInlineValue = child.name == contentXMLTag && !(child is ObjectWithAttributesGene)

            if (isInlineValue) {
                sb.append(childXml)
                continue
            }

            if (child is ObjectWithAttributesGene || child is ObjectGene) {
                sb.append(childXml)
                continue
            }

            sb.append("<${child.name}>")
            sb.append(childXml)
            sb.append("</${child.name}>")
        }

        sb.append("</$name>")
        return sb.toString()
    }
}