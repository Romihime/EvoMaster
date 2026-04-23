package com.foo.rest.examples.bb.jsonpatch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/jsonpatch"])
@RestController
open class BBJsonPatchApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBJsonPatchApplication::class.java, *args)
        }
    }

    private val mapper = ObjectMapper()

    /**
     * In-memory resource store keyed by id.
     * Each resource is a JSON object with fields: name (String), age (Int), active (Boolean).
     */
    private val resources = mutableMapOf<Int, ObjectNode>()

    init {
        resetState()
    }

    fun resetState() {
        resources.clear()
        val node = mapper.createObjectNode()
        node.put("name", "Alice")
        node.put("age", 30)
        node.put("active", true)
        resources[1] = node

        val node2 = mapper.createObjectNode()
        node2.put("name", "Bob")
        node2.put("age", 25)
        node2.put("active", false)
        resources[2] = node2
    }

    /* ===================== DATA CLASS FOR SCHEMA ===================== */

    data class Resource(
        val name: String = "",
        val age: Int = 0,
        val active: Boolean = false
    )

    /* ===================== GET ENDPOINT (provides schema for JSON Patch) ===================== */

    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Resource found",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Resource::class))]),
        ApiResponse(responseCode = "404", description = "Resource not found")
    ])
    @GetMapping(
        "/{id}",
        produces = ["application/json"]
    )
    fun getResource(@Parameter(example = "1", schema = Schema(type = "integer", minimum = "1", maximum = "5")) @PathVariable id: Int): ResponseEntity<Any> {
        val resource = resources[id]
            ?: return ResponseEntity.status(404).body(mapOf("error" to "not found"))

        CoveredTargets.cover("JSONPATCH_GET")
        return ResponseEntity.ok(mapper.treeToValue(resource, Map::class.java))
    }

    /* ===================== PATCH ENDPOINT (JSON Patch RFC 6902) ===================== */

    @Operation(summary = "Apply a JSON Patch to a resource")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Patch applied successfully"),
        ApiResponse(responseCode = "400", description = "Invalid patch"),
        ApiResponse(responseCode = "404", description = "Resource not found"),
        ApiResponse(responseCode = "409", description = "JSON Patch test operation failed")
    ])
    @PatchMapping(
        "/{id}",
        consumes = ["application/json-patch+json"],
        produces = ["application/json"]
    )
    fun patchResource(
        @Parameter(example = "1", schema = Schema(type = "integer", minimum = "1", maximum = "5")) @PathVariable id: Int,
        @RequestBody patchBody: String
    ): ResponseEntity<Any> {

        val resource = resources[id]
            ?: return ResponseEntity.status(404).body(mapOf("error" to "not found"))

        val patchNode: JsonNode
        try {
            patchNode = mapper.readTree(patchBody)
        } catch (e: Exception) {
            return ResponseEntity.status(400).body(mapOf("error" to "invalid JSON"))
        }

        if (!patchNode.isArray) {
            return ResponseEntity.status(400).body(mapOf("error" to "patch must be an array"))
        }

        // Apply each operation
        val result = resource.deepCopy()
        for (opNode in patchNode) {
            val op = opNode.get("op")?.asText()
                ?: return ResponseEntity.status(400).body(mapOf("error" to "missing op"))
            val path = opNode.get("path")?.asText()
                ?: return ResponseEntity.status(400).body(mapOf("error" to "missing path"))

            // Only single-level paths pointing to existing fields are supported
            val fieldName = path.removePrefix("/")
            if (fieldName.isEmpty() || fieldName.contains("/")) {
                return ResponseEntity.status(400).body(mapOf("error" to "invalid path: only single-level paths allowed"))
            }
            val validFields = setOf("name", "age", "active")
            if (fieldName !in validFields) {
                return ResponseEntity.status(400).body(mapOf("error" to "unknown field: $fieldName"))
            }

            when (op) {
                "add", "replace" -> {
                    val value = opNode.get("value")
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "missing value for $op"))
                    result.set<JsonNode>(fieldName, value)
                    CoveredTargets.cover("JSONPATCH_${op.uppercase()}")
                }
                "remove" -> {
                    if (!result.has(fieldName)) {
                        return ResponseEntity.status(400).body(mapOf("error" to "field $fieldName not found"))
                    }
                    result.remove(fieldName)
                    CoveredTargets.cover("JSONPATCH_REMOVE")
                }
                "test" -> {
                    val value = opNode.get("value")
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "missing value for test"))
                    val current = result.get(fieldName)
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "field $fieldName not found"))
                    if (current != value) {
                        CoveredTargets.cover("JSONPATCH_TEST_FAIL")
                        return ResponseEntity.status(409).body(mapOf("error" to "test failed"))
                    }
                }
                "move" -> {
                    val from = opNode.get("from")?.asText()
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "missing from"))
                    val fromField = from.removePrefix("/")
                    if (fromField !in validFields) {
                        return ResponseEntity.status(400).body(mapOf("error" to "unknown from field: $fromField"))
                    }
                    val movedValue = result.get(fromField)
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "from field not found"))
                    result.remove(fromField)
                    result.set<JsonNode>(fieldName, movedValue)
                    // JSONPATCH_MOVE not tracked as coverage target
                }
                "copy" -> {
                    val from = opNode.get("from")?.asText()
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "missing from"))
                    val fromField = from.removePrefix("/")
                    if (fromField !in validFields) {
                        return ResponseEntity.status(400).body(mapOf("error" to "unknown from field: $fromField"))
                    }
                    val copiedValue = result.get(fromField)
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "from field not found"))
                    result.set<JsonNode>(fieldName, copiedValue.deepCopy())
                    // JSONPATCH_COPY not tracked as coverage target
                }
                else -> {
                    return ResponseEntity.status(400).body(mapOf("error" to "unknown op: $op"))
                }
            }
        }

        // Validate that the required schema fields are still present and correctly typed
        val requiredFields = mapOf("name" to "string", "age" to "number", "active" to "boolean")
        for ((field, type) in requiredFields) {
            val node = result.get(field)
                ?: return ResponseEntity.status(400).body(mapOf("error" to "patch would remove required field '$field'"))
            val valid = when (type) {
                "string" -> node.isTextual
                "number" -> node.isNumber
                "boolean" -> node.isBoolean
                else -> true
            }
            if (!valid) {
                return ResponseEntity.status(400).body(mapOf("error" to "patch would change type of field '$field'"))
            }
        }

        // In BB mode, generated tests run without state reset between calls.
        // Persisting changes would break GET assertions in generated test suites.
        // The WB variant (JsonPatchApplication) does persist, as its controller resets state.
        CoveredTargets.cover("JSONPATCH_APPLIED")
        return ResponseEntity.ok(mapper.treeToValue(result, Map::class.java))
    }
}