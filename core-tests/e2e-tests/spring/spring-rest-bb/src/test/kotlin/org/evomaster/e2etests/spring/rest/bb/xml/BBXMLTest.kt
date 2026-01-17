package org.evomaster.e2etests.spring.rest.bb.xml

import com.foo.rest.examples.bb.xml.BBXMLController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled


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

    fun testRunEMGeneric(basicAssertions: Boolean, className: ClassName, outputFormat: OutputFormat? = OutputFormat.JAVA_JUNIT_5) {

        val lambda = { args: MutableList<String> ->
            args.add("--enableBasicAssertions")
            args.add(basicAssertions.toString())

            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)

            /* ========= string / person ========= */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/receive-string-respond-xml", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/receive-string-respond-xml", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/receive-xml-respond-string", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/receive-xml-respond-string", null)

            /* ========= nesting ========= */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/employee", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/employee", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/company", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/company", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/department", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/department", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/organization", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/organization", null)

            /* ========= attributes ========= */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/project", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/project", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/projects", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/projects", null)

            /* ========= person with attributes ========= */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/person-with-attr", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbxml/person-with-attr", null)
        }
    }
}