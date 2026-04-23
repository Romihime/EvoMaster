package com.foo.rest.examples.bb.jsonpatch

import com.foo.rest.examples.bb.SpringController

class BBJsonPatchController : SpringController(BBJsonPatchApplication::class.java) {

    override fun resetStateOfSUT() {
        val app = ctx?.getBean(BBJsonPatchApplication::class.java)
        app?.resetState()
    }
}