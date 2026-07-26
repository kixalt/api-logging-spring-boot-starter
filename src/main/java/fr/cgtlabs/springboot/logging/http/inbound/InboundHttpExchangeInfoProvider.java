package fr.cgtlabs.springboot.logging.http.inbound;

import fr.cgtlabs.springboot.logging.http.common.Direction;
import fr.cgtlabs.springboot.logging.http.common.HttpExchangeInfoProvider;
import fr.cgtlabs.springboot.logging.utils.HttpLoggingUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Implémentation de {@link HttpExchangeInfoProvider} pour les requêtes HTTP entrantes.
 * Cette classe extrait les informations nécessaires à la journalisation à partir
 * des objets {@link ContentCachingRequestWrapper} et {@link ContentCachingResponseWrapper}.
 */
public class InboundHttpExchangeInfoProvider implements HttpExchangeInfoProvider {

    private final ContentCachingRequestWrapper request;
    private final ContentCachingResponseWrapper response;
    private final String handlerSignature;

    /**
     * Construit une nouvelle instance de {@code InboundHttpExchangeInfoProvider}.
     *
     * @param request          La requête HTTP entrante enveloppée.
     * @param response         La réponse HTTP sortante enveloppée.
     * @param handlerSignature La signature du handler MVC ciblé.
     */
    public InboundHttpExchangeInfoProvider(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, String handlerSignature) {
        this.request = request;
        this.response = response;
        this.handlerSignature = handlerSignature;
    }

    @Override
    public Direction getDirection() {
        return Direction.INBOUND;
    }

    @Override
    public String getExchangeDescriptor() {
        return handlerSignature;
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public URI getUri() {
        try {
            return new URI(buildRequestUri(request));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.addAll(headerName, Collections.list(request.getHeaders(headerName)));
        }
        return headers;
    }

    @Override
    public byte[] getRequestBody() {
        return request.getContentAsByteArray();
    }

    @Override
    public int getResponseStatus() {
        return response.getStatus();
    }

    @Override
    public HttpHeaders getResponseHeaders() {
        var headers = new HttpHeaders();
        response.getHeaderNames().forEach(headerName -> headers.addAll(headerName, List.copyOf(response.getHeaders(headerName))));
        return headers;
    }

    @Override
    public byte[] getResponseBody() {
        return response.getContentAsByteArray();
    }

    @Override
    public long getElapsedTime() {
        return computeElapsed(request);
    }

    @Override
    public MediaType getRequestContentType() {
        return HttpLoggingUtils.parseMediaType(request.getContentType());
    }

    @Override
    public MediaType getResponseContentType() {
        return HttpLoggingUtils.parseMediaType(response.getContentType());
    }

    /**
     * Calcule la durée de traitement de la requête en millisecondes.
     * Cette durée est calculée en soustrayant l'instant de début du traitement
     * (stocké dans l'attribut de requête {@link InboundHttpLoggingAttributes#START_TIME})
     * de l'heure actuelle.
     *
     * @param request La requête HTTP courante.
     * @return La durée écoulée en millisecondes, ou {@code -1L} si l'attribut de temps de début est absent ou invalide.
     */
    private long computeElapsed(HttpServletRequest request) {
        Object startTime = request.getAttribute(InboundHttpLoggingAttributes.START_TIME);
        if (startTime instanceof Long start) {
            return System.currentTimeMillis() - start;
        }
        return -1L;
    }

    /**
     * Construit l'URI complète de la requête HTTP, incluant la chaîne de requête (query string)
     * si elle est présente.
     *
     * @param request La requête HTTP courante.
     * @return L'URI complète de la requête, potentiellement enrichie de la query string.
     */
    private String buildRequestUri(HttpServletRequest request) {
        var queryString = request.getQueryString();
        return !StringUtils.hasText(queryString) ? request.getRequestURI() : request.getRequestURI() + "?" + queryString;
    }
}
