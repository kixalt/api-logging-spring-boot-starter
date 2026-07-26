package fr.cgtlabs.springboot.logging.http.outbound;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.OutboundHttpLoggingProperties;
import fr.cgtlabs.springboot.logging.http.service.HttpLoggingService;

/**
 * Interceptor de logging des appels HTTP sortants effectués via {@code RestClient}.
 * <p>
 * Ce composant journalise les requêtes et réponses HTTP sortantes en y associant
 * un contexte appelant explicite ({@code callerName}) afin d'identifier le
 * composant métier ou technique à l'origine de l'appel.
 * </p>
 * <p>
 * Il prend en charge :
 * </p>
 * <ul>
 *   <li>le logging des métadonnées de requête : méthode, URI, headers ;</li>
 *   <li>le logging des métadonnées de réponse : statut HTTP, durée, URI ;</li>
 *   <li>le logging optionnel des corps de requête et de réponse ;</li>
 *   <li>l'anonymisation des headers sensibles ;</li>
 *   <li>l'anonymisation de champs sensibles dans les payloads textuels ;</li>
 *   <li>la limitation de taille des bodies journalisés.</li>
 * </ul>
 * <p>
 * Cette classe n'a pas vocation à être instanciée directement depuis l'extérieur
 * du package ; elle est destinée à être créée via une factory dédiée afin de
 * garantir la fourniture explicite du contexte appelant.
 * </p>
 */
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientLoggingInterceptor.class);

    private final String callerName;

    private final HttpLoggingService httpLoggingService;

    /**
     * Construit un interceptor de logging HTTP sortant associé à un appelant explicite.
     *
     * @param callerName nom logique du composant à l'origine de l'appel HTTP
     *                   (par exemple une classe métier, une façade ou un client externe)
     * @param anonymizeProperties propriétés définissant les headers et champs de body
     *                            à masquer dans les logs
     * @param httpLoggingProperties propriétés pilotant le logging sortant
     *                              (headers, bodies, taille maximale journalisée)
     */
    RestClientLoggingInterceptor(String callerName, AnonymizeProperties anonymizeProperties, OutboundHttpLoggingProperties httpLoggingProperties) {
        this.callerName = callerName;
        this.httpLoggingService = new HttpLoggingService(httpLoggingProperties, anonymizeProperties);
    }

    /**
     * Intercepte une requête HTTP sortante exécutée via {@code RestClient}.
     * <p>
     * Le traitement suit les étapes suivantes :
     * </p>
     * <ol>
     *   <li>journalisation de la requête sortante ;</li>
     *   <li>exécution réelle de la requête ;</li>
     *   <li>mesure du temps d'exécution ;</li>
     *   <li>journalisation de la réponse reçue si celle-ci est disponible.</li>
     * </ol>
     * <p>
     * En cas d'erreur d'entrée/sortie lors de l'exécution, l'exception est propagée
     * telle quelle à l'appelant.
     * </p>
     *
     * @param request requête HTTP sortante
     * @param body corps de la requête sous forme binaire
     * @param execution exécuteur fourni par Spring pour poursuivre la chaîne
     * @return la réponse HTTP renvoyée par le serveur distant
     * @throws IOException si une erreur d'entrée/sortie survient pendant l'appel HTTP
     */
    @Override
    public ClientHttpResponse intercept(@NonNull HttpRequest request, byte @NonNull [] body, ClientHttpRequestExecution execution) throws IOException {

        long start = System.currentTimeMillis();
        ClientHttpResponse response = null;
        try {
            response = execution.execute(request, body);
            return response;
        } finally {
            if (response != null && LOG.isInfoEnabled()) {
                long elapsed = System.currentTimeMillis() - start;
                var infoProvider = new OutboundHttpExchangeInfoProvider(request, body, response, callerName, elapsed);
                LOG.info(httpLoggingService.buildExchangeLog(infoProvider));
            }
        }
    }
}
