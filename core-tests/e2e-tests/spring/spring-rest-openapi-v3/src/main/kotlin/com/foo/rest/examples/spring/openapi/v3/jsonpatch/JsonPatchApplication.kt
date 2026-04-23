package com.foo.rest.examples.spring.openapi.v3.jsonpatch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/jsonpatch"])
@RestController
open class JsonPatchApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(JsonPatchApplication::class.java, *args)
        }
    }

    private val mapper = ObjectMapper()

    /**
     * In-memory resource store keyed by id.
     */
    private val resources = mutableMapOf<Int, ObjectNode>()

    init {
        resetState()
    }

    fun resetState() {
        resources.clear()

        val node1 = mapper.createObjectNode()
        node1.put("name", "Alice")
        node1.put("age", 30)
        node1.put("active", true)
        resources[1] = node1

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
    fun getResource(@PathVariable id: Int): ResponseEntity<Any> {
        val resource = resources[id]
            ?: return ResponseEntity.status(404).body(mapOf("error" to "not found"))

        return ResponseEntity.ok(mapper.treeToValue(resource, Map::class.java))
    }

    /* ===================== PATCH ENDPOINT (JSON Patch RFC 6902) ===================== */

    @Operation(summary = "Apply a JSON Patch to a resource")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Patch applied successfully"),
        ApiResponse(responseCode = "400", description = "Invalid patch"),
        ApiResponse(responseCode = "404", description = "Resource not found")
    ])
    @PatchMapping(
        "/{id}",
        consumes = ["application/json-patch+json"],
        produces = ["application/json"]
    )
    fun patchResource(
        @PathVariable id: Int,
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
        val validFields = setOf("name", "age", "active")

        for (opNode in patchNode) {
            val op = opNode.get("op")?.asText()
                ?: return ResponseEntity.status(400).body(mapOf("error" to "missing op"))
            val path = opNode.get("path")?.asText()
                ?: return ResponseEntity.status(400).body(mapOf("error" to "missing path"))

            val fieldName = path.removePrefix("/")
            if (fieldName.isEmpty() || fieldName.contains("/")) {
                return ResponseEntity.status(400).body(mapOf("error" to "only single-level paths supported"))
            }
            if (fieldName !in validFields) {
                return ResponseEntity.status(400).body(mapOf("error" to "unknown field: $fieldName"))
            }

            when (op) {
                "add", "replace" -> {
                    val value = opNode.get("value")
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "missing value for $op"))
                    result.set<JsonNode>(fieldName, value)
                }
                "remove" -> {
                    if (!result.has(fieldName)) {
                        return ResponseEntity.status(400).body(mapOf("error" to "field $fieldName not found"))
                    }
                    result.remove(fieldName)
                }
                "test" -> {
                    val value = opNode.get("value")
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "missing value for test"))
                    val current = result.get(fieldName)
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "field $fieldName not found"))
                    if (current != value) {
                        return ResponseEntity.status(400).body(mapOf("error" to "test failed for $fieldName"))
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

        resources[id] = result
        return ResponseEntity.ok(mapper.treeToValue(result, Map::class.java))
    }
}