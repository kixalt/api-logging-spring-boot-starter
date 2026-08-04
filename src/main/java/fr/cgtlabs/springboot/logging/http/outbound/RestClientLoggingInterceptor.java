package fr.cgtlabs.springboot.logging.http.outbound;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import fr.cgtlabs.springboot.logging.http.service.HttpLoggingService;
import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.OutboundHttpLoggingProperties;

/**
 * Interceptor for logging outbound HTTP calls made via {@code RestClient}.
 * <p>
 * This component logs outbound HTTP requests and responses, associating them
 * with an explicit calling context ({@code callerName}) to identify the
 * business or technical component originating the call.
 * </p>
 * <p>
 * It supports:
 * </p>
 * <ul>
 *   <li>logging of request metadata: method, URI, headers;</li>
 *   <li>logging of response metadata: HTTP status, duration, URI;</li>
 *   <li>optional logging of request and response bodies;</li>
 *   <li>anonymization of sensitive headers;</li>
 *   <li>anonymization of sensitive fields in textual payloads;</li>
 *   <li>size limitation of logged bodies;</li>
 *   <li>buffering of the HTTP response body so it can be logged without
 *       preventing downstream consumers from reading it.</li>
 * </ul>
 * <p>
 * This class is not intended to be instantiated directly from outside
 * the package; it is designed to be created via a dedicated factory to
 * ensure explicit provision of the calling context.
 * </p>
 */
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientLoggingInterceptor.class);

    private final String callerName;

    private final HttpLoggingService httpLoggingService;

    /**
     * Constructs an outbound HTTP logging interceptor associated with an explicit caller.
     *
     * @param callerName            logical name of the component originating the HTTP call
     *                              (e.g., a business class, a facade, or an external client)
     * @param anonymizeProperties   properties defining headers and body fields
     *                              to be masked in logs
     * @param httpLoggingProperties properties controlling outbound logging
     *                              (headers, bodies, maximum logged size)
     */
    RestClientLoggingInterceptor(String callerName, AnonymizeProperties anonymizeProperties, OutboundHttpLoggingProperties httpLoggingProperties) {
        this.callerName = callerName;
        this.httpLoggingService = new HttpLoggingService(httpLoggingProperties, anonymizeProperties);
    }

    /**
     * Intercepts an outbound HTTP request executed via {@code RestClient}.
     * <p>
     * The processing follows these steps:
     * </p>
     * <ol>
     *   <li>execution of the outbound request;</li>
     *   <li>buffering of the received response body;</li>
     *   <li>measurement of execution time;</li>
     *   <li>logging of the HTTP exchange using the buffered response;</li>
     *   <li>return of a buffered response wrapper so the response body remains
     *       readable by downstream consumers.</li>
     * </ol>
     * <p>
     * In case of an I/O error during execution, the exception is propagated
     * as is to the caller.
     * </p>
     *
     * @param request   outbound HTTP request
     * @param bodyRequest      request body as binary data
     * @param execution executor provided by Spring to continue the chain
     * @return a buffered HTTP response that can be consumed after logging
     * @throws IOException if an I/O error occurs during the HTTP call
     */
    @Override
    public @NonNull ClientHttpResponse intercept(@NonNull HttpRequest request, byte @NonNull [] bodyRequest, @NonNull ClientHttpRequestExecution execution) throws IOException {

        long start = System.currentTimeMillis();
        ClientHttpResponse responseToReturn = null;
        try (ClientHttpResponse response = execution.execute(request, bodyRequest)) {
            responseToReturn = new BufferedClientHttpResponse(response);
            return responseToReturn;
        } finally {
            if (responseToReturn != null && LOG.isInfoEnabled()) {
                long elapsed = System.currentTimeMillis() - start;
                var infoProvider = new OutboundHttpExchangeInfoProvider(request, bodyRequest, responseToReturn, callerName, elapsed);
                LOG.info(httpLoggingService.buildExchangeLog(infoProvider));
            }
        }
    }
}
