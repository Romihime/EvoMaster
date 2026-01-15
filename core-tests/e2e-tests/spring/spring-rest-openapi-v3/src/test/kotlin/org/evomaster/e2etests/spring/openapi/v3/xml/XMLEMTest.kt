package org.evomaster.e2etests.spring.openapi.v3.xml

import com.foo.rest.examples.spring.openapi.v3.xml.XMLController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test



/**
 * White-box E2E test for XML handling with attributes.
 *
 * This test verifies that EvoMaster can properly:
 * 1. Handle REST APIs that accept both JSON and XML payloads on the same endpoint
 * 2. Parse XML attributes (@XmlAttribute) correctly, not just elements (@XmlElement)
 * 3. Generate test cases that cover different branches based on attribute values
 *
 * Key scenarios tested:
 * - /product: Dual JSON/XML endpoint with @XmlAttribute for 'sku' field
 * - /order: XML-only endpoint with nested objects containing @XmlAttribute
 * - /author: JSON-only endpoint (for comparison)
 * - /create-product: Endpoint that returns XML with attributes
 */
class XMLEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(XMLController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "XMLEM",
            "org.foo.XMLEM",
            500,
            true,
            { args: MutableList<String> ->

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)

                // Test dual JSON/XML endpoint with @XmlAttribute
                // The 'sku' field is an XML attribute, testing proper attribute parsing
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/product", "regular_product")
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/product", "premium_product")


                // Test JSON-only endpoint (for comparison)
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/author", null)
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/author", "young_author")
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/author", "classic_author")

                // Test XML-only endpoint with nested @XmlAttribute
                // Both 'orderId' and 'itemCode' are XML attributes
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/order", null)

                // Test endpoint that produces XML response with attributes
                assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/create-product", null)

                // Verify no 500 errors (proper XML parsing)
                assertNone(solution, HttpVerb.POST, 500)
            },
            3,
        )

    }

}