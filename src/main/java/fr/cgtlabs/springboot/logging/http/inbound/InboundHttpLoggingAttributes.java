package fr.cgtlabs.springboot.logging.http.inbound;

import lombok.experimental.UtilityClass;

/**
 * <p>
 * This utility class contains technical constants (attribute names)
 * used to facilitate communication and information sharing
 * between the Spring MVC interceptor ({@link LoggedRestEndpointInterceptor})
 * and the inbound HTTP logging filter ({@link InboundHttpLoggingFilter}).
 * </p>
 * <p>
 * These attributes are stored in the {@link jakarta.servlet.http.HttpServletRequest} object
 * to be accessible throughout the request lifecycle.
 * </p>
 */
@UtilityClass
public final class InboundHttpLoggingAttributes {

    /**
     * Request attribute name that indicates whether the target endpoint
     * for the HTTP request is explicitly annotated with {@link LoggedRestEndpoint}
     * and therefore should be logged.
     * The associated value is a {@link Boolean}, typically {@code Boolean.TRUE}.
     */
    public final String LOGGING_ENABLED = InboundHttpLoggingAttributes.class.getName() + ".LOGGING_ENABLED";

    /**
     * Request attribute name that stores the start time of the HTTP request
     * processing. This time is expressed in milliseconds since the epoch
     * (epoch milliseconds), obtained via {@link System#currentTimeMillis()}.
     * It is used to calculate the total processing duration of the request.
     */
    public final String START_TIME = InboundHttpLoggingAttributes.class.getName() + ".START_TIME";

    /**
     * Request attribute name that contains the readable signature of the
     * Spring MVC handler that processes the request. This signature is useful for
     * contextualizing logging messages and precisely identifying
     * the executed controller method. The format is typically
     * {@code ControllerName#methodName}.
     */
    public final String HANDLER_SIGNATURE = InboundHttpLoggingAttributes.class.getName() + ".HANDLER_SIGNATURE";

}