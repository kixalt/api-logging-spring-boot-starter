package fr.cgtlabs.springboot.logging.http.outbound;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.OutboundHttpLoggingProperties;
import org.springframework.stereotype.Component;

/**
 * Factory for creating {@link RestClientLoggingInterceptor} instances.
 * This factory ensures that each interceptor is created with the necessary
 * logging and anonymization properties.
 */
public class RestClientLoggingInterceptorFactory {

    private final AnonymizeProperties anonymizeProperties;

    private final OutboundHttpLoggingProperties httpLoggingProperties;

    /**
     * Constructs a new {@code RestClientLoggingInterceptorFactory}.
     *
     * @param anonymizeProperties   Properties for anonymizing sensitive data in logs.
     * @param httpLoggingProperties Properties for outbound HTTP logging.
     */
    public RestClientLoggingInterceptorFactory(AnonymizeProperties anonymizeProperties, OutboundHttpLoggingProperties httpLoggingProperties) {
        this.anonymizeProperties = anonymizeProperties;
        this.httpLoggingProperties = httpLoggingProperties;
    }

    /**
     * Creates a new {@link RestClientLoggingInterceptor} instance for a given caller.
     *
     * @param callerName The logical name of the component originating the HTTP call.
     * @return A new {@link RestClientLoggingInterceptor} instance.
     */
    public RestClientLoggingInterceptor create(String callerName) {
        return new RestClientLoggingInterceptor(callerName, anonymizeProperties, httpLoggingProperties);
    }
}