package fr.cgtlabs.springboot.logging.http.outbound;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.OutboundHttpLoggingProperties;
import fr.cgtlabs.springboot.logging.utils.HttpExchangeLogBuilder;
import fr.cgtlabs.springboot.logging.utils.HttpLoggingUtils;

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

    private final AnonymizeProperties anonymizeProperties;

    private final OutboundHttpLoggingProperties httpLoggingProperties;

    private final String callerName;

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
        this.anonymizeProperties = anonymizeProperties;
        this.httpLoggingProperties = httpLoggingProperties;
        this.callerName = callerName;
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
                String message = buildExchangeLog(request, body, response, elapsed);
                LOG.info(message);
            }
        }
    }

    /**
     * Construit le log complet de l'échange HTTP sortant sous forme d'un bloc
     * unique regroupant requête et réponse.
     *
     * @param request requête HTTP sortante
     * @param requestBody corps brut de la requête
     * @param response réponse HTTP reçue
     * @param elapsed durée d'exécution en millisecondes
     * @return message de log complet prêt à être journalisé
     * @throws IOException si la lecture du corps de la réponse échoue
     */
    private String buildExchangeLog(HttpRequest request, byte[] requestBody, ClientHttpResponse response, long elapsed) throws IOException {
        var builder = new HttpExchangeLogBuilder(callerName)
                .start(Direction.OUTBOUND)
                .sectionSeparator();

        appendRequest(builder, request, requestBody);
        builder.blankLine().sectionSeparator();
        appendResponse(builder, response, request, elapsed);

        return builder.build();
    }

    /**
     * Ajoute au builder les métadonnées de la requête sortante puis,
     * si activé, son corps lorsque celui-ci est présent.
     *
     * @param builder     builder de bloc de log HTTP
     * @param request     requête HTTP sortante
     * @param requestBody corps brut de la requête
     */
    private void appendRequest(HttpExchangeLogBuilder builder, HttpRequest request, byte[] requestBody) {
        builder.sectionTitle("→ Requête sortante")
                .field("Méthode", request.getMethod())
                .field("URI", request.getURI())
                .field("Headers", httpLoggingProperties.isLogHeaders() ? HttpLoggingUtils.buildHeadersLog(request, anonymizeProperties) : "[désactivé]");

        appendRequestBody(builder, request, requestBody);
    }

    /**
     * Ajoute au builder les métadonnées de la réponse HTTP reçue puis,
     * si activé, son corps lorsque celui-ci est disponible.
     *
     * @param builder  builder de bloc de log HTTP
     * @param response réponse HTTP reçue
     * @param request  requête initiale associée
     * @param elapsed  durée d'exécution en millisecondes
     * @throws IOException si la lecture du corps de la réponse échoue
     */
    private void appendResponse(HttpExchangeLogBuilder builder, ClientHttpResponse response, HttpRequest request, long elapsed) throws IOException {
        builder.sectionTitle("← Réponse reçue")
                .field("Statut", response.getStatusCode().value())
                .field("Durée", elapsed + " ms")
                .field("URI", request.getURI());

        appendResponseBody(builder, response);
    }

    /**
     * Ajoute au builder le corps de la requête si l'option correspondante est activée
     * et si le corps n'est pas vide.
     *
     * @param builder     builder de bloc de log HTTP
     * @param request     requête HTTP sortante
     * @param requestBody corps brut de la requête
     */
    private void appendRequestBody(HttpExchangeLogBuilder builder, HttpRequest request, byte[] requestBody) {
        if (httpLoggingProperties.isLogRequestBody() && requestBody.length > 0) {
            MediaType contentType = request.getHeaders().getContentType();
            appendBody(builder, "requête", requestBody, contentType);
        }
    }

    /**
     * Ajoute au builder le corps de la réponse si l'option correspondante est activée
     * et si le corps contient des données.
     *
     * @param builder  builder de bloc de log HTTP
     * @param response réponse HTTP reçue
     * @throws IOException si la lecture du flux de réponse échoue
     */
    private void appendResponseBody(HttpExchangeLogBuilder builder, ClientHttpResponse response) throws IOException {
        if (httpLoggingProperties.isLogResponseBody()) {
            byte[] responseBody = response.getBody().readAllBytes();
            if (responseBody.length > 0) {
                MediaType contentType = response.getHeaders().getContentType();
                appendBody(builder, "réponse", responseBody, contentType);
            }
        }
    }

    /**
     * Ajoute au builder un body HTTP en tenant compte de son type de contenu.
     * <p>
     * Si le contenu est considéré comme textuel/loggable, il est converti,
     * anonymisé puis éventuellement tronqué avant ajout au bloc.
     * Sinon, seules des métadonnées de type et de taille sont produites.
     * </p>
     *
     * @param builder      builder de bloc de log HTTP
     * @param payloadLabel libellé fonctionnel du payload journalisé
     *                     ({@code requête} ou {@code réponse})
     * @param body         contenu brut du body
     * @param contentType  type de contenu HTTP associé
     */
    private void appendBody(HttpExchangeLogBuilder builder, String payloadLabel, byte[] body, MediaType contentType) {
        if (HttpLoggingUtils.isLoggableContentType(contentType)) {
            var formattedBody = HttpLoggingUtils.extractTextBody(body, contentType, httpLoggingProperties.getMaxBodyLogBytes(), anonymizeProperties);
            builder.body(payloadLabel, contentType, formattedBody);
        } else {
            builder.ignoredBody(payloadLabel, contentType, body.length);
        }
    }

}