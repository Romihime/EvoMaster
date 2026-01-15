package com.foo.rest.examples.spring.openapi.v3.xml

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement



@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/xml"])
@RestController
open class XMLApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(XMLApplication::class.java, *args)
        }
    }

    /**
     * Endpoint 1: Accepts both JSON and XML for the same endpoint.
     * Uses a model with @XmlAttribute to test attribute parsing.
     */
    @PostMapping(
        "/product",
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun processProduct(@RequestBody product: Product): String {
        // Test that XML attributes are properly parsed
        if (product.sku.isBlank()) {
            return "missing_sku"
        }

        if (product.name.isBlank()) {
            return "missing_name"
        }

        // Test specific attribute values
        return when {
            product.sku == "SPECIAL-001" && product.price > 100.0 -> "premium_product"
            product.sku.startsWith("SALE-") && product.price < 50.0 -> "sale_product"
            product.price >= 1000.0 -> "luxury_product"
            product.price <= 0 -> "invalid_price"
            else -> "regular_product"
        }
    }

    /**
     * Endpoint 2: JSON only (for comparison with dual endpoint).
     */
    @PostMapping(
        "/author",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun processAuthor(@RequestBody author: Author): String {
        return when {
            author.name.isBlank() -> "missing_name"
            author.name == "Jane Doe" && author.birthYear < 1950 -> "legendary_author"
            author.birthYear > 2000 -> "young_author"
            author.birthYear in 1900..1999 -> "classic_author"
            else -> "ancient_author"
        }
    }

    /**
     * Endpoint 3: XML only with nested objects containing attributes.
     * Tests complex XML structure with @XmlAttribute at multiple levels.
     */
    @PostMapping(
        "/order",
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun processOrder(@RequestBody order: Order): String {
        if (order.orderId.isBlank()) {
            return "missing_order_id"
        }

        if (order.items.isEmpty()) {
            return "empty_order"
        }

        var totalQuantity = 0
        var hasValidItems = false

        for (item in order.items) {
            if (item.itemCode.isNotBlank() && item.quantity > 0) {
                hasValidItems = true
                totalQuantity += item.quantity
            }
        }

        return when {
            !hasValidItems -> "no_valid_items"
            order.orderId.startsWith("VIP-") && totalQuantity > 10 -> "vip_bulk_order"
            order.orderId.startsWith("VIP-") -> "vip_order"
            totalQuantity > 100 -> "bulk_order"
            totalQuantity > 0 -> "order_with_$totalQuantity"+"_items"
            else -> "invalid_order"
        }
    }

    /**
     * Endpoint 4: Returns XML response with attributes.
     */
    @PostMapping(
        "/create-product",
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.APPLICATION_XML_VALUE]
    )
    fun createProduct(@RequestBody name: String): Product {
        return Product(
            sku = "GEN-${name.hashCode().toString().takeLast(4)}",
            name = name,
            price = name.length * 9.99
        )
    }
}


/* ===================== MODELS ===================== */

/**
 * Product model with XML attributes.
 * The 'sku' field is an XML attribute, not an element.
 * This tests that EvoMaster properly handles @XmlAttribute.
 */
@XmlRootElement(name = "product")
@XmlAccessorType(XmlAccessType.FIELD)
open class Product(
    @field:XmlAttribute(name = "sku")
    var sku: String = "",

    @field:XmlElement(name = "name")
    var name: String = "",

    @field:XmlElement(name = "price")
    var price: Double = 0.0
)

/**
 * Author model - JSON only (no XML annotations).
 */
data class Author(
    var name: String = "",
    var birthYear: Int = 0
)

/**
 * OrderItem with XML attribute for itemCode.
 */
@XmlRootElement(name = "item")
@XmlAccessorType(XmlAccessType.FIELD)
open class OrderItem(
    @field:XmlAttribute(name = "itemCode")
    var itemCode: String = "",

    @field:XmlElement(name = "quantity")
    var quantity: Int = 0
)

/**
 * Order model with XML attribute and nested list of items with attributes.
 * This tests:
 * - @XmlAttribute at root level (orderId)
 * - @XmlElement with list of objects that also have @XmlAttribute (items)
 */
@XmlRootElement(name = "order")
@XmlAccessorType(XmlAccessType.FIELD)
open class Order(
    @field:XmlAttribute(name = "orderId")
    var orderId: String = "",

    @field:XmlElement(name = "item")
    var items: List<OrderItem> = emptyList()
)