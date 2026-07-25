package fr.cgtlabs.springboot.logging.http.inbound;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque explicitement une méthode de contrôleur REST comme éligible au logging HTTP entrant.
 * <p>
 * Cette annotation est destinée à être utilisée sur des méthodes Spring MVC / REST afin de
 * déclencher le logging mutualisé des appels entrants, sous réserve que la requête ait déjà
 * été retenue par le filtre technique (par exemple via les paths protégés configurés).
 * </p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggedRestEndpoint {
}
