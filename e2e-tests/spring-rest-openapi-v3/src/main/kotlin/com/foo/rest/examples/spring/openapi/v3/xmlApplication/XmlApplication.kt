package com.foo.rest.examples.spring.openapi.v3.xmlApplication

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.xml.bind.annotation.XmlRootElement


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping("/api/xml")
open class XmlApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(XmlApplication::class.java, *args)
        }
    }

    // 1. receive XML, respond STRING
    @PostMapping(
        path = ["/receive-xml-respond-string"],
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun xmlToString(@RequestBody input: Person): ResponseEntity<String> {
        return if (input.age in 20..30) {
            ResponseEntity.ok("ok")
        } else {
            ResponseEntity.ok("not ok")
        }
    }

    // 2. receive STRING, respond XML
    @PostMapping(
        path = ["/receive-string-respond-xml"],
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.APPLICATION_XML_VALUE]
    )
    fun stringToXml(@RequestBody input: String): ResponseEntity<Person> {
        val name = input.trim()
        return ResponseEntity.ok(Person(name, age = 25))
    }
}

@XmlRootElement(name = "person")
data class Person(
    var name: String = "",
    var age: Int = 0
)