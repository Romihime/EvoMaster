import com.foo.rest.examples.spring.openapi.v3.xmlController.XmlController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.evomaster.client.java.instrumentation.shared.ClassName

class XmlEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            initClass(XmlController(), config)
        }
    }

    @Test
    fun testRunEM() {

        val className = ClassName("org.foo.XmlEM")
        val outputFormat = OutputFormat.JAVA_JUNIT_5

        testRunEMGeneric(true, className, outputFormat)

    }

    fun testRunEMGeneric(basicAssertions: Boolean, className: ClassName, outputFormat: OutputFormat? = OutputFormat.JAVA_JUNIT_5){

        val lambda = { args : MutableList<String> ->
            args.add("--enableBasicAssertions")
            args.add(basicAssertions.toString())

            setOutputFormat(args, outputFormat)

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
        }

    }
}