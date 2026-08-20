package com.irontrail.api.testsupport

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

// Started once per JVM (singleton container pattern) and shared across every repository test
// class that extends this base, rather than one container per class - Flyway migrations plus
// a fresh Postgres instance is too slow to pay per test class.
private val postgres: PostgreSQLContainer =
    PostgreSQLContainer("postgres:16")
        .withDatabaseName("irontrail_test")
        .withUsername("irontrail_test")
        .withPassword("irontrail_test")
        .apply { start() }

// AutoConfigureTestDatabase.Replace.NONE: @DataJpaTest defaults to swapping in an embedded
// in-memory DB, which would skip Flyway/real Postgres SQL semantics entirely (exactly the class
// of bug this project has hit twice before - JPQL IS EMPTY and null-bytea inference, both
// Postgres/Hibernate-specific, both invisible against H2). Keeping the real container is the point.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class RepositoryTestBase {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
