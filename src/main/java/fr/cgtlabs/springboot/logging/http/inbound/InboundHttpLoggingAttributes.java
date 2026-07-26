package fr.cgtlabs.springboot.logging.http.inbound;

import lombok.experimental.UtilityClass;

/**
 * <p>
 * Cette classe utilitaire contient des constantes techniques (noms d'attributs)
 * utilisées pour faciliter la communication et le partage d'informations
 * entre l'intercepteur Spring MVC ({@link LoggedRestEndpointInterceptor})
 * et le filtre de journalisation HTTP entrant ({@link InboundHttpLoggingFilter}).
 * </p>
 * <p>
 * Ces attributs sont stockés dans l'objet {@link jakarta.servlet.http.HttpServletRequest}
 * pour être accessibles tout au long du cycle de vie de la requête.
 * </p>
 */
@UtilityClass
public final class InboundHttpLoggingAttributes {

    /**
     * Nom de l'attribut de requête qui indique si le point de terminaison (endpoint)
     * ciblé par la requête HTTP est explicitement annoté avec {@link LoggedRestEndpoint}
     * et doit donc être journalisé.
     * La valeur associée est un {@link Boolean}, généralement {@code Boolean.TRUE}.
     */
    public final String LOGGING_ENABLED = InboundHttpLoggingAttributes.class.getName() + ".LOGGING_ENABLED";

    /**
     * Nom de l'attribut de requête qui stocke l'instant de début du traitement
     * de la requête HTTP. Cet instant est exprimé en millisecondes depuis l'époque
     * (epoch milliseconds), obtenu via {@link System#currentTimeMillis()}.
     * Il est utilisé pour calculer la durée totale de traitement de la requête.
     */
    public final String START_TIME = InboundHttpLoggingAttributes.class.getName() + ".START_TIME";

    /**
     * Nom de l'attribut de requête qui contient la signature lisible du gestionnaire
     * (handler) Spring MVC qui traite la requête. Cette signature est utile pour
     * contextualiser les messages de journalisation et identifier précisément
     * la méthode du contrôleur exécutée. Le format est généralement
     * {@code NomControleur#nomMethode}.
     */
    public final String HANDLER_SIGNATURE = InboundHttpLoggingAttributes.class.getName() + ".HANDLER_SIGNATURE";

}