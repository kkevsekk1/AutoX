package org.autojs.autojs

import org.junit.Test

class Test {
    @Test
    fun test(){
        println("ws://192.168.2.209:9317".matches(Regex("^(ws|wss)://.*")))
    }
}