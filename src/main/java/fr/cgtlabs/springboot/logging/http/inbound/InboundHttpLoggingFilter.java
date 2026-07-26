package fr.cgtlabs.springboot.logging.http.inbound;

import java.io.IOException;
import java.util.Objects;

import fr.cgtlabs.springboot.logging.http.common.HttpExchangeInfoProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.InboundHttpLoggingProperties;
import fr.cgtlabs.springboot.logging.http.service.HttpLoggingService;

/**
 * Inbound HTTP logging filter.
 * <p>
 * This filter intercepts inbound HTTP requests to log their details.
 * It only applies to paths configured via {@link InboundHttpLoggingProperties#getIncludedPaths()}.
 * To allow reading request and response bodies, it wraps the
 * {@link HttpServletRequest} and {@link HttpServletResponse} objects with "caching" versions
 * ({@link ContentCachingRequestWrapper} and {@link ContentCachingResponseWrapper}).
 * Full logging of the HTTP exchange (request and response) is only performed
 * if the target endpoint has been explicitly marked as loggable
 * by an MVC interceptor, typically via an annotation like
 * {@code @LoggedRestEndpoint} (although the annotation is not directly referenced here,
 * the mechanism is implicit via {@link InboundHttpLoggingAttributes#LOGGING_ENABLED}).
 * </p>
 */
public class InboundHttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(InboundHttpLoggingFilter.class);

    private final InboundHttpLoggingProperties properties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final HttpLoggingService httpLoggingService;

    /**
     * Constructs a new instance of the inbound HTTP logging filter.
     *
     * @param properties          The configuration properties for inbound HTTP logging.
     * @param anonymizeProperties The configuration properties for anonymizing sensitive data in logs.
     */
    public InboundHttpLoggingFilter(InboundHttpLoggingProperties properties, AnonymizeProperties anonymizeProperties) {
        this.properties = properties;
        this.httpLoggingService = new HttpLoggingService(properties, anonymizeProperties);
    }

    /**
     * Determines whether this filter should be applied to the given HTTP request.
     * The filter is excluded if the logging feature is disabled,
     * or if the list of included paths is empty, or if the request path
     * does not match any of the patterns configured in {@link InboundHttpLoggingProperties#getIncludedPaths()}.
     *
     * @param request The incoming HTTP request.
     * @return {@code true} if the filter should NOT apply to this request, {@code false} if it SHOULD apply.
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
     * Intercepts the request and response to allow logging.
     * This method wraps the request and response with "caching" versions
     * to make their bodies readable multiple times. It then executes the
     * filter chain and, if the request has been marked as loggable
     * (e.g., by an MVC interceptor), it triggers the full logging
     * of the HTTP exchange.
     * <p>
     * It is crucial that the response body is systematically copied
     * to the original response via {@link ContentCachingResponseWrapper#copyBodyToResponse()}
     * in the {@code finally} block to ensure the client receives the response.
     * </p>
     *
     * @param request     The incoming HTTP request.
     * @param response    The outgoing HTTP response.
     * @param filterChain The servlet filter chain to execute.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException      If an input/output error occurs.
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
     * Triggers the logging of the HTTP exchange if the request has been marked as
     * loggable (via {@link InboundHttpLoggingAttributes#LOGGING_ENABLED}).
     * Regardless of logging, this method ensures that the response body
     * is copied to the original HTTP response to be sent to the client.
     *
     * @param request  The wrapped (caching) HTTP request.
     * @param response The wrapped (caching) HTTP response.
     * @throws IOException If an input/output error occurs while copying the response body.
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

    /**
     * Resolves the request path within the application, removing the context path
     * if it is present in the request URI.
     *
     * @param request The incoming HTTP request.
     * @return The request path relative to the application.
     */
    private String resolvePathWithinApplication(HttpServletRequest request) {
        var requestUri = request.getRequestURI();
        var contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    /**
     * Logs the complete HTTP exchange (request and response) if the INFO logging level
     * is enabled. This method constructs a detailed log message including
     * request and response metadata, as well as their bodies if configured.
     *
     * @param request  The HTTP request wrapped with the cached body.
     * @param response The HTTP response wrapped with the cached body.
     */
    private void logExchange(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) throws IOException {
        if (LOG.isInfoEnabled()) {
            var handlerSignature = String.valueOf(request.getAttribute(InboundHttpLoggingAttributes.HANDLER_SIGNATURE));
            var infoProvider = new InboundHttpExchangeInfoProvider(request, response, handlerSignature);
            LOG.info(httpLoggingService.buildExchangeLog(infoProvider));
        }
    }


}