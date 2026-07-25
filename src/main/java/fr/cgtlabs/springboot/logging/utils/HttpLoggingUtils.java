package fr.cgtlabs.springboot.logging.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;

/**
 * Utilitaires partagés de logging HTTP.
 * <p>
 * Cette classe factorise la logique commune de journalisation entre les appels
 * HTTP sortants et les appels HTTP entrants : détermination des types de contenu
 * loggables, extraction d'un body textuel tronqué, anonymisation des headers et
 * des champs sensibles présents dans les payloads.
 * </p>
 */
public final class HttpLoggingUtils {

    private HttpLoggingUtils() {
        // Utility class
    }

    /**
     * Indique si un type de contenu peut être journalisé comme texte.
     *
     * @param contentType type de contenu HTTP à évaluer
     * @return {@code true} si le contenu est textuel/loggable, {@code false} sinon
     */
    public static boolean isLoggableContentType(MediaType contentType) {
        return contentType != null && (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)
                        || contentType.isCompatibleWith(MediaType.APPLICATION_XML)
                        || contentType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)
                        || "text".equalsIgnoreCase(contentType.getType()));

    }

    /**
     * Convertit un corps binaire en texte, applique l'anonymisation puis tronque le
     * résultat si la taille maximale de log est dépassée.
     *
     * @param body corps HTTP sous forme d'octets
     * @param contentType type de contenu HTTP
     * @param maxBodyLogBytes taille maximale de body conservée pour le logging
     * @param anonymizeProperties propriétés d'anonymisation
     * @return corps converti, anonymisé et éventuellement tronqué
     */
    public static String extractTextBody(byte[] body, MediaType contentType, int maxBodyLogBytes, AnonymizeProperties anonymizeProperties) {
        boolean truncated = body.length > maxBodyLogBytes;
        int limit = truncated ? maxBodyLogBytes : body.length;

        var charset = contentType != null && contentType.getCharset() != null ? contentType.getCharset() : StandardCharsets.UTF_8;
        var rawBody = new String(body, 0, limit, charset);
        var maskedBody = maskBody(rawBody, anonymizeProperties);

        return truncated ? maskedBody + " [tronqué à %d Ko]".formatted(maxBodyLogBytes / 1024) : maskedBody;
    }

    /**
     * Construit une représentation textuelle multiligne des en-têtes d'une requête
     * HTTP Spring en appliquant l'anonymisation configurée.
     *
     * @param request requête HTTP Spring contenant les en-têtes à journaliser
     * @param anonymizeProperties propriétés d'anonymisation
     * @return chaîne multiligne représentant les en-têtes
     */
    public static String buildHeadersLog(HttpRequest request, AnonymizeProperties anonymizeProperties) {
        var toMask = Arrays.stream(anonymizeProperties.getHeaders()).filter(Objects::nonNull).map(String::toLowerCase).toList();

        var anonymizedHeaders = new ArrayList<String>();
        request.getHeaders().forEach((name, values) -> values.forEach(val -> {
            boolean anonymize = toMask.contains(name.toLowerCase());
            anonymizedHeaders.add(name + ": " + (anonymize ? anonymizeProperties.getAnonymizedString() : val));
        }));

        return String.join("\n", anonymizedHeaders);
    }

    /**
     * Construit une représentation textuelle multiligne des en-têtes d'une requête
     * servlet en appliquant l'anonymisation configurée.
     *
     * @param request requête servlet contenant les en-têtes à journaliser
     * @param anonymizeProperties propriétés d'anonymisation
     * @return chaîne multiligne représentant les en-têtes, ou un message indiquant
     *         qu'aucun en-tête n'est présent
     */
    public static List<String> buildHeadersLog(HttpServletRequest request, AnonymizeProperties anonymizeProperties) {
        var toMask = Arrays.stream(anonymizeProperties.getHeaders()).filter(Objects::nonNull).map(String::toLowerCase).toList();

        List<String> anonymizedHeaders = new ArrayList<>();
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            var headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                boolean anonymize = toMask.contains(headerName.toLowerCase());
                anonymizedHeaders.add(headerName + ": " + (anonymize ? anonymizeProperties.getAnonymizedString() : headerValue));
            }
        }

        return anonymizedHeaders;
    }

    /**
     * Masque les champs sensibles configurés dans un corps textuel.
     *
     * @param body corps textuel à anonymiser
     * @param anonymizeProperties propriétés d'anonymisation
     * @return corps anonymisé, ou la valeur d'origine si aucun masquage ne s'applique
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
     * Tente de parser une valeur brute d'en-tête {@code Content-Type} en
     * {@link MediaType} Spring.
     *
     * @param contentType valeur brute du type de contenu
     * @return type de contenu parsé, ou {@code null} si la valeur est absente ou invalide
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
     * Construit l'expression régulière utilisée pour détecter les champs à masquer
     * dans les corps textuels journalisés.
     *
     * @param configuredFields noms de champs sensibles à cibler
     * @return motif regex compilé
     */
    public static Pattern buildBodyPattern(String[] configuredFields) {
        String fields = String.join("|", configuredFields);
        String regex = "(?i)(\"(?:%s)\")\\s*:\\s*(?:\"[^\"]*\"|\\d+|true|false|null|\\[[^\\]]*\\])".formatted(fields);
        return Pattern.compile(regex);
    }
}
