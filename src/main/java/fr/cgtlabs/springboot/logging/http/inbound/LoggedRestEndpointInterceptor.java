package fr.cgtlabs.springboot.logging.http.inbound;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor responsible for identifying REST controller methods
 * explicitly annotated with {@link LoggedRestEndpoint}.
 * <p>
 * When an annotated method is detected, the interceptor sets the necessary
 * technical attributes in the request for the inbound HTTP logging filter:
 * logging activation, start time, and handler signature.
 * </p>
 */
public class LoggedRestEndpointInterceptor implements HandlerInterceptor {

    /**
     * Detects if the targeted MVC handler corresponds to a method annotated with
     * {@link LoggedRestEndpoint} and, if so, enriches the request with the
     * attributes necessary for inbound HTTP logging.
     * <p>
     * No blocking is applied: this method always returns {@code true}
     * to allow the MVC chain to proceed normally.
     * </p>
     *
     * @param request incoming HTTP request
     * @param response current HTTP response
     * @param handler Spring MVC handler resolved for the request
     * @return always {@code true}
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
     * Builds a readable signature of the targeted Spring MVC handler in the format
     * {@code ControllerName#methodName}.
     *
     * @param handlerMethod resolved Spring MVC method
     * @return textual signature of the handler
     */
    private String buildHandlerSignature(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
    }
}