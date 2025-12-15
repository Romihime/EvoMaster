package com.foo.rest.examples.bb.xml

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import java.net.URL


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/bbxml"])
@RestController
open class BBXMLApplication {



}