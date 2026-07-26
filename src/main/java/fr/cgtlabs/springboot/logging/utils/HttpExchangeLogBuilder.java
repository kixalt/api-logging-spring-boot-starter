package fr.cgtlabs.springboot.logging.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;

/**
 * Builder dedicated to constructing log blocks representing a complete HTTP exchange,
 * whether inbound or outbound.
 * <p>
 * It centralizes the common formatting of sections, fields, and bodies to
 * ensure consistent rendering across HTTP logging components.
 * </p>
 */
public final class HttpExchangeLogBuilder {

    private final String context;

    private final List<String> lines = new ArrayList<>();

    /**
     * Constructs an HTTP log block builder.
     * @param context context displayed in the log (MVC handler, caller name, etc.)
     */
    public HttpExchangeLogBuilder(String context) {
        this.context = context;
    }

    /**
     * Adds a header line for the beginning of the log block.
     *
     * @param headerLine The header line to add.
     * @return current builder
     */
    public HttpExchangeLogBuilder header(String headerLine) {
        lines.add("");
        lines.add(Constants.EXCHANGE_SEPARATOR);
        lines.add(headerLine);
        return this;
    }

    /**
     * Adds a section separator.
     *
     * @return current builder
     */
    public HttpExchangeLogBuilder sectionSeparator() {
        lines.add(Constants.SECTION_SEPARATOR);
        return this;
    }

    /**
     * Adds an empty line.
     *
     * @return current builder
     */
    public HttpExchangeLogBuilder blankLine() {
        lines.add("");
        return this;
    }

    /**
     * Adds a section title to the log block.
     *
     * @param title title to display
     * @return current builder
     */
    public HttpExchangeLogBuilder sectionTitle(String title) {
        lines.add(title);
        return this;
    }

    /**
     * Adds a labeled field/value to the log block.
     *
     * @param label label to display
     * @param value associated value
     * @return current builder
     */
    public HttpExchangeLogBuilder field(String label, Object value) {
        lines.add("  %-8s : %s".formatted(label, value));
        return this;
    }

    /**
     * Adds a loggable textual body to the log block.
     *
     * @param payloadLabel functional label of the payload
     * @param contentType HTTP content type
     * @param body formatted body to display
     * @return current builder
     */
    public HttpExchangeLogBuilder body(String payloadLabel, MediaType contentType, String body) {
        lines.add("[%s] -> Body (%s, type=%s :".formatted(context, payloadLabel, contentType));
        lines.add(body);
        return this;
    }

    /**
     * Adds ignored body information to the log block.
     *
     * @param payloadLabel functional label of the payload
     * @param contentType HTTP content type
     * @param length body size in bytes
     * @return current builder
     */
    public HttpExchangeLogBuilder ignoredBody(String payloadLabel, MediaType contentType, int length) {
        lines.add("[%s] -> Body (Content of %s ignored, type=%s: size=%s bytes)"
                .formatted(context, payloadLabel, contentType != null ? contentType : "unknown", length));
        return this;
    }

    /**
     * Builds the final string representing the complete log block.
     *
     * @return final message ready to be logged
     */
    public String build() {
        return String.join("\n", lines) + "\n" + Constants.EXCHANGE_SEPARATOR;
    }
}