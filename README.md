![Application Architecture](./Architecture.gif)

# Specmatic Sample: Spring Boot API pushing AVRO messages to Kafka

* [Specmatic Website](https://specmatic.io)
* [Specmatic Documentation](https://docs.specmatic.io)

This sample project demonstrates how we can practice contract-driven development and contract testing in a REST (Kotlin) API that sends AVRO messages to a Kafka topic.
Here, Specmatic is used to mock calls the Kafka dependency using an AsyncAPI specification.

### Running the contract tests

#### 1. Start the Schema Registry Server
```shell
docker compose up -d
```

#### 2. Start the Specmatic async mock server

- On Unix and Windows Powershell:

```shell
docker run --rm --network host -v "$(pwd):/usr/src/app" specmatic/enterprise mock
```

- On Windows CMD Prompt:
```shell
docker run --rm --network host -v "%cd%:/usr/src/app" specmatic/enterprise mock
```

#### 3. Run the app

- On Unix and Windows Powershell:

```shell
./gradlew bootRun
```

- On Windows CMD Prompt:

```shell
gradlew bootRun
```

#### 4. Run the contract tests using Docker

- On Unix and Windows Powershell:

```shell
docker run --rm --network host -v "$(pwd):/usr/src/app" specmatic/enterprise test
```

- On Windows CMD Prompt:

```shell
docker run --rm --network host -v "%cd%:/usr/src/app" specmatic/enterprise test
```
