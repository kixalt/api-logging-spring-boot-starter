package fr.cgtlabs.springboot.logging.http.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;

/**
 * Interface for providing information about an HTTP exchange (request/response)
 * in an agnostic way, whether it is inbound or outbound.
 */
public interface HttpExchangeInfoProvider {

    /**
     * Returns the direction of the HTTP exchange (INBOUND or OUTBOUND).
     * @return The direction of the exchange.
     */
    Direction getDirection();

    /**
     * Returns a descriptor of the exchange, which can be the handler's signature
     * for an inbound request or the caller's name for an outbound request.
     * @return The exchange descriptor.
     */
    String getExchangeDescriptor();

    /**
     * Returns the HTTP method of the request.
     * @return The HTTP method.
     */
    HttpMethod getHttpMethod();

    /**
     * Returns the URI of the request.
     * @return The URI.
     */
    URI getUri();

    /**
     * Returns the request headers.
     * @return The request headers.
     */
    HttpHeaders getRequestHeaders();

    /**
     * Returns the request body as a byte array.
     * @return The request body.
     */
    byte[] getRequestBody();

    /**
     * Returns the HTTP status code of the response.
     * @return The status code.
     */
    int getResponseStatus();

    /**
     * Returns the response headers.
     * @return The response headers.
     */
    HttpHeaders getResponseHeaders();

    /**
     * Returns the response body as a byte array.
     * @return The response body.
     * @throws IOException If an error occurs while reading the body.
     */
    byte[] getResponseBody() throws IOException;

    /**
     * Returns the elapsed time for the exchange in milliseconds.
     * @return The elapsed time.
     */
    long getElapsedTime();

    /**
     * Returns the content type of the request.
     * @return The content type of the request.
     */
    MediaType getRequestContentType();

    /**
     * Returns the content type of the response.
     * @return The content type of the response.
     */
    MediaType getResponseContentType();
}