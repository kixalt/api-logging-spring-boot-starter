package fr.cgtlabs.springboot.logging.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Propriétés de configuration du logging HTTP entrant.
 */
@ConfigurationProperties(prefix = "logging.inbound")
@Data
public final class InboundHttpLoggingProperties implements HttpLoggingProperties {

    /**
     * Active ou désactive globalement la fonctionnalité de logging HTTP entrant.
     */
    private boolean enabled = false;

    /**
     * Active le logging des headers HTTP.
     */
    private boolean logHeaders = true;

    /**
     * Active le logging du body de la requête entrante.
     */
    private boolean logRequestBody = true;

    /**
     * Active le logging du body de la réponse sortante.
     */
    private boolean logResponseBody = true;

    /**
     * Taille maximale, en octets, d'un body loggué.
     */
    private int maxBodyLogBytes = 10 * 1024;

    /**
     * Liste des paths HTTP inclus dans le périmètre technique du filtre de logging entrant.
     * <p>
     * Les patterns sont destinés à être évalués avec la sémantique Spring/Ant de type
     * {@code /api/**}.
     * </p>
     */
    private List<String> includedPaths = new ArrayList<>();
}