# api-logging-spring-boot-starter
Starter Spring Boot pour centraliser le logging HTTP :

- **entrant** côté Spring MVC, avec opt-in par annotation sur les endpoints ;
- **sortant** côté `RestClient`, avec interception des appels externes ;
- **anonymisation** des headers et des champs sensibles dans les bodies.

## Fonctionnalités

### Logging HTTP entrant

Le projet fournit une infrastructure de logging pour les appels reçus par une application Spring MVC :

- activation via propriétés Spring Boot ;
- filtrage par chemins inclus (`includedPaths`) ;
- annotation explicite des méthodes à logger avec `@LoggedRestEndpoint` ;
- logging de la requête et de la réponse dans un bloc unique par échange ;
- mesure du temps d'exécution ;
- support du logging des headers ;
- support du logging des bodies requête/réponse ;
- limitation de la taille des bodies loggés ;
- anonymisation des données sensibles.

Composants principaux :

- `ApiLoggingAutoConfiguration`
- `InboundHttpLoggingFilter`
- `LoggedRestEndpointInterceptor`
- `InboundHttpLoggingMvcConfigurer`
- `LoggedRestEndpoint`
- `InboundHttpLoggingProperties`
- `AnonymizeProperties`

### Logging HTTP sortant

Le projet expose également un interceptor pour les appels réalisés avec `RestClient` :

- logging de la méthode HTTP, de l'URI, des headers et des bodies ;
- regroupement de la requête et de la réponse dans un bloc de log unique par échange ;
- mesure du temps d'exécution ;
- anonymisation des headers et des champs sensibles ;
- limitation de la taille des bodies loggés ;
- prise en charge des contenus textuels courants.

Composants principaux :

- `RestClientLoggingInterceptor`
- `OutboundHttpLoggingProperties`
- `AnonymizeProperties`

## Types de contenu pris en charge

Les bodies sont loggés en clair uniquement pour les contenus textuels/loggables :

- `application/json`
- `application/xml`
- `application/x-www-form-urlencoded`
- `text/*`

Pour les contenus non textuels, le projet logge uniquement le type et la taille du payload.

## Installation

### Prérequis

- Java **25**
- Gradle
- Spring Boot **4.1.0**

### Dépendances principales

Le projet s'appuie notamment sur :

- `spring-boot-starter-webmvc`
- `spring-boot-starter-restclient`
- Lombok

## Configuration

### Logging entrant

```yaml application.yml
logging:
  inbound:
    enabled: true
    included-paths:
      - /api/**
    log-headers: true
    log-request-body: true
    log-response-body: true
    max-body-log-bytes: 10240
```

### Anonymisation

```yaml application.yml
logging:
  anonymize:
    headers:
      - Authorization
      - Cookie
      - X-Api-Key
    body:
      - password
      - token
      - iban
      - secret
    anonymized-string: "***"
```

### Logging sortant

```yaml application.yml
logging:
  outbound:
    log-headers: true
    log-request-body: true
    log-response-body: true
    max-body-log-bytes: 10240
```

## Utilisation

### 1. Logger un endpoint REST entrant

Annoter explicitement les méthodes Spring MVC à tracer :

```java example
import fr.cgtlabs.springboot.logging.http.inbound.LoggedRestEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserController {

    @LoggedRestEndpoint
    @PostMapping("/search")
    ResponseEntity<String> search(@RequestBody String body) {
        return ResponseEntity.ok(body);
    }
}
```

Notes :

- le logging entrant ne s'active que si `logging.inbound.enabled=true` ;
- au moins un chemin doit être présent dans `logging.inbound.included-paths` ;
- seuls les endpoints annotés avec `@LoggedRestEndpoint` sont effectivement loggés.

### 2. Brancher le logging sortant sur un `RestClient`

L'interceptor sortant est créé via `RestClientLoggingInterceptorFactory` afin d'associer explicitement un nom d'appelant à chaque client.

```java example
import fr.cgtlabs.springboot.logging.http.outbound.RestClientLoggingInterceptorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class RestClientConfiguration {

    @Bean
    RestClient customerRestClient(RestClient.Builder builder,
                                  RestClientLoggingInterceptorFactory interceptorFactory) {
        return builder
                .requestInterceptor(interceptorFactory.create("CustomerApiClient"))
                .build();
    }
}
```

## Exemple de logs produits

### Entrant

```text logs
====================================================================================================
[UserController#search] HTTP inbound
------------------------------------------------------------
[UserController#search] → Requête entrante
  Handler  : UserController#search
  Méthode  : POST
  URI      : /api/users/search
  Headers  : Authorization: ***
Content-Type: application/json
[UserController#search] -> Body (requête, type=application/json :
{"login":"john.doe","password":"***"}

------------------------------------------------------------
[UserController#search] ← Réponse envoyée
  Handler  : UserController#search
  Statut   : 200
  Durée    : 18 ms
  URI      : /api/users/search
[UserController#search] -> Body (réponse, type=application/json :
{"status":"ok","token":"***"}
====================================================================================================
```

### Sortant

```text logs
====================================================================================================
[CustomerApiClient] HTTP outbound
------------------------------------------------------------
[CustomerApiClient] → Requête sortante
  Méthode  : POST
  URI      : https://example.test/customers
  Headers  : Authorization: ***
Content-Type: application/json
Body (requête, type=application/json :
{"customerId":"12345","secret":"***"}

------------------------------------------------------------
[CustomerApiClient] ← Réponse reçue
  Statut   : 200
  Durée    : 37 ms
  URI      : https://example.test/customers
Body (réponse, type=application/json :
{"status":"ok","secret":"***"}
====================================================================================================
```

## Auto-configuration

L'auto-configuration Spring Boot enregistre actuellement l'infrastructure de logging **entrant** via `ApiLoggingAutoConfiguration`.

Le fichier d'import Spring Boot est présent dans :

- `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

## Limites et points d'attention

- le masquage des bodies repose sur une regex ;
- les bodies binaires ne sont pas affichés ;
- les bodies volumineux sont tronqués ;
- le logging entrant repose sur des wrappers `ContentCaching*` ;
- le logging sortant lit le body de réponse pour le journaliser : ce point doit être validé selon votre usage ;
- le logging entrant et sortant produit désormais un bloc unique par échange HTTP, ce qui améliore la lisibilité mais regroupe requête et réponse dans un même message.

## Tests

Le projet contient des tests ciblant les deux mécanismes :

- `InboundHttpLoggingFilterTest`
- `RestClientLoggingInterceptorTest`

Lancer les tests :

```sh
./gradlew test
```

## Structure du projet

```text
src/main/java/fr/cgtlabs/springboot/logging/
├── autoconfigure/
├── http/inbound/
├── http/outbound/
├── properties/
└── utils/
```

## Résumé

Ce starter fournit une base de logging HTTP réutilisable pour Spring Boot avec :

- un **logging entrant opt-in** pour les contrôleurs Spring MVC ;
- un **logging sortant** pour `RestClient` ;
- une **configuration centralisée** ;
- une **anonymisation intégrée** pour limiter l'exposition des données sensibles.
