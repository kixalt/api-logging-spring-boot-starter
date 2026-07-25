package fr.cgtlabs.springboot.logging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "logging.anonymize")
@Data
public final class AnonymizeProperties {

    private String[] headers = new String[0];

    private String[] body = new String[0];

    private String anonymizedString = "***";
}
