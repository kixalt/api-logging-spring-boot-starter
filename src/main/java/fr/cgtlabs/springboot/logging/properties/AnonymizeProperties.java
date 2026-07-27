package fr.cgtlabs.springboot.logging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for anonymizing sensitive data in HTTP logs.
 * These properties define which headers and body fields should be masked.
 */
@ConfigurationProperties(prefix = "logging.anonymize")
@Getter
@Setter
public final class AnonymizeProperties {

    /**
     * An array of HTTP header names whose values should be anonymized in the logs.
     * Case-insensitive matching is applied.
     */
    private String[] headers = new String[0];

    /**
     * An array of JSON/XML field names whose values should be anonymized in the logs.
     * These fields are matched using JSONPath for JSON and XPath for XML.
     */
    private String[] body = new String[0];

    /**
     * The string used to replace anonymized values in the logs.
     * Defaults to "***".
     */
    private String anonymizedString = "***";
}