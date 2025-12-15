package org.evomaster.e2etests.spring.rest.bb.xml

import com.foo.rest.examples.bb.xml.BBXMLController
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
            initClass(BBXMLController())
        }
    }

    @Test
    fun testGeneratedTestsForJsonAndXml() {
        println("Starting BBXML test...")

        val args = mutableListOf<String>()

        setOption(args, "blackBox", "true")
        setOption(args, "bbSwaggerUrl", "file:///C:/Users/Usuario/Documents/GitHub/petstore.txt")
        setOption(args, "bbTargetUrl", "http://localhost:8080")
        setOption(args, "outputFormat", "JAVA_JUNIT_5")
        setOption(args, "outputFolder", "./evomaster-tests")
        setOption(args, "maxTime", "30s")
        setOption(args, "ratePerMinute", "60")
        setOption(args, "blackBoxCleanUp", "false")

        val solution = initAndRun(args)

        println("=== SOLUTION ===")
        println(solution)

        assertTrue(solution.individuals.isNotEmpty())

        // tus validaciones personalizadas
        assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/pet", null)
        assertHasAtLeastOne(solution, HttpVerb.PUT, 400, "/api/pet", null)
    }
}