package fr.cgtlabs.springboot.logging.http.outbound;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.OutboundHttpLoggingProperties;

@Component
public class RestClientLoggingInterceptorFactory {

    private final AnonymizeProperties anonymizeProperties;

    private final OutboundHttpLoggingProperties httpLoggingProperties;

    public RestClientLoggingInterceptorFactory(AnonymizeProperties anonymizeProperties, OutboundHttpLoggingProperties httpLoggingProperties) {
        this.anonymizeProperties = anonymizeProperties;
        this.httpLoggingProperties = httpLoggingProperties;
    }

    public RestClientLoggingInterceptor create(String callerName) {
        return new RestClientLoggingInterceptor(callerName, anonymizeProperties, httpLoggingProperties);
    }
}

