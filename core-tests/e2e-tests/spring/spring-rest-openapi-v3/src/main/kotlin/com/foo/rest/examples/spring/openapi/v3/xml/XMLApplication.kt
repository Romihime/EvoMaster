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

    // Endpoint 1: JSON y XML con mismo nombre
    @PostMapping(
        "/book",
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun processBook(@RequestBody book: Book): String {
        return when {
            book.title.isBlank() -> "missing_title"
            book.title == "The Great Book" && book.pages > 300 -> "great_book"
            book.pages >= 100 -> "valid_book"
            book.pages < 10 -> "too_short"
            else -> "unknown_book"
        }
    }

    // Endpoint 2: Solo JSON (para comparar)
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

    // Endpoint 3: Responde XML (testear generación XML)
    @PostMapping(
        "/create-book",
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.APPLICATION_XML_VALUE]
    )
    fun createBook(@RequestBody title: String): Book {
        return Book(
            title = title,
            pages = title.length * 10
        )
    }
}



/* ===================== MODELS ===================== */

@XmlRootElement(name = "book")
@XmlAccessorType(XmlAccessType.FIELD)
data class Book(
    @XmlElement(name = "title")
    var title: String = "",

    @XmlElement(name = "pages")
    var pages: Int = 0
)

data class Author(
    var name: String = "",
    var birthYear: Int = 0
)