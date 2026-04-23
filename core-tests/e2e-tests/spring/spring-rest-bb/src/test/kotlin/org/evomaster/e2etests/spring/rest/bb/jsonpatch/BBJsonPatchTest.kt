package org.evomaster.e2etests.spring.rest.bb.jsonpatch

import com.foo.rest.examples.bb.jsonpatch.BBJsonPatchController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBJsonPatchTest : SpringTestBase() {

    companion object {

        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            initClass(BBJsonPatchController(), config)
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "BBJsonPatchEM",
            1000,
            3,
            listOf("JSONPATCH_GET", "JSONPATCH_APPLIED",
                "JSONPATCH_ADD", "JSONPATCH_REPLACE", "JSONPATCH_REMOVE",
                "JSONPATCH_TEST_FAIL")
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            /* GET endpoint should be reachable */
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/jsonpatch/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/jsonpatch/{id}", null)

            /* PATCH endpoint: at least a successful application, a 400, and a 409 (test fail) */
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/api/jsonpatch/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/api/jsonpatch/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 409, "/api/jsonpatch/{id}", null)
        }
    }
}