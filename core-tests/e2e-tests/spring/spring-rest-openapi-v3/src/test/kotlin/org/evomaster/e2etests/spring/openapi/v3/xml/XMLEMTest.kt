package org.evomaster.e2etests.spring.openapi.v3.xml

import com.foo.rest.examples.spring.openapi.v3.xml.XMLController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test



/**
 * White-box E2E test for XML handling with attributes.
 * Tests that EvoMaster can properly handle REST APIs that accept both JSON and XML,
 * and properly parse/generate XML attributes (e.g., @XmlAttribute).
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
            500
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/book", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/author", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/create-book", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/author", "young_author")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/author", "classic_author")
        }
    }

}
