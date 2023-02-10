package com.frankie.server

import io.micronaut.runtime.Micronaut

object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .args(*args)
                .packages("com.frankie.server")
                .mainClass(Server.javaClass)
                .start()
    }
}