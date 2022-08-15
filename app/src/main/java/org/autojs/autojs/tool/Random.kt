package org.autojs.autojs.tool

fun getRandomString(length: Int) : String {
    val charset = ('a'..'z')
    return (0 until length)
        .map { charset.random() }
        .joinToString("")
}

fun main() {
    val length = 10

    val randomString = getRandomString(length)

    println(randomString)        // SamSU712JQ
}
