package org.evomaster.e2etests.spring.openapi.v3.jsonpatch

import com.foo.rest.examples.spring.openapi.v3.jsonpatch.JsonPatchController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * White-box E2E test for JSON Patch (RFC 6902) support.
 *
 * This test verifies that EvoMaster can:
 * 1. Detect the application/json-patch+json content type in the OpenAPI spec
 * 2. Use the GET endpoint schema to generate schema-aware JSON Patch operations
 * 3. Generate valid JSON Patch documents that the SUT can process
 * 4. Explore different HTTP status codes (200, 400, 404)
 */
class JsonPatchEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(JsonPatchController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "JsonPatchEM",
            "org.foo.JsonPatchEM",
            2000,
            true,
            { args: MutableList<String> ->

                val solution = initAndRun(args)
                assertTrue(solution.individuals.size >= 1)

                /* GET endpoint */
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/jsonpatch/{id}", null)
                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/jsonpatch/{id}", null)

                /* PATCH endpoint: successful patch application */
                assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/api/jsonpatch/{id}", null)

                /* PATCH endpoint: invalid patch (400) */
                assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/api/jsonpatch/{id}", null)
            },
            3,
        )
    }
}