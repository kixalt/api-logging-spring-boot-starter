# api-logging-spring-boot-starter 🚀
Spring Boot Starter to centralize HTTP logging:

[![codecov](https://codecov.io/github/kixalt/api-logging-spring-boot-starter/branch/main/graph/badge.svg?token=HH1TFGK3CV)](https://codecov.io/github/kixalt/api-logging-spring-boot-starter)

- **Inbound** for Spring MVC, with opt-in via annotation on endpoints; ➡️
- **Outbound** for `RestClient`, with interception of external calls; ⬅️
- **Anonymization** of sensitive headers and configured JSON fields in textual bodies. 🔒

## Features ✨

### Inbound HTTP Logging ➡️
This project provides a logging infrastructure for calls received by a Spring MVC application:

- Activation via Spring Boot properties; ✅
- Filtering by included paths (`includedPaths`);
- Explicit annotation of methods to be logged with `@LoggedRestEndpoint`;
- Logging of request and response in a single block per exchange;
- Execution time measurement; ⏱️
- Support for logging headers;
- Support for logging request/response bodies;
- Limitation of logged body size;
- Anonymization of sensitive headers and masking of configured JSON fields. 🔒

### Outbound HTTP Logging ⬅️
The project also exposes an interceptor for calls made with `RestClient`:

- Logging of HTTP method, URI, headers, and bodies;
- Grouping of request and response in a single log block per exchange;
- Execution time measurement; ⏱️
- Anonymization of sensitive headers and masking of configured JSON fields; 🔒
- Limitation of logged body size;
- Support for common textual content types.

## Supported Content Types 📝

Bodies are logged in plain text only for textual/loggable content types:

- `application/json`
- `application/xml`
- `application/x-www-form-urlencoded`
- `text/*`

For non-textual content, the project only logs the content type and payload size.

## Installation ⚙️

### Prerequisites
- Java **25**
- Gradle
- Spring Boot **4.1.0**

### Core Dependencies
The project relies notably on:

- `spring-boot-starter-webmvc`
- `spring-boot-starter-restclient`
- `dev.blaauwendraad.masker.json:json-masker` for JSON body masking

### Via Gradle

To use this library in your project, add the dependency to your `build.gradle` file:

```gradle
// build.gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation 'fr.cgtlabs:api-logging-spring-boot-starter:1.0.2' // Replace with your desired version
}
```

> No JitPack repository is required anymore.

## Configuration 💻

### Inbound Logging

```yaml application.yml
logging:
  inbound:
    enabled: true
    included-paths:
      - /api/**
    log-headers: true
    log-request-body: true
    log-response-body: true
    max-body-log-bytes: 10240
```

### Anonymization 🔒

```yaml application.yml
logging:
  anonymize:
    headers:
      - Authorization
      - Cookie
      - X-Api-Key
    body:
      - password
      - token
      - iban
      - secret
    anonymized-string: "***"
```

`logging.anonymize.body` contains JSON field names to mask in logged payloads.
When the content type is `application/json`, masking is handled with `JsonMasker`.
For other textual payloads, no structured body masking is currently applied.

### Outbound Logging

```yaml application.yml
logging:
  outbound:
    log-headers: true
    log-request-body: true
    log-response-body: true
    max-body-log-bytes: 10240
```

## Usage 💡

### 1. Log an Inbound REST Endpoint

Explicitly annotate Spring MVC methods to be traced:

```java example
import fr.cgtlabs.springboot.logging.http.inbound.LoggedRestEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserController {

    @LoggedRestEndpoint // add annotation
    @PostMapping("/search")
    ResponseEntity<String> search(@RequestBody String body) {
        return ResponseEntity.ok(body);
    }
}
```

Notes:

- Inbound logging is only activated if `logging.inbound.enabled=true`;
- At least one path must be present in `logging.inbound.included-paths`;
- Only endpoints annotated with `@LoggedRestEndpoint` are actually logged.

### 2. Connect Outbound Logging to a `RestClient`

The outbound interceptor is created via `RestClientLoggingInterceptorFactory` to explicitly associate a caller name with each client.

```java example
import fr.cgtlabs.springboot.logging.http.outbound.RestClientLoggingInterceptorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class RestClientConfiguration {

    @Bean
    RestClient customerRestClient(RestClient.Builder builder,
                                  RestClientLoggingInterceptorFactory interceptorFactory) {
        return builder
                .requestInterceptor(interceptorFactory.create("CustomerApiClient"))
                .build();
    }
}
```

## Example of Generated Logs 📊

### Inbound

```text logs
====================================================================================================
[UserController#search] HTTP inbound
------------------------------------------------------------
[UserController#search] → Inbound Request
  Handler  : UserController#search
  Method   : POST
  URI      : /api/users/search
  Headers  : Authorization: ***
             Content-Type: application/json
[UserController#search] -> Body (request, type=application/json :
{"login":"john.doe","password":"***"}

------------------------------------------------------------
[UserController#search] ← Response Sent
  Handler  : UserController#search
  Status   : 200
  Duration : 18 ms
  URI      : /api/users/search
[UserController#search] -> Body (response, type=application/json :
{"status":"ok","token":"***"}
====================================================================================================
```

### Outbound

```text logs
====================================================================================================
[CustomerApiClient] HTTP outbound
------------------------------------------------------------
[CustomerApiClient] → Outbound Request
  Method   : POST
  URI      : https://example.test/customers
  Headers  : Authorization: ***
             Content-Type: application/json
  Body (request, type=application/json :
{"customerId":"12345","secret":"***"}

------------------------------------------------------------
[CustomerApiClient] ← Response Received
  Status   : 200
  Duration : 37 ms
  URI      : https://example.test/customers
  Body (response, type=application/json :
{"status":"ok","secret":"***"}
====================================================================================================
```

## Auto-configuration ⚙️

The Spring Boot auto-configuration currently registers the **inbound** logging infrastructure via `ApiLoggingAutoConfiguration`.

The Spring Boot import file is located in:

- `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

## Limitations and Considerations ⚠️

- Structured body masking is currently applied only to `application/json` payloads via `JsonMasker`;
- XML and other non-JSON textual payloads are logged as text but are not structurally masked;
- Binary bodies are not displayed;
- Large bodies are truncated;
- Inbound logging relies on `ContentCaching*` wrappers;
- Outbound logging reads the response body for logging: this point should be validated according to your use case;
- Inbound and outbound logging now produce a single block per HTTP exchange, which improves readability but groups request and response in the same message.

## Tests ✅

The project contains tests targeting both mechanisms:

- `InboundHttpLoggingFilterTest`
- `RestClientLoggingInterceptorTest`

Run tests:

```sh
./gradlew test
```

## Project Structure 📁

```text
src/main/java/fr/cgtlabs/springboot/logging/
├── autoconfigure/
├── http/inbound/
├── http/outbound/
├── properties/
└── utils/
```

## Summary 🌟

This starter provides a reusable HTTP logging foundation for Spring Boot with:

- **Opt-in inbound logging** for Spring MVC controllers;
- **Outbound logging** for `RestClient`;
- **Centralized configuration**;
- **Integrated anonymization** of headers and configured JSON fields to limit sensitive data exposure.
