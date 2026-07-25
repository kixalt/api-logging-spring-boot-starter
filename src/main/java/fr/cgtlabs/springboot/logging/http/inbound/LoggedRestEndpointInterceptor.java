package fr.cgtlabs.springboot.logging.http.inbound;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor Spring MVC chargé d'identifier les méthodes de contrôleur REST
 * explicitement annotées avec {@link LoggedRestEndpoint}.
 * <p>
 * Lorsqu'une méthode annotée est détectée, l'interceptor pose dans la requête
 * les attributs techniques nécessaires au filtre de logging HTTP entrant :
 * activation du logging, instant de départ et signature du handler.
 * </p>
 */
public class LoggedRestEndpointInterceptor implements HandlerInterceptor {

    /**
     * Détecte si le handler MVC ciblé correspond à une méthode annotée avec
     * {@link LoggedRestEndpoint} et, le cas échéant, enrichit la requête avec les
     * attributs nécessaires au logging HTTP entrant.
     * <p>
     * Aucun blocage n'est appliqué : cette méthode retourne toujours {@code true}
     * afin de laisser la chaîne MVC se poursuivre normalement.
     * </p>
     *
     * @param request requête HTTP entrante
     * @param response réponse HTTP courante
     * @param handler handler Spring MVC résolu pour la requête
     * @return toujours {@code true}
     */
    @Override
    public boolean preHandle(
                    @NonNull HttpServletRequest request,
                    @NonNull HttpServletResponse response,
                    @NonNull Object handler) {

        if (handler instanceof HandlerMethod handlerMethod && handlerMethod.hasMethodAnnotation(LoggedRestEndpoint.class)) {
            request.setAttribute(InboundHttpLoggingAttributes.LOGGING_ENABLED, Boolean.TRUE);
            request.setAttribute(InboundHttpLoggingAttributes.START_TIME, System.currentTimeMillis());
            request.setAttribute(InboundHttpLoggingAttributes.HANDLER_SIGNATURE, buildHandlerSignature(handlerMethod));
        }
        return true;
    }

    /**
     * Construit une signature lisible du handler Spring MVC ciblé sous la forme
     * {@code NomControleur#nomMethode}.
     *
     * @param handlerMethod méthode Spring MVC résolue
     * @return signature textuelle du handler
     */
    private String buildHandlerSignature(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
    }
}
