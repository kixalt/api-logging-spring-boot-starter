package fr.cgtlabs.springboot.logging.http.inbound;

import lombok.experimental.UtilityClass;

/**
 * Constantes techniques utilisées pour échanger des informations entre
 * l'interceptor MVC et le filtre de logging HTTP entrant via les attributs
 * de requête.
 */
@UtilityClass
public final class InboundHttpLoggingAttributes {

    /**
     * Indique que le endpoint ciblé est explicitement annoté et doit donc être loggué.
     */
    public final String LOGGING_ENABLED = InboundHttpLoggingAttributes.class.getName() + ".LOGGING_ENABLED";

    /**
     * Instant de début du traitement de la requête, exprimé en millisecondes epoch.
     */
    public final String START_TIME = InboundHttpLoggingAttributes.class.getName() + ".START_TIME";

    /**
     * Signature lisible du handler Spring MVC ciblé, utile pour contextualiser les logs.
     */
    public final String HANDLER_SIGNATURE = InboundHttpLoggingAttributes.class.getName() + ".HANDLER_SIGNATURE";

}
