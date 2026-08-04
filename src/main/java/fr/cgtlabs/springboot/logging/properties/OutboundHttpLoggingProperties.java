package fr.cgtlabs.springboot.logging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Min;

/**
 * Configuration properties for outbound HTTP logging.
 */
@ConfigurationProperties(prefix = "logging.outbound")
@Validated
@Getter
@Setter
public class OutboundHttpLoggingProperties implements HttpLoggingProperties {

    /**
     * The maximum size in bytes of the request or response body that will be logged
     * for outbound requests. If the body exceeds this size, it will be truncated.
     */
    @Min(0)
    private int maxBodyLogBytes = 10 * 1024;

    /**
     * Indicates whether the HTTP request body should be logged for outbound requests.
     */
    private boolean logRequestBody = true;

    /**
     * Indicates whether the HTTP response body should be logged for outbound requests.
     */
    private boolean logResponseBody = true;

    /**
     * Indicates whether HTTP headers should be logged for outbound requests.
     */
    private boolean logHeaders = true;
}