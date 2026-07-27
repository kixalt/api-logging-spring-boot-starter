package fr.cgtlabs.springboot.logging.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;

/**
 * Shared HTTP logging utilities.
 * <p>
 * This class centralizes common logging logic for both outbound
 * and inbound HTTP calls: determining loggable content types,
 * extracting a truncated textual body, and anonymizing sensitive headers
 * and fields present in payloads.
 * </p>
 */
public final class HttpLoggingUtils {

    private HttpLoggingUtils() {
        // Utility class
    }

    /**
     * Indicates whether a content type can be logged as text.
     *
     * @param contentType HTTP content type to evaluate
     * @return {@code true} if the content is textual/loggable, {@code false} otherwise
     */
    public static boolean isLoggableContentType(MediaType contentType) {
        return contentType != null && (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)
                        || contentType.isCompatibleWith(MediaType.APPLICATION_XML)
                        || contentType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)
                        || "text".equalsIgnoreCase(contentType.getType()));

    }

    /**
     * Converts a binary body to text, applies anonymization, then truncates the
     * result if the maximum log size is exceeded.
     *
     * @param body HTTP body as bytes
     * @param contentType HTTP content type
     * @param maxBodyLogBytes maximum body size retained for logging
     * @param anonymizeProperties anonymization properties
     * @return converted, anonymized, and potentially truncated body
     */
    public static String extractTextBody(byte[] body, MediaType contentType, int maxBodyLogBytes, AnonymizeProperties anonymizeProperties) {
        boolean truncated = body.length > maxBodyLogBytes;
        int limit = truncated ? maxBodyLogBytes : body.length;

        var charset = contentType != null && contentType.getCharset() != null ? contentType.getCharset() : StandardCharsets.UTF_8;
        var rawBody = new String(body, 0, limit, charset);
        var maskedBody = maskBody(rawBody, anonymizeProperties);

        return truncated ? maskedBody + " [truncated to %d B]".formatted(maxBodyLogBytes ) : maskedBody;
    }

    /**
     * Builds a multi-line textual representation of HTTP headers
     * by applying the configured anonymization.
     *
     * @param headers HTTP headers to log
     * @param anonymizeProperties anonymization properties
     * @return multi-line string representing the headers
     */
    public static List<String> buildHeadersLog(HttpHeaders headers, AnonymizeProperties anonymizeProperties) {
        var toMask = Arrays.stream(anonymizeProperties.getHeaders()).filter(Objects::nonNull).map(String::toLowerCase).collect(Collectors.toSet());

        var anonymizedHeaders = new ArrayList<String>();
        headers.forEach((name, values) -> values.forEach(val -> {
            boolean anonymize = toMask.contains(name.toLowerCase());
            anonymizedHeaders.add(name + ": " + (anonymize ? anonymizeProperties.getAnonymizedString() : val));
        }));

        return anonymizedHeaders;
    }

    /**
     * Masks configured sensitive fields in a textual body.
     *
     * @param body textual body to anonymize
     * @param anonymizeProperties anonymization properties
     * @return anonymized body, or the original value if no masking applies
     */
    public static String maskBody(String body, AnonymizeProperties anonymizeProperties) {
        if (body != null && !body.isBlank()) {
            String[] configuredFields = anonymizeProperties.getBody();
            if (configuredFields != null && configuredFields.length > 0) {
                return buildBodyPattern(configuredFields).matcher(body).replaceAll("$1\"" + anonymizeProperties.getAnonymizedString() + "\"");
            }
        }

        return body;
    }

    /**
     * Attempts to parse a raw {@code Content-Type} header value into
     * a Spring {@link MediaType}.
     *
     * @param contentType raw content type value
     * @return parsed content type, or {@code null} if the value is missing or invalid
     */
    public static MediaType parseMediaType(String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        return null;
    }

    /**
     * Builds the regular expression used to detect fields to mask
     * in logged textual bodies.
     *
     * @param configuredFields names of sensitive fields to target
     * @return compiled regex pattern
     */
    public static Pattern buildBodyPattern(String[] configuredFields) {
        String fields = String.join("|", configuredFields);
        String regex = "(?i)(\"(?:%s)\"\\s*:\\s*)(?:\"[^\"]*\"|\\d+|true|false|null|\\[[^\\]]*\\])".formatted(fields);
        return Pattern.compile(regex);
    }
}