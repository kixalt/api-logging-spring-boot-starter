package fr.cgtlabs.springboot.logging.properties;

/**
 * Interface commune pour les propriétés de configuration de la journalisation HTTP.
 * Elle définit les méthodes pour accéder aux paramètres de logging partagés
 * entre la journalisation des requêtes entrantes et sortantes.
 */
public interface HttpLoggingProperties {

    /**
     * Indique si les en-têtes HTTP doivent être journalisés.
     *
     * @return {@code true} si les en-têtes doivent être journalisés, {@code false} sinon.
     */
    boolean isLogHeaders();

    /**
     * Indique si le corps de la requête HTTP doit être journalisé.
     *
     * @return {@code true} si le corps de la requête doit être journalisé, {@code false} sinon.
     */
    boolean isLogRequestBody();

    /**
     * Indique si le corps de la réponse HTTP doit être journalisé.
     *
     * @return {@code true} si le corps de la réponse doit être journalisé, {@code false} sinon.
     */
    boolean isLogResponseBody();

    /**
     * Retourne la taille maximale en octets du corps de la requête ou de la réponse
     * qui sera journalisée. Si le corps dépasse cette taille, il sera tronqué.
     *
     * @return La taille maximale du corps à journaliser en octets.
     */
    int getMaxBodyLogBytes();
}
