package com.foo.rest.examples.spring.openapi.v3.xmlController

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.xmlApplication.XmlApplication

class XmlController : SpringController(XmlApplication::class.java) {
}