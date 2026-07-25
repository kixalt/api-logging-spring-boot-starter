package fr.cgtlabs.springboot.logging.http.inbound;

import java.io.IOException;
import java.util.Objects;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import fr.cgtlabs.springboot.logging.http.outbound.Direction;
import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.InboundHttpLoggingProperties;
import fr.cgtlabs.springboot.logging.utils.HttpExchangeLogBuilder;
import fr.cgtlabs.springboot.logging.utils.HttpLoggingUtils;

/**
 * Filtre de logging HTTP entrant.
 * <p>
 * Le filtre ne s'applique qu'aux paths inclus dans la configuration. Il wrappe la requête et la
 * réponse afin de permettre la lecture des corps, puis ne journalise l'échange que si
 * l'interceptor MVC a explicitement marqué la requête comme loggable via
 * {@link LoggedRestEndpoint}.
 * </p>
 */
public class InboundHttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(InboundHttpLoggingFilter.class);

    private final InboundHttpLoggingProperties properties;

    private final AnonymizeProperties anonymizeProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Construit le filtre de logging HTTP entrant.
     *
     * @param properties          propriétés de pilotage du logging entrant
     * @param anonymizeProperties propriétés de masquage des données sensibles
     */
    public InboundHttpLoggingFilter(InboundHttpLoggingProperties properties, AnonymizeProperties anonymizeProperties) {
        this.properties = properties;
        this.anonymizeProperties = anonymizeProperties;
    }

    /**
     * Détermine si la requête doit être exclue du filtre selon l'activation globale
     * de la fonctionnalité et la liste des paths inclus configurés.
     *
     * @param request requête HTTP entrante
     * @return {@code true} si le filtre ne doit pas s'appliquer, {@code false} sinon
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        var includedPaths = properties.getIncludedPaths();
        if (properties.isEnabled() && !CollectionUtils.isEmpty(includedPaths)) {
            var path = resolvePathWithinApplication(request);
            return includedPaths.stream().filter(Objects::nonNull).noneMatch(pattern -> pathMatcher.match(pattern, path));
        }

        return true;
    }

    /**
     * Wrappe la requête et la réponse afin de mettre en cache leurs corps, exécute
     * la chaîne de filtres puis déclenche le logging final si l'interceptor MVC a
     * explicitement marqué la requête comme loggable.
     * <p>
     * Le corps de la réponse est systématiquement recopié vers la réponse d'origine
     * via {@link ContentCachingResponseWrapper#copyBodyToResponse()}.
     * </p>
     *
     * @param request     requête HTTP entrante
     * @param response    réponse HTTP sortante
     * @param filterChain chaîne de filtres servlet
     * @throws ServletException en cas d'erreur servlet
     * @throws IOException      en cas d'erreur d'entrée/sortie
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper cachingRequest ? cachingRequest : new ContentCachingRequestWrapper(request, properties.getMaxBodyLogBytes());
        ContentCachingResponseWrapper wrappedResponse = response instanceof ContentCachingResponseWrapper cachingResponse ? cachingResponse : new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logAndCopyResponse(wrappedRequest, wrappedResponse);
        }
    }

    /**
     * Déclenche le logging final de l'échange si la requête a été marquée comme
     * loggable, puis recopie systématiquement le corps de la réponse vers la réponse
     * d'origine.
     *
     * @param request  requête HTTP wrappée
     * @param response réponse HTTP wrappée
     * @throws IOException en cas d'erreur lors de la recopie de la réponse
     */
    private void logAndCopyResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) throws IOException {
        try {
            if (Boolean.TRUE.equals(request.getAttribute(InboundHttpLoggingAttributes.LOGGING_ENABLED))) {
                logExchange(request, response);
            }
        } finally {
            response.copyBodyToResponse();
        }
    }

    private String resolvePathWithinApplication(HttpServletRequest request) {
        var requestUri = request.getRequestURI();
        var contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    /**
     * Journalise l'échange HTTP complet lorsque le niveau INFO est actif.
     *
     * @param request  requête HTTP wrappée avec corps mis en cache
     * @param response réponse HTTP wrappée avec corps mis en cache
     */
    private void logExchange(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        if (LOG.isInfoEnabled()) {
            var handlerSignature = String.valueOf(request.getAttribute(InboundHttpLoggingAttributes.HANDLER_SIGNATURE));
            long elapsed = computeElapsed(request);
            String message = buildExchangeLog(request, response, handlerSignature, elapsed);
            LOG.info(message);
        }
    }

    /**
     * Calcule la durée de traitement de la requête à partir de l'attribut technique
     * posé par l'interceptor MVC.
     *
     * @param request requête HTTP courante
     * @return durée écoulée en millisecondes, ou {@code -1} si l'instant de départ
     * est absent
     */
    private long computeElapsed(HttpServletRequest request) {
        Object startTime = request.getAttribute(InboundHttpLoggingAttributes.START_TIME);
        if (startTime instanceof Long start) {
            return System.currentTimeMillis() - start;
        }
        return -1L;
    }

    /**
     * Construit le log complet de l'échange HTTP entrant sous forme d'un bloc
     * unique regroupant la requête, la réponse et leurs éventuels bodies.
     *
     * @param request          requête HTTP wrappée
     * @param response         réponse HTTP wrappée
     * @param handlerSignature signature du handler MVC ciblé
     * @param elapsed          durée d'exécution en millisecondes
     * @return message de log complet prêt à être journalisé
     */
    private String buildExchangeLog(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, String handlerSignature, long elapsed) {
        String uri = buildRequestUri(request);
        var builder = new HttpExchangeLogBuilder(handlerSignature)
                .start(Direction.INBOUND)
                .sectionSeparator();

        appendRequest(builder, request, handlerSignature, uri);
        builder.blankLine().sectionSeparator();
        appendResponse(builder, response, handlerSignature, elapsed, uri);

        return builder.build();
    }

    /**
     * Ajoute au builder les métadonnées de la requête entrante puis,
     * si activé, son corps lorsque celui-ci est présent.
     *
     * @param builder          builder de bloc de log HTTP
     * @param request          requête HTTP wrappée
     * @param handlerSignature signature du handler MVC ciblé
     * @param uri              URI complète de la requête à journaliser
     */
    private void appendRequest(HttpExchangeLogBuilder builder, ContentCachingRequestWrapper request, String handlerSignature, String uri) {
        builder.sectionTitle("→ Requête entrante")
                .field("Handler", handlerSignature)
                .field("Méthode", request.getMethod())
                .field("URI", uri);
        appendHeader(builder, request);
        appendRequestBody(builder, request);
    }

    private void appendHeader(HttpExchangeLogBuilder builder, ContentCachingRequestWrapper request) {
        if (properties.isLogHeaders()) {
            var headersLines = HttpLoggingUtils.buildHeadersLog(request, anonymizeProperties);
            builder.field("Headers", headersLines.isEmpty() ? "[aucun]" : headersLines.getFirst());
            headersLines.stream()
                    .skip(1)
                    .forEach(value -> builder.field("      ", value));
        } else {
            builder.field("Headers", "[désactivé]");
        }
    }

    /**
     * Ajoute au builder les métadonnées de la réponse HTTP produite puis,
     * si activé, son corps lorsque celui-ci est présent.
     *
     * @param builder          builder de bloc de log HTTP
     * @param response         réponse HTTP wrappée
     * @param handlerSignature signature du handler MVC ciblé
     * @param elapsed          durée d'exécution en millisecondes
     * @param uri              URI complète de la requête associée
     */
    private void appendResponse(HttpExchangeLogBuilder builder, ContentCachingResponseWrapper response, String handlerSignature, long elapsed, String uri) {
        builder.sectionTitle("← Réponse envoyée")
                .field("Handler", handlerSignature)
                .field("Statut", response.getStatus())
                .field("Durée", elapsed + " ms")
                .field("URI", uri);

        appendResponseBody(builder, response);
    }

    /**
     * Ajoute au builder le body de la requête si l'option correspondante est
     * activée et si le contenu peut être exploité pour le logging.
     *
     * @param builder builder de bloc de log HTTP
     * @param request requête HTTP wrappée
     */
    private void appendRequestBody(HttpExchangeLogBuilder builder, ContentCachingRequestWrapper request) {
        if (properties.isLogRequestBody()) {
            byte[] requestBody = request.getContentAsByteArray();
            if (requestBody.length > 0) {
                MediaType contentType = HttpLoggingUtils.parseMediaType(request.getContentType());
                appendBody(builder, "requête", requestBody, contentType);
            }
        }
    }

    /**
     * Ajoute au builder le body de la réponse si l'option correspondante est
     * activée et si le contenu peut être exploité pour le logging.
     *
     * @param builder  builder de bloc de log HTTP
     * @param response réponse HTTP wrappée
     */
    private void appendResponseBody(HttpExchangeLogBuilder builder, ContentCachingResponseWrapper response) {
        if (properties.isLogResponseBody()) {
            byte[] responseBody = response.getContentAsByteArray();
            if (responseBody.length > 0) {
                MediaType contentType = HttpLoggingUtils.parseMediaType(response.getContentType());
                appendBody(builder, "réponse", responseBody, contentType);
            }
        }
    }

    /**
     * Ajoute au builder un body HTTP textuel si son type de contenu est loggable ;
     * sinon, ajoute uniquement son type et sa taille.
     *
     * @param builder      builder de bloc de log HTTP
     * @param payloadLabel libellé fonctionnel du payload journalisé
     * @param body         contenu brut du body
     * @param contentType  type de contenu HTTP
     */
    private void appendBody(HttpExchangeLogBuilder builder, String payloadLabel, byte[] body, MediaType contentType) {
        if (HttpLoggingUtils.isLoggableContentType(contentType)) {
            var formattedBody = HttpLoggingUtils.extractTextBody(body, contentType, properties.getMaxBodyLogBytes(), anonymizeProperties);
            builder.body(payloadLabel, contentType, formattedBody);
        } else {
            builder.ignoredBody(payloadLabel, contentType, body.length);
        }
    }

    /**
     * Construit l'URI complète de la requête à logger, incluant la query string si
     * elle est présente.
     *
     * @param request requête HTTP courante
     * @return URI éventuellement enrichie de la query string
     */
    private String buildRequestUri(HttpServletRequest request) {
        var queryString = request.getQueryString();
        return !StringUtils.hasText(queryString) ? request.getRequestURI() : request.getRequestURI() + "?" + queryString;
    }

}
