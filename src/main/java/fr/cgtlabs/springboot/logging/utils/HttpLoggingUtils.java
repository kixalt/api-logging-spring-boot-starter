package fr.cgtlabs.springboot.logging.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import dev.blaauwendraad.masker.json.JsonMasker;
import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;

/**
 * Shared HTTP logging utilities.
 * <p>
 * This class centralizes common logging logic for both outbound
 * and inbound HTTP calls: determining loggable content types,
 * extracting textual bodies for logging, anonymizing sensitive headers,
 * and applying a preconfigured JSON masker when available.
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
     * Converts a body to text after applying content-aware masking and truncation.
     * <p>
     * When the content type is compatible with {@code application/json}, the body is
     * masked with the provided preconfigured {@link JsonMasker}. This masker is expected
     * to have been built once by the caller using the configured sensitive field names
     * and replacement rules. For other textual content types, the body is left unchanged
     * and only truncated if it exceeds the configured limit.
     * </p>
     *
     * @param body            HTTP body as bytes
     * @param contentType     HTTP content type
     * @param maxBodyLogBytes maximum body size retained for logging
     * @param masker          preconfigured JSON masker, or {@code null} if JSON masking is disabled
     * @return masked and potentially truncated body as text
     */
    public static String getMaskedAndTruncatedBody(byte[] body, MediaType contentType, int maxBodyLogBytes, JsonMasker masker) {
        var charset = contentType.getCharset() != null ? contentType.getCharset() : StandardCharsets.UTF_8;
        var formattedBody = body;
        if (null != masker && contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            formattedBody = masker.mask(body);
        }
        boolean truncated = formattedBody.length > maxBodyLogBytes;
        int limit = truncated ? maxBodyLogBytes : formattedBody.length;
        return new String(formattedBody, 0, limit, charset);
    }

    /**
     * Builds a multi-line textual representation of HTTP headers
     * by applying the configured anonymization.
     *
     * @param headers             HTTP headers to log
     * @param anonymizeProperties anonymization properties
     * @return multi-line string representing the headers
     */
    public static List<String> buildHeadersLog(HttpHeaders headers, AnonymizeProperties anonymizeProperties) {
        var toMask = Arrays.stream(anonymizeProperties.getHeaders())
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        var anonymizedHeaders = new ArrayList<String>();
        headers.forEach((name, values) -> values.forEach(value -> {
            boolean anonymize = toMask.contains(name.toLowerCase());
            anonymizedHeaders.add(name + ": " + (anonymize ? anonymizeProperties.getAnonymizedString() : value));
        }));
        return anonymizedHeaders;
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
            } catch (IllegalArgumentException _) {
                return null;
            }
        }
        return null;
    }

}