package fr.cgtlabs.springboot.logging.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for inbound HTTP logging.
 */
@ConfigurationProperties(prefix = "logging.inbound")
@Getter
@Setter
public final class InboundHttpLoggingProperties implements HttpLoggingProperties {

    /**
     * Enables or disables the inbound HTTP logging feature globally.
     */
    private boolean enabled = false;

    /**
     * Indicates whether HTTP headers should be logged for inbound requests.
     */
    private boolean logHeaders = true;

    /**
     * Indicates whether the HTTP request body should be logged for inbound requests.
     */
    private boolean logRequestBody = true;

    /**
     * Indicates whether the HTTP response body should be logged for inbound requests.
     */
    private boolean logResponseBody = true;

    /**
     * The maximum size in bytes of the request or response body that will be logged
     * for inbound requests. If the body exceeds this size, it will be truncated.
     */
    private int maxBodyLogBytes = 10 * 1024;

    /**
     * List of HTTP paths included in the technical scope of the inbound logging filter.
     * <p>
     * Patterns are intended to be evaluated with Spring/Ant-style semantics, e.g.,
     * {@code /api/**}.
     * </p>
     */
    private List<String> includedPaths = new ArrayList<>();
}