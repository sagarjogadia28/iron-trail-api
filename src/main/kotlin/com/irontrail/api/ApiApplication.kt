package com.irontrail.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.runApplication

// TODO(Module 3): remove this exclusion once Postgres + a real connection URL are wired up.
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
class ApiApplication

fun main(args: Array<String>) {
	runApplication<ApiApplication>(*args)
}
