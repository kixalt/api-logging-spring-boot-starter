package fr.cgtlabs.springboot.logging.config;

import fr.cgtlabs.springboot.logging.http.inbound.InboundHttpLoggingFilter;
import fr.cgtlabs.springboot.logging.http.inbound.InboundHttpLoggingMvcConfigurer;
import fr.cgtlabs.springboot.logging.http.inbound.LoggedRestEndpointInterceptor;
import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.InboundHttpLoggingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for inbound HTTP logging.
 * This class conditionally registers the inbound HTTP logging filter
 * and the associated MVC interceptor if the configuration properties allow it.
 */
@Configuration
@ConditionalOnProperty(prefix = "logging.inbound", name = "enabled", havingValue = "true")
public class HttpLoggingAutoConfiguration {

    /**
     * Registers the {@link InboundHttpLoggingFilter} as a Spring bean.
     * The filter is enabled if the `logging.inbound.enabled` property is set to `true`.
     *
     * @param inboundHttpLoggingProperties Inbound logging configuration properties.
     * @param anonymizeProperties Anonymization properties.
     * @return A {@link FilterRegistrationBean} for the inbound logging filter.
     */
    @Bean
    public FilterRegistrationBean<InboundHttpLoggingFilter> inboundHttpLoggingFilterRegistration(
            InboundHttpLoggingProperties inboundHttpLoggingProperties,
            AnonymizeProperties anonymizeProperties) {
        FilterRegistrationBean<InboundHttpLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new InboundHttpLoggingFilter(inboundHttpLoggingProperties, anonymizeProperties));
        registration.addUrlPatterns("/*"); // Applies the filter to all URLs
        registration.setOrder(1); // Sets the execution order of the filter
        return registration;
    }

    /**
     * Registers the {@link LoggedRestEndpointInterceptor} as a Spring bean.
     *
     * @return An instance of {@link LoggedRestEndpointInterceptor}.
     */
    @Bean
    public LoggedRestEndpointInterceptor loggedRestEndpointInterceptor() {
        return new LoggedRestEndpointInterceptor();
    }

    /**
     * Registers the {@link InboundHttpLoggingMvcConfigurer} to add
     * the {@link LoggedRestEndpointInterceptor} to the MVC chain.
     *
     * @param loggedRestEndpointInterceptor The REST logging interceptor.
     * @return An instance of {@link WebMvcConfigurer}.
     */
    @Bean
    public WebMvcConfigurer inboundHttpLoggingMvcConfigurer(LoggedRestEndpointInterceptor loggedRestEndpointInterceptor) {
        return new InboundHttpLoggingMvcConfigurer(loggedRestEndpointInterceptor);
    }
}