package fr.cgtlabs.springboot.logging.http.test;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.InboundHttpLoggingProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "fr.cgtlabs.springboot.logging.properties")
@EnableConfigurationProperties({AnonymizeProperties.class, InboundHttpLoggingProperties.class})
public class TestApplication {
}
