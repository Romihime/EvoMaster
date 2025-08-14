package org.evomaster.e2etests.spring.openapi.v3.xmlTest


import com.foo.rest.examples.spring.openapi.v3.xmlController.XmlController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class XmlEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()

            config.outputFormat = OutputFormat.JAVA_JUNIT_5

            initClass(XmlController(), config)
        }
    }
    @Disabled
    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "XmlEM",
            "org.foo.XmlEM",
            100,
            true,
            { args: MutableList<String> ->
                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)

                assertHasAtLeastOne(
                    solution,
                    HttpVerb.POST,
                    200,
                    "/api/xml/receive-string-respond-xml",
                    null
                )

                assertHasAtLeastOne(
                    solution,
                    HttpVerb.POST,
                    200,
                    "/api/xml/receive-xml-respond-string",
                    null
                )
            },
            3
        )
    }
}