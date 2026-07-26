package fr.cgtlabs.springboot.logging.http.inbound;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicitly marks a REST controller method as eligible for inbound HTTP logging.
 * <p>
 * This annotation is intended to be used on Spring MVC / REST methods to
 * trigger the centralized logging of inbound calls, provided that the request has already
 * been selected by the technical filter (e.g., via configured protected paths).
 * </p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggedRestEndpoint {
}