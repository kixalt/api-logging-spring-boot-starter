package fr.cgtlabs.springboot.logging.http.inbound;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the inbound HTTP logging interceptor in the Spring MVC chain.
 */
public final class InboundHttpLoggingMvcConfigurer implements WebMvcConfigurer {

    private final LoggedRestEndpointInterceptor loggedRestEndpointInterceptor;

    /**
     * Constructs the MVC configurer responsible for registering the interceptor
     * for detecting REST endpoints to be logged.
     *
     * @param loggedRestEndpointInterceptor MVC interceptor for detecting annotated methods
     */
    public InboundHttpLoggingMvcConfigurer(LoggedRestEndpointInterceptor loggedRestEndpointInterceptor) {
        this.loggedRestEndpointInterceptor = loggedRestEndpointInterceptor;
    }

    /**
     * Registers the inbound logging interceptor in the Spring MVC registry.
     *
     * @param registry MVC interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggedRestEndpointInterceptor);
    }
}