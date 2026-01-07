package org.evomaster.e2etests.spring.rest.bb.xml

import com.foo.rest.examples.bb.xml.BBXMLController
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

    @Disabled
    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "BBXmlEM",
            "org.foo.XmlEM",
            100
        ) { args: MutableList<String> ->

            addBlackBoxOptions(args, OutputFormat.JAVA_JUNIT_5)
            args.add("--enableBasicAssertions")
            args.add("true")

            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/receive-string-respond-xml", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/receive-xml-respond-string", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/employee", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/company", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/department", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/organization", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/projects", null)

            // Test XML attribute handling - endpoint /project requires proper parsing of @XmlAttribute
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/project", null)
        }
    }

    @Disabled
    @Test
    fun testRunEMWithAttributeFocus() {

        runTestHandlingFlakyAndCompilation(
            "BBXmlAttrEM",
            "org.foo.XmlAttrEM",
            100
        ) { args: MutableList<String> ->

            addBlackBoxOptions(args, OutputFormat.JAVA_JUNIT_5)
            args.add("--enableBasicAssertions")
            args.add("true")

            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)

            // Focus on endpoints that use XML attributes
            // /project endpoint checks for code attribute and member ids
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/project", "missing code")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/project", "no members")
            // /projects endpoint - list of objects with attributes
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/projects", "no projects")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/projects", "invalid projects")

            // This requires proper handling of multiple projects with code attributes and member ids
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbxml/projects", "valid projects")
        }
    }
}