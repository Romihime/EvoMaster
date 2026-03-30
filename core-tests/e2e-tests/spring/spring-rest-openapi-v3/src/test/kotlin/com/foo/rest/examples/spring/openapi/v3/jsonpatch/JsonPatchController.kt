package com.foo.rest.examples.spring.openapi.v3.jsonpatch

import com.foo.rest.examples.spring.openapi.v3.SpringController

class JsonPatchController : SpringController(JsonPatchApplication::class.java) {

    override fun resetStateOfSUT() {
        val app = ctx?.getBean(JsonPatchApplication::class.java)
        app?.resetState()
    }
}