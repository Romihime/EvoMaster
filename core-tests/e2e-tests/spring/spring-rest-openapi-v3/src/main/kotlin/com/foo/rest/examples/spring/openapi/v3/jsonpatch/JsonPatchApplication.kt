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
        node1.put("role", "admin")
        resources[1] = node1

        val node2 = mapper.createObjectNode()
        node2.put("name", "Bob")
        node2.put("age", 25)
        node2.put("active", false)
        node2.put("role", "user")
        resources[2] = node2
    }

    /* ===================== DATA CLASS FOR SCHEMA ===================== */

    data class Resource(
        val name: String = "",
        val age: Int = 0,
        val active: Boolean = false,
        val role: String = "user"
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
        ApiResponse(responseCode = "404", description = "Resource not found"),
        ApiResponse(responseCode = "422", description = "Patch violates business rules")
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

        if (patchNode.size() == 0) {
            return ResponseEntity.status(400).body(mapOf("error" to "patch must not be empty"))
        }

        // Apply each operation
        val result = resource.deepCopy()
        for (opNode in patchNode) {
            val op = opNode.get("op")?.asText()
                ?: return ResponseEntity.status(400).body(mapOf("error" to "missing op"))
            val path = opNode.get("path")?.asText()
                ?: return ResponseEntity.status(400).body(mapOf("error" to "missing path"))

            val fieldName = path.removePrefix("/")
            if (fieldName.isEmpty() || fieldName.contains("/")) {
                return ResponseEntity.status(400).body(mapOf("error" to "only single-level paths supported"))
            }

            when (op) {
                "add" -> {
                    val value = opNode.get("value")
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "missing value for add"))
                    if (result.has(fieldName)) {
                        return ResponseEntity.status(400).body(mapOf("error" to "field already exists, use replace"))
                    }
                    result.set<JsonNode>(fieldName, value)
                }
                "replace" -> {
                    val value = opNode.get("value")
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "missing value for replace"))
                    if (!result.has(fieldName)) {
                        return ResponseEntity.status(400).body(mapOf("error" to "field $fieldName not found for replace"))
                    }
                    result.set<JsonNode>(fieldName, value)
                }
                "remove" -> {
                    if (!result.has(fieldName)) {
                        return ResponseEntity.status(400).body(mapOf("error" to "field $fieldName not found"))
                    }
                    // Prevent removing required fields
                    if (fieldName == "name" || fieldName == "role") {
                        return ResponseEntity.status(422).body(mapOf("error" to "cannot remove required field: $fieldName"))
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
                    if (fromField == "name" || fromField == "role") {
                        return ResponseEntity.status(422).body(mapOf("error" to "cannot move required field: $fromField"))
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
                    val copiedValue = result.get(fromField)
                        ?: return ResponseEntity.status(400).body(mapOf("error" to "from field not found"))
                    result.set<JsonNode>(fieldName, copiedValue.deepCopy())
                }
                else -> {
                    return ResponseEntity.status(400).body(mapOf("error" to "unknown op: $op"))
                }
            }
        }

        // Business rule validation on the result
        val name = result.get("name")?.asText() ?: ""
        if (name.isBlank()) {
            return ResponseEntity.status(422).body(mapOf("error" to "name must not be blank"))
        }
        if (name.length > 50) {
            return ResponseEntity.status(422).body(mapOf("error" to "name too long"))
        }

        val age = result.get("age")?.asInt() ?: 0
        if (age < 0 || age > 150) {
            return ResponseEntity.status(422).body(mapOf("error" to "age out of range"))
        }

        val role = result.get("role")?.asText() ?: ""
        if (role !in listOf("admin", "user", "guest")) {
            return ResponseEntity.status(422).body(mapOf("error" to "invalid role: $role"))
        }

        // Admin must be at least 21
        if (role == "admin" && age < 21) {
            return ResponseEntity.status(422).body(mapOf("error" to "admin must be at least 21"))
        }

        resources[id] = result
        return ResponseEntity.ok(mapper.treeToValue(result, Map::class.java))
    }
}