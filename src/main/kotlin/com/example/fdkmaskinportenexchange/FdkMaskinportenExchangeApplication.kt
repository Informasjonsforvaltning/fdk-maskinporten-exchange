package com.example.fdkmaskinportenexchange

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.SpringApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class FdkMaskinportenExchangeApplication

fun main(args: Array<String>) {
    SpringApplication.run(FdkMaskinportenExchangeApplication::class.java, *args)
}
