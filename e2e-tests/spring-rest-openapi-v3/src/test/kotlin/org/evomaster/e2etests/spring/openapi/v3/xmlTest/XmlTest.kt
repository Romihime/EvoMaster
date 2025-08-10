package org.evomaster.e2etests.spring.openapi.v3.xmlTest

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.output.EvaluatedIndividualBuilder.Companion.buildEvaluatedIndividual
import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.sql.SqlXMLGene
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.Table
import com.foo.rest.examples.spring.openapi.v3.xmlController.XmlController
import io.restassured.RestAssured.given
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class XmlEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(XmlController())
        }
    }

    @Test
    fun test_receiveStringRespondsWithExpectedXml() {
        val input = "TestName"

        val response = given()
            .contentType("text/plain")
            .accept("application/xml")
            .body(input)
            .post("/api/xml/receive-string-respond-xml")

        /*val statusCode = response.statusCode()
        val contentType = response.contentType()
        val responseBody = response.body().asString()

        println("Status: $statusCode")
        println("Content-Type: $contentType")
        println("Body:\n$responseBody")*/

        Assertions.assertEquals(200, response.statusCode())
        Assertions.assertTrue(response.contentType().contains("application/xml"))
        Assertions.assertTrue(response.asString().contains("<name>$input</name>"))
        Assertions.assertTrue(response.asString().contains("<age>25</age>"))
    }
}