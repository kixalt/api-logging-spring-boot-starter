package fr.cgtlabs.springboot.logging.http.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.InboundHttpLoggingProperties;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "fr.cgtlabs.springboot.logging.properties")
@EnableConfigurationProperties({AnonymizeProperties.class, InboundHttpLoggingProperties.class})
public class TestApplication {
    // La méthode main n'est pas nécessaire pour une application de test Spring Boot
}
