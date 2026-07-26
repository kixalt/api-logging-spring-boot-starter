package fr.cgtlabs.springboot.logging.config;

import fr.cgtlabs.springboot.logging.http.inbound.InboundHttpLoggingFilter;
import fr.cgtlabs.springboot.logging.http.inbound.InboundHttpLoggingMvcConfigurer;
import fr.cgtlabs.springboot.logging.http.inbound.LoggedRestEndpointInterceptor;
import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.InboundHttpLoggingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration pour la journalisation HTTP entrante.
 * Cette classe enregistre conditionnellement le filtre de logging HTTP entrant
 * et l'intercepteur MVC associé si les propriétés de configuration le permettent.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "logging.inbound", name = "enabled", havingValue = "true")
public class HttpLoggingAutoConfiguration {

    /**
     * Enregistre le {@link InboundHttpLoggingFilter} en tant que bean Spring.
     * Le filtre est activé si la propriété `logging.inbound.enabled` est à `true`.
     *
     * @param inboundHttpLoggingProperties Propriétés de configuration du logging entrant.
     * @param anonymizeProperties Propriétés d'anonymisation.
     * @return Un {@link FilterRegistrationBean} pour le filtre de logging entrant.
     */
    @Bean
    public FilterRegistrationBean<InboundHttpLoggingFilter> inboundHttpLoggingFilterRegistration(
            InboundHttpLoggingProperties inboundHttpLoggingProperties,
            AnonymizeProperties anonymizeProperties) {
        FilterRegistrationBean<InboundHttpLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new InboundHttpLoggingFilter(inboundHttpLoggingProperties, anonymizeProperties));
        registration.addUrlPatterns("/*"); // Applique le filtre à toutes les URL
        registration.setOrder(1); // Définit l'ordre d'exécution du filtre
        return registration;
    }

    /**
     * Enregistre le {@link LoggedRestEndpointInterceptor} en tant que bean Spring.
     *
     * @return Une instance de {@link LoggedRestEndpointInterceptor}.
     */
    @Bean
    public LoggedRestEndpointInterceptor loggedRestEndpointInterceptor() {
        return new LoggedRestEndpointInterceptor();
    }

    /**
     * Enregistre le {@link InboundHttpLoggingMvcConfigurer} pour ajouter
     * le {@link LoggedRestEndpointInterceptor} à la chaîne MVC.
     *
     * @param loggedRestEndpointInterceptor L'intercepteur de logging REST.
     * @return Une instance de {@link WebMvcConfigurer}.
     */
    @Bean
    public WebMvcConfigurer inboundHttpLoggingMvcConfigurer(LoggedRestEndpointInterceptor loggedRestEndpointInterceptor) {
        return new InboundHttpLoggingMvcConfigurer(loggedRestEndpointInterceptor);
    }
}
