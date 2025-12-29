package org.evomaster.e2etests.spring.rest.bb.xml

import com.foo.rest.examples.bb.xml.BBXMLController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll


class BBXMLTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            initClass(BBXMLController(), config)
        }
    }

    @Test
    fun testRunEM() {

        val className = ClassName("org.foo.XmlEM")
        val outputFormat = OutputFormat.JAVA_JUNIT_5

        testRunEMGeneric(true, className, outputFormat)

    }

    fun testRunEMGeneric(basicAssertions: Boolean, className: ClassName, outputFormat: OutputFormat? = OutputFormat.JAVA_JUNIT_5){

        val lambda = { args: MutableList<String> ->
            args.add("--enableBasicAssertions")
            args.add(basicAssertions.toString())

            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/receive-string-respond-xml", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/receive-xml-respond-string", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/employee", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/company", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/department", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/organization", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/projects", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/xml/project", null)
        }
    }
}