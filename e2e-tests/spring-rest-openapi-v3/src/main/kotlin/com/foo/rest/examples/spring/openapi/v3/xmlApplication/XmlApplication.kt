package com.foo.rest.examples.spring.openapi.v3.xmlApplication

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
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

    @PostMapping(
        path = ["/company"],
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun companyEndpoint(@RequestBody company: Company): ResponseEntity<String> {
        return if (company.employees.size > 2 && company.employees.any { it.age > 40 }) {
            ResponseEntity.ok("big company with seniors")
        } else {
            ResponseEntity.ok("small company")
        }
    }

    @PostMapping(
        path = ["/employee"],
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun employeeEndpoint(@RequestBody emp: Employee): ResponseEntity<String> {
        return if (emp.role == Role.ADMIN && emp.person.age > 30) {
            ResponseEntity.ok("experienced admin")
        } else {
            ResponseEntity.ok("not admin or too young")
        }
    }
}

@XmlRootElement(name = "person")
data class Person(
    @field:NotBlank
    var name: String = "",
    @field:Min(18)
    @field:Max(99)
    var age: Int = 0
)

@XmlRootElement(name = "company")
data class Company(
    var name: String = "",
    var employees: List<Person> = mutableListOf()
)

enum class Role { ADMIN, USER, GUEST }

@XmlRootElement(name = "employee")
data class Employee(
    var person: Person = Person(),
    var role: Role = Role.USER
)