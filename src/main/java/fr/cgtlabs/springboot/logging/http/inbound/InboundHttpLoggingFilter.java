package fr.cgtlabs.springboot.logging.http.inbound;

import java.io.IOException;
import java.util.Objects;

import fr.cgtlabs.springboot.logging.http.common.HttpExchangeInfoProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.InboundHttpLoggingProperties;
import fr.cgtlabs.springboot.logging.http.service.HttpLoggingService;

/**
 * Filtre de journalisation HTTP entrant.
 * <p>
 * Ce filtre intercepte les requêtes HTTP entrantes pour en journaliser les détails.
 * Il s'applique uniquement aux chemins d'accès (paths) configurés via {@link InboundHttpLoggingProperties#getIncludedPaths()}.
 * Pour permettre la lecture des corps de requête et de réponse, il enveloppe (wraps) les objets
 * {@link HttpServletRequest} et {@link HttpServletResponse} avec des versions "caching"
 * ({@link ContentCachingRequestWrapper} et {@link ContentCachingResponseWrapper}).
 * La journalisation complète de l'échange HTTP (requête et réponse) n'est effectuée
 * que si le point de terminaison (endpoint) ciblé a été explicitement marqué comme
 * journalisable par un intercepteur MVC, généralement via une annotation comme
 * {@code @LoggedRestEndpoint} (bien que l'annotation ne soit pas directement référencée ici,
 * le mécanisme est implicite via {@link InboundHttpLoggingAttributes#LOGGING_ENABLED}).
 * </p>
 */
public class InboundHttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(InboundHttpLoggingFilter.class);

    private final InboundHttpLoggingProperties properties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final HttpLoggingService httpLoggingService;

    /**
     * Construit une nouvelle instance du filtre de journalisation HTTP entrant.
     *
     * @param properties          Les propriétés de configuration pour la journalisation HTTP entrante.
     * @param anonymizeProperties Les propriétés de configuration pour l'anonymisation des données sensibles dans les logs.
     */
    public InboundHttpLoggingFilter(InboundHttpLoggingProperties properties, AnonymizeProperties anonymizeProperties) {
        this.properties = properties;
        this.httpLoggingService = new HttpLoggingService(properties, anonymizeProperties);
    }

    /**
     * Détermine si ce filtre doit être appliqué à la requête HTTP donnée.
     * Le filtre est exclu si la fonctionnalité de journalisation est désactivée,
     * ou si la liste des chemins inclus est vide, ou si le chemin de la requête
     * ne correspond à aucun des motifs configurés dans {@link InboundHttpLoggingProperties#getIncludedPaths()}.
     *
     * @param request La requête HTTP entrante.
     * @return {@code true} si le filtre ne doit PAS s'appliquer à cette requête, {@code false} s'il DOIT s'appliquer.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        var includedPaths = properties.getIncludedPaths();
        if (properties.isEnabled() && !CollectionUtils.isEmpty(includedPaths)) {
            var path = resolvePathWithinApplication(request);
            return includedPaths.stream().filter(Objects::nonNull).noneMatch(pattern -> pathMatcher.match(pattern, path));
        }

        return true;
    }

    /**
     * Intercepte la requête et la réponse pour permettre la journalisation.
     * Cette méthode enveloppe la requête et la réponse avec des versions "caching"
     * pour rendre leurs corps lisibles plusieurs fois. Elle exécute ensuite la
     * chaîne de filtres et, si la requête a été marquée comme journalisable
     * (par exemple, par un intercepteur MVC), elle déclenche la journalisation
     * complète de l'échange HTTP.
     * <p>
     * Il est crucial que le corps de la réponse soit systématiquement recopié
     * vers la réponse d'origine via {@link ContentCachingResponseWrapper#copyBodyToResponse()}
     * dans le bloc {@code finally} pour s'assurer que le client reçoit la réponse.
     * </p>
     *
     * @param request     La requête HTTP entrante.
     * @param response    La réponse HTTP sortante.
     * @param filterChain La chaîne de filtres servlet à exécuter.
     * @throws ServletException Si une erreur spécifique au servlet se produit.
     * @throws IOException      Si une erreur d'entrée/sortie se produit.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper cachingRequest ? cachingRequest : new ContentCachingRequestWrapper(request, properties.getMaxBodyLogBytes());
        ContentCachingResponseWrapper wrappedResponse = response instanceof ContentCachingResponseWrapper cachingResponse ? cachingResponse : new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logAndCopyResponse(wrappedRequest, wrappedResponse);
        }
    }

    /**
     * Déclenche la journalisation de l'échange HTTP si la requête a été marquée comme
     * journalisable (via {@link InboundHttpLoggingAttributes#LOGGING_ENABLED}).
     * Indépendamment de la journalisation, cette méthode s'assure que le corps
     * de la réponse est copié vers la réponse HTTP originale pour être envoyé au client.
     *
     * @param request  La requête HTTP enveloppée (caching).
     * @param response La réponse HTTP enveloppée (caching).
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de la copie du corps de la réponse.
     */
    private void logAndCopyResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) throws IOException {
        try {
            if (Boolean.TRUE.equals(request.getAttribute(InboundHttpLoggingAttributes.LOGGING_ENABLED))) {
                logExchange(request, response);
            }
        } finally {
            response.copyBodyToResponse();
        }
    }

    /**
     * Résout le chemin de la requête au sein de l'application, en retirant le chemin de contexte
     * si celui-ci est présent dans l'URI de la requête.
     *
     * @param request La requête HTTP entrante.
     * @return Le chemin de la requête relatif à l'application.
     */
    private String resolvePathWithinApplication(HttpServletRequest request) {
        var requestUri = request.getRequestURI();
        var contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    /**
     * Journalise l'échange HTTP complet (requête et réponse) si le niveau de journalisation
     * INFO est activé. Cette méthode construit un message de log détaillé incluant
     * les métadonnées de la requête et de la réponse, ainsi que leurs corps si configuré.
     *
     * @param request  La requête HTTP enveloppée avec le corps mis en cache.
     * @param response La réponse HTTP enveloppée avec le corps mis en cache.
     */
    private void logExchange(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) throws IOException {
        if (LOG.isInfoEnabled()) {
            var handlerSignature = String.valueOf(request.getAttribute(InboundHttpLoggingAttributes.HANDLER_SIGNATURE));
            var infoProvider = new InboundHttpExchangeInfoProvider(request, response, handlerSignature);
            LOG.info(httpLoggingService.buildExchangeLog(infoProvider));
        }
    }


}
