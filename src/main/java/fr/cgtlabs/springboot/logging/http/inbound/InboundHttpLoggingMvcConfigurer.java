package fr.cgtlabs.springboot.logging.http.inbound;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enregistre l'interceptor de logging HTTP entrant dans la chaîne Spring MVC.
 */
public final class InboundHttpLoggingMvcConfigurer implements WebMvcConfigurer {

    private final LoggedRestEndpointInterceptor loggedRestEndpointInterceptor;

    /**
     * Construit le configurer MVC chargé d'enregistrer l'interceptor de détection
     * des endpoints REST à logger.
     *
     * @param loggedRestEndpointInterceptor interceptor MVC de détection des méthodes annotées
     */
    public InboundHttpLoggingMvcConfigurer(LoggedRestEndpointInterceptor loggedRestEndpointInterceptor) {
        this.loggedRestEndpointInterceptor = loggedRestEndpointInterceptor;
    }

    /**
     * Enregistre l'interceptor de logging entrant dans le registre Spring MVC.
     *
     * @param registry registre des interceptors MVC
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggedRestEndpointInterceptor);
    }
}
