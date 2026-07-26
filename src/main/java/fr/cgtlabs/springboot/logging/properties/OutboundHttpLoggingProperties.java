package fr.cgtlabs.springboot.logging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "logging.outbound")
@Data
public class OutboundHttpLoggingProperties implements HttpLoggingProperties {

    private int maxBodyLogBytes = 10 * 1024;

    private boolean logRequestBody = true;

    private boolean logResponseBody = true;

    private boolean logHeaders = true;
}