package fr.cgtlabs.springboot.logging.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;

import fr.cgtlabs.springboot.logging.http.outbound.Direction;

/**
 * Builder dédié à la construction de blocs de logs représentant un échange HTTP
 * complet, entrant ou sortant.
 * <p>
 * Il centralise le formatage commun des sections, champs et bodies afin de
 * garantir un rendu homogène entre les composants de logging HTTP.
 * </p>
 */
public final class HttpExchangeLogBuilder {

    private final String context;

    private final List<String> lines = new ArrayList<>();

    /**
     * Construit un builder de bloc de log HTTP.
     * @param context contexte affiché dans le log (handler MVC, caller name, etc.)
     */
    public HttpExchangeLogBuilder(String context) {
        this.context = context;
    }

    /**
     * Démarre le bloc de log avec son en-tête commun.
     *
     * @return builder courant
     */
    public HttpExchangeLogBuilder start(Direction direction) {
        lines.add("");
        lines.add(Constants.EXCHANGE_SEPARATOR);
        lines.add("[%s] HTTP %s".formatted(context, direction));
        return this;
    }

    /**
     * Ajoute un séparateur de section.
     *
     * @return builder courant
     */
    public HttpExchangeLogBuilder sectionSeparator() {
        lines.add(Constants.SECTION_SEPARATOR);
        return this;
    }

    /**
     * Ajoute une ligne vide.
     *
     * @return builder courant
     */
    public HttpExchangeLogBuilder blankLine() {
        lines.add("");
        return this;
    }

    /**
     * Ajoute un titre de section préfixé par le contexte courant.
     *
     * @param title titre à afficher
     * @return builder courant
     */
    public HttpExchangeLogBuilder sectionTitle(String title) {
        lines.add("[%s] %s".formatted(context, title));
        return this;
    }

    /**
     * Ajoute un champ libellé/valeur au bloc de log.
     *
     * @param label libellé à afficher
     * @param value valeur associée
     * @return builder courant
     */
    public HttpExchangeLogBuilder field(String label, Object value) {
        lines.add("  %-8s : %s".formatted(label, value));
        return this;
    }

    /**
     * Ajoute un body textuel journalisable au bloc de log.
     *
     * @param payloadLabel libellé fonctionnel du payload
     * @param contentType type de contenu HTTP
     * @param body corps formaté à afficher
     * @return builder courant
     */
    public HttpExchangeLogBuilder body(String payloadLabel, MediaType contentType, String body) {
        lines.add("[%s] -> Body (%s, type=%s :".formatted(context, payloadLabel, contentType));
        lines.add(body);
        return this;
    }

    /**
     * Ajoute une information de body ignoré au bloc de log.
     *
     * @param payloadLabel libellé fonctionnel du payload
     * @param contentType type de contenu HTTP
     * @param length taille du body en octets
     * @return builder courant
     */
    public HttpExchangeLogBuilder ignoredBody(String payloadLabel, MediaType contentType, int length) {
        lines.add("[%s] -> Body (Contenu de la %s ignoré, type=%s : taille=%s octets)"
                .formatted(context, payloadLabel, contentType != null ? contentType : "inconnu", length));
        return this;
    }

    /**
     * Construit la chaîne finale représentant le bloc de log complet.
     *
     * @return message final prêt à être journalisé
     */
    public String build() {
        return String.join("\n", lines) + "\n" + Constants.EXCHANGE_SEPARATOR;
    }
}

