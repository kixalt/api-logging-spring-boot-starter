package fr.cgtlabs.springboot.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import fr.cgtlabs.springboot.logging.properties.InboundHttpLoggingProperties;
import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties({AnonymizeProperties.class, InboundHttpLoggingProperties.class})
public class SpringLoggingApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringLoggingApplication.class, args);
	}

}
