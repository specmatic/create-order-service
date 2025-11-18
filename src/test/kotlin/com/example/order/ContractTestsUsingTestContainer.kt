package com.example.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.time.Duration

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContractTestsUsingTestContainer {
    companion object {
        private val DOCKER_COMPOSE_FILE = File("docker-compose.yaml")
        private const val REGISTER_SCHEMAS_SERVICE = "register-schemas"
        private const val SCHEMA_REGISTERED_REGEX = ".*(?i)schemas registered.*"
    }

    private val applicationHost = "host.docker.internal"
    private val applicationPort = 8080
    private val actuatorEndpoint = "http://$applicationHost:$applicationPort/actuator/mappings"
    private val excludedEndpoint = "'/health,/swagger/v1/swagger,/swagger-ui.html'"

    @Value("\${spring.kafka.properties.schema.registry.url}")
    lateinit var schemaRegistryUrl: String

    @Value("\${spring.kafka.bootstrap-servers}")
    lateinit var kafkaBootstrapServers: String

    private val schemaRegistry = schemaRegistry()

    private fun schemaRegistry(): ComposeContainer {
        return ComposeContainer(DOCKER_COMPOSE_FILE)
            .withLocalCompose(true).waitingFor(
                REGISTER_SCHEMAS_SERVICE,
                LogMessageWaitStrategy()
                    .withRegEx(SCHEMA_REGISTERED_REGEX)
                    .withStartupTimeout(Duration.ofSeconds(60))
            )
    }

    private fun GenericContainer<*>.configure(waitLogMessage: String): GenericContainer<*> {
        return this.withEnv(
            mapOf(
                "SCHEMA_REGISTRY_URL" to schemaRegistryUrl,
                "SCHEMA_REGISTRY_KIND" to "CONFLUENT",
                "AVAILABLE_SERVERS" to kafkaBootstrapServers
            )
        )
            .withFileSystemBind(
                "./specmatic.yaml",
                "/usr/src/app/specmatic.yaml",
                BindMode.READ_ONLY,
            ).withFileSystemBind(
                "./build/reports/specmatic",
                "/usr/src/app/build/reports/specmatic",
                BindMode.READ_WRITE,
            ).waitingFor(Wait.forLogMessage(".*$waitLogMessage.*", 1))
            .withNetworkMode("host")
            .withLogConsumer { print(it.utf8String) }
    }

    private fun stubContainer(): GenericContainer<*> {
        return GenericContainer("specmatic/specmatic-kafka")
            .withCommand(
                "virtualize",
                "--broker=$kafkaBootstrapServers",
            ).configure("KafkaMock has started")
    }

    private val testContainer: GenericContainer<*> =
        GenericContainer("specmatic/specmatic-openapi")
            .withCommand(
                "test",
                "--host=$applicationHost",
                "--port=$applicationPort",
                "--filter=PATH!=$excludedEndpoint",
            ).withEnv("endpointsAPI", actuatorEndpoint)
            .withFileSystemBind(
                "./specmatic.yaml",
                "/usr/src/app/specmatic.yaml",
                BindMode.READ_ONLY,
            ).withFileSystemBind(
                "./build/reports/specmatic",
                "/usr/src/app/build/reports/specmatic",
                BindMode.READ_WRITE,
            ).waitingFor(Wait.forLogMessage(".*Tests run:.*", 1))
            .withExtraHost("host.docker.internal", "host-gateway")
            .withLogConsumer { print(it.utf8String) }

    @BeforeAll
    fun setup() {
        schemaRegistry.start()
    }

    @AfterAll
    fun tearDown() {
        schemaRegistry.stop()
    }

    @Test
    fun specmaticContractTest() {
        val stubContainer = stubContainer()
        try {
            stubContainer.start()
            testContainer.start()
            val hasSucceeded = testContainer.logs.contains("Failures: 0")
            assertThat(hasSucceeded).isTrue()
        } finally {
            // wait for message to be published on new-orders topic
            Thread.sleep(3000)
            stubContainer.execInContainer(
                "curl",
                "-sS",
                "-X",
                "POST",
                "http://localhost:9999/stop"
            )
            stubContainer.stop()
        }
    }
}
