package fr.cgtlabs.springboot.logging.properties;

/**
 * Common interface for HTTP logging configuration properties.
 * It defines methods to access logging parameters shared
 * between inbound and outbound request logging.
 */
public interface HttpLoggingProperties {

    /**
     * Indicates whether HTTP headers should be logged.
     *
     * @return {@code true} if headers should be logged, {@code false} otherwise.
     */
    boolean isLogHeaders();

    /**
     * Indicates whether the HTTP request body should be logged.
     *
     * @return {@code true} if the request body should be logged, {@code false} otherwise.
     */
    boolean isLogRequestBody();

    /**
     * Indicates whether the HTTP response body should be logged.
     *
     * @return {@code true} if the response body should be logged, {@code false} otherwise.
     */
    boolean isLogResponseBody();

    /**
     * Returns the maximum size in bytes of the request or response body
     * that will be logged. If the body exceeds this size, it will be truncated.
     *
     * @return The maximum size of the body to log in bytes.
     */
    int getMaxBodyLogBytes();
}