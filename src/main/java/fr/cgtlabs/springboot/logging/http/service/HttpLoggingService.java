package fr.cgtlabs.springboot.logging.http.service;

import fr.cgtlabs.springboot.logging.http.common.HttpExchangeInfoProvider;
import fr.cgtlabs.springboot.logging.http.common.Direction;
import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.HttpLoggingProperties;
import fr.cgtlabs.springboot.logging.utils.HttpExchangeLogBuilder;
import fr.cgtlabs.springboot.logging.utils.HttpLoggingUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * HTTP logging service that centralizes the logic for building log messages
 * for both inbound and outbound HTTP exchanges.
 * It uses an {@link HttpExchangeInfoProvider} to obtain exchange details
 * and {@link HttpLoggingProperties} to determine what should be logged.
 */
public class HttpLoggingService {

    private final HttpLoggingProperties properties;
    private final AnonymizeProperties anonymizeProperties;

    /**
     * Constructs a new instance of {@code HttpLoggingService}.
     *
     * @param properties          The configuration properties for HTTP logging.
     * @param anonymizeProperties The configuration properties for anonymizing sensitive data in logs.
     */
    public HttpLoggingService(HttpLoggingProperties properties, AnonymizeProperties anonymizeProperties) {
        this.properties = properties;
        this.anonymizeProperties = anonymizeProperties;
    }

    /**
     * Builds the complete log message for an HTTP exchange.
     * This message is formatted as a single block, grouping request and response details,
     * and their respective bodies if available and configured.
     *
     * @param infoProvider The HTTP exchange information provider.
     * @return The complete log message, ready to be logged.
     * @throws IOException If an I/O error occurs while reading the bodies.
     */
    public String buildExchangeLog(HttpExchangeInfoProvider infoProvider) throws IOException {
        var builder = new HttpExchangeLogBuilder(infoProvider.getExchangeDescriptor())
                .header("[%s] HTTP %s".formatted(infoProvider.getExchangeDescriptor(), infoProvider.getDirection()))
                .sectionSeparator();

        appendRequest(builder, infoProvider);
        builder.blankLine().sectionSeparator();
        appendResponse(builder, infoProvider);

        return builder.build();
    }

    /**
     * Appends HTTP request metadata to the log builder.
     * If request body logging is enabled and the body is present, it is also appended.
     *
     * @param builder      The HTTP log block builder.
     * @param infoProvider The HTTP exchange information provider.
     */
    private void appendRequest(HttpExchangeLogBuilder builder, HttpExchangeInfoProvider infoProvider) {
        builder.sectionTitle("[%s] → %s Request".formatted(infoProvider.getExchangeDescriptor(), infoProvider.getDirection() == Direction.INBOUND ? "Inbound" : "Outbound"))
                .field(infoProvider.getDirection() == Direction.INBOUND ? "Handler" : "Caller", infoProvider.getExchangeDescriptor())
                .field("Method", infoProvider.getHttpMethod())
                .field("URI", infoProvider.getUri());
        appendHeader(builder, infoProvider.getRequestHeaders());
        appendRequestBody(builder, infoProvider.getRequestBody(), infoProvider.getRequestContentType());
    }

    /**
     * Appends HTTP headers to the log builder.
     * If header logging is enabled, headers are formatted and potentially anonymized before being added.
     * Otherwise, an indication that headers are disabled is added.
     *
     * @param builder The HTTP log block builder.
     * @param headers The HTTP headers.
     */
    private void appendHeader(HttpExchangeLogBuilder builder, HttpHeaders headers) {
        if (properties.isLogHeaders()) {
            var headersLines = HttpLoggingUtils.buildHeadersLog(headers, anonymizeProperties);
            builder.field("Headers", headersLines.isEmpty() ? "[none]" : headersLines.getFirst());
            headersLines.stream()
                    .skip(1)
                    .forEach(value -> builder.field("      ", value));
        } else {
            builder.field("Headers", "[disabled]");
        }
    }

    /**
     * Appends HTTP response metadata to the log builder.
     * If response body logging is enabled and the body is present, it is also appended.
     *
     * @param builder      The HTTP log block builder.
     * @param infoProvider The HTTP exchange information provider.
     * @throws IOException If an I/O error occurs while reading the response body.
     */
    private void appendResponse(HttpExchangeLogBuilder builder, HttpExchangeInfoProvider infoProvider) throws IOException {
        builder.sectionTitle("[%s] ← Response %s".formatted(infoProvider.getExchangeDescriptor(), infoProvider.getDirection() == Direction.INBOUND ? "Sent" : "Received"))
                .field(infoProvider.getDirection() == Direction.INBOUND ? "Handler" : "Caller", infoProvider.getExchangeDescriptor())
                .field("Status", infoProvider.getResponseStatus())
                .field("Duration", infoProvider.getElapsedTime() + " ms")
                .field("URI", infoProvider.getUri());
        appendHeader(builder, infoProvider.getResponseHeaders());
        appendResponseBody(builder, infoProvider.getResponseBody(), infoProvider.getResponseContentType());
    }

    /**
     * Appends the HTTP request body to the log builder, if request body logging
     * is enabled and the request body is not empty.
     *
     * @param builder     The HTTP log block builder.
     * @param requestBody The request body.
     * @param contentType The content type of the request.
     */
    private void appendRequestBody(HttpExchangeLogBuilder builder, byte[] requestBody, MediaType contentType) {
        if (properties.isLogRequestBody() && requestBody != null && requestBody.length > 0) {
            appendBody(builder, "request", requestBody, contentType);
        }
    }

    /**
     * Appends the HTTP response body to the log builder, if response body logging
     * is enabled and the response body is not empty.
     *
     * @param builder     The HTTP log block builder.
     * @param responseBody The response body.
     * @param contentType The content type of the response.
     */
    private void appendResponseBody(HttpExchangeLogBuilder builder, byte[] responseBody, MediaType contentType) {
        if (properties.isLogResponseBody() && responseBody != null && responseBody.length > 0) {
            appendBody(builder, "response", responseBody, contentType);
        }
    }

    /**
     * Appends an HTTP message body (request or response) to the log builder.
     * If the content type is loggable (e.g., text, JSON, XML), the content is extracted,
     * formatted, and potentially anonymized before being added.
     * Otherwise, only the content type and body size are logged.
     *
     * @param builder      The HTTP log block builder.
     * @param payloadLabel The functional label of the logged payload (e.g., "request", "response").
     * @param body         The raw content of the HTTP body as a byte array.
     * @param contentType  The HTTP content type (e.g., "application/json", "text/plain").
     */
    private void appendBody(HttpExchangeLogBuilder builder, String payloadLabel, byte[] body, MediaType contentType) {
        if (HttpLoggingUtils.isLoggableContentType(contentType)) {
            var formattedBody = HttpLoggingUtils.extractTextBody(body, contentType, properties.getMaxBodyLogBytes(), anonymizeProperties);
            builder.body(payloadLabel, contentType, formattedBody);
        } else {
            builder.ignoredBody(payloadLabel, contentType, body.length);
        }
    }
}