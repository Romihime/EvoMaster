package com.foo.rest.examples.bb.xml

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import java.net.URL
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbxml"])
@RestController
open class BBXMLApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBXMLApplication::class.java, *args)
        }
    }

    //1.
    @PostMapping("/receive-string-respond-xml", consumes = ["text/plain"], produces = ["application/xml"])
    fun stringToXml(@RequestBody body: String): Person {
        return Person(name = body, age = body.length)
    }

    //2.
    @PostMapping("/receive-xml-respond-string", consumes = ["application/xml"], produces = ["text/plain"])
    fun xmlToString(@RequestBody person: Person): String {
        return "not ok"
    }

    //3. 2 level of nesting
    @PostMapping("/employee", consumes = ["application/xml"], produces = ["text/plain"])
    fun employee(@RequestBody employee: Employee): String {
        return if (employee.role == Role.ADMIN && employee.person.age > 30)
            "admin"
        else
            "not admin or too young"
    }

    //4. 3 level of nesting
    @PostMapping("/company", consumes = ["application/xml"], produces = ["text/plain"])
    fun company(@RequestBody company: Company): String {
        return if (company.employees.isEmpty()) "small company" else "big company"
    }

    //5. loop
    @PostMapping("/department", consumes = ["application/xml"], produces = ["text/plain"])
    fun department(@RequestBody department: Department): String {
        return "department with ${department.employees.size + 1} employees"
    }

    //6. 3 lists
    @PostMapping("/organization", consumes = ["application/xml"], produces = ["text/plain"])
    fun organization(@RequestBody organization: Organization): String {
        return "organization with ${organization.people.size} people"
    }

    //7. attributes
    @PostMapping("/project", consumes = ["application/xml"], produces = ["text/plain"])
    fun project(@RequestBody project: Project): String {

        if (project.code.isBlank())
            return "missing code"

        if (project.members.isEmpty())
            return "no members"

        var adults = 0

        for (m in project.members) {
            if (m.id.isNotBlank() && m.age >= 18)
                adults++
        }

        return if (adults > 0)
            "project ${project.code} has $adults adult members"
        else
            "project ${project.code} has only minors"
    }

    //8. list of objs with attributes
    @PostMapping(
        "/projects",
        consumes = ["application/xml"],
        produces = ["text/plain"]
    )
    fun postProjects(@RequestBody list: ProjectList): String {

        if (list.projects.isEmpty())
            return "no projects"

        var members = 0
        var hasCode = false

        for (p in list.projects) {
            if (p.code.isNotBlank())
                hasCode = true

            for (m in p.members) {
                if (m.id.isNotBlank())
                    members++
            }
        }

        return if (hasCode && members > 0)
            "valid projects with $members members"
        else
            "invalid projects"
    }


}

/* ===================== MODELS (JAXB) ===================== */


@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.FIELD)
open class Person(
    var name: String = "",
    var age: Int = 0
)

@XmlRootElement(name = "employee")
@XmlAccessorType(XmlAccessType.FIELD)
open class Employee(
    var person: Person = Person(),
    var role: Role = Role.USER
)

@XmlRootElement(name = "company")
@XmlAccessorType(XmlAccessType.FIELD)
open class Company(
    var name: String = "",
    @field:XmlElement(name = "Person", namespace = "")
    var employees: List<Person> = emptyList()
)

enum class Role { ADMIN, USER, GUEST }

@XmlRootElement(name = "department")
@XmlAccessorType(XmlAccessType.FIELD)
open class Department(
    var name: String = "",
    @field:XmlElement(name = "Employee", namespace = "")
    var employees: List<Employee> = emptyList(),
    @field:XmlElement(name = "Department", namespace = "")
    var subDepartments: List<Department> = emptyList()
)

@XmlRootElement(name = "organization")
@XmlAccessorType(XmlAccessType.FIELD)
open class Organization(
    var name: String = "",
    @field:XmlElement(name = "Person", namespace = "")
    var people: List<Person> = emptyList(),
    @field:XmlElement(name = "Employee", namespace = "")
    var employees: List<Employee> = emptyList(),
    @field:XmlElement(name = "Company", namespace = "")
    var companies: List<Company> = emptyList()
)

@XmlRootElement(name = "personWithAttr")
@XmlAccessorType(XmlAccessType.FIELD)
open class PersonWithAttr(
    @XmlAttribute(name = "id")
    var id: String = "",
    var name: String = "",
    var age: Int = 0
)

@XmlRootElement(name = "project")
@XmlAccessorType(XmlAccessType.FIELD)
open class Project(
    @XmlAttribute(name = "code")
    var code: String = "",
    @field:XmlElement(name = "PersonWithAttr", namespace = "")
    var members: List<PersonWithAttr> = emptyList()
)

@XmlRootElement(name = "projectList")
@XmlAccessorType(XmlAccessType.FIELD)
open class ProjectList(
    @field:XmlElement(name = "Project", namespace = "")
    var projects: List<Project> = emptyList()
)

