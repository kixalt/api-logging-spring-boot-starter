package fr.cgtlabs.springboot.logging.http.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;

/**
 * Interface pour fournir des informations sur un échange HTTP (requête/réponse)
 * de manière agnostique, qu'il soit entrant ou sortant.
 */
public interface HttpExchangeInfoProvider {

    /**
     * Retourne la direction de l'échange HTTP (INBOUND ou OUTBOUND).
     * @return La direction de l'échange.
     */
    Direction getDirection();

    /**
     * Retourne un descripteur de l'échange, qui peut être la signature du handler
     * pour une requête entrante ou le nom de l'appelant pour une requête sortante.
     * @return Le descripteur de l'échange.
     */
    String getExchangeDescriptor();

    /**
     * Retourne la méthode HTTP de la requête.
     * @return La méthode HTTP.
     */
    HttpMethod getHttpMethod();

    /**
     * Retourne l'URI de la requête.
     * @return L'URI.
     */
    URI getUri();

    /**
     * Retourne les en-têtes de la requête.
     * @return Les en-têtes de la requête.
     */
    HttpHeaders getRequestHeaders();

    /**
     * Retourne le corps de la requête sous forme de tableau d'octets.
     * @return Le corps de la requête.
     */
    byte[] getRequestBody();

    /**
     * Retourne le code de statut HTTP de la réponse.
     * @return Le code de statut.
     */
    int getResponseStatus();

    /**
     * Retourne les en-têtes de la réponse.
     * @return Les en-têtes de la réponse.
     */
    HttpHeaders getResponseHeaders();

    /**
     * Retourne le corps de la réponse sous forme de tableau d'octets.
     * @return Le corps de la réponse.
     * @throws IOException En cas d'erreur lors de la lecture du corps.
     */
    byte[] getResponseBody() throws IOException;

    /**
     * Retourne le temps écoulé pour l'échange en millisecondes.
     * @return Le temps écoulé.
     */
    long getElapsedTime();

    /**
     * Retourne le type de contenu de la requête.
     * @return Le type de contenu de la requête.
     */
    MediaType getRequestContentType();

    /**
     * Retourne le type de contenu de la réponse.
     * @return Le type de contenu de la réponse.
     */
    MediaType getResponseContentType();
}
