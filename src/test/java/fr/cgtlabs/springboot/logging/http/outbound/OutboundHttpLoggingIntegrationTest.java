package fr.cgtlabs.springboot.logging.http.outbound;

import fr.cgtlabs.springboot.logging.properties.AnonymizeProperties;
import fr.cgtlabs.springboot.logging.properties.OutboundHttpLoggingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
public class OutboundHttpLoggingIntegrationTest {

    private RestClient.Builder restClient;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // Configure AnonymizeProperties
        AnonymizeProperties anonymizeProperties = new AnonymizeProperties();
        anonymizeProperties.setHeaders(new String[]{"Authorization", "X-Secret-Header"});
        anonymizeProperties.setBody(new String[]{"password", "secret"});

        // Configure OutboundHttpLoggingProperties
        OutboundHttpLoggingProperties loggingProperties = new OutboundHttpLoggingProperties();
        loggingProperties.setLogRequestBody(true);
        loggingProperties.setMaxBodyLogBytes(1000);

        // Create the interceptor
        var factory = new RestClientLoggingInterceptorFactory(anonymizeProperties, loggingProperties);
        RestClientLoggingInterceptor loggingInterceptor = factory.create("my-method");

        // Build RestClient with the interceptor
        restClient = RestClient.builder()
                .baseUrl("http://localhost:8080")
                .requestInterceptor(loggingInterceptor);

        // Create MockRestServiceServer for the RestClient
        mockServer = MockRestServiceServer.createServer(restClient);
    }

    @Test
    void testLoggedOutboundGetRequest(CapturedOutput output) {
        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8080/api/resource"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess("response body", MediaType.TEXT_PLAIN));

        restClient.build().get()
                .uri("/api/resource")
                .header("Authorization", "Bearer token")
                .retrieve()
                .toBodilessEntity();

        mockServer.verify();

        assertThat(output).contains("→ Outbound Request");
        assertThat(output).contains("Caller   : my-method");
        assertThat(output).contains("Method   : GET");
        assertThat(output).contains("URI      : http://localhost:8080/api/resource");
        assertThat(output).contains("Authorization: ***"); // Anonymized header
        assertThat(output).contains("← Response Received");
        assertThat(output).contains("Status   : 200");
        assertThat(output).contains("Body (response, type=text/plain :");
        assertThat(output).contains("response body");
        assertThat(output).contains("Duration : ");
    }

    @Test
    void testLoggedOutboundPostRequestWithBody(CapturedOutput output) {
        String requestBody = """
                {"name":"test","password":"secret_password"}""";
        String responseBody = """
                {"status":"success","data":"some data"}""";

        mockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8080/api/data"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().json(requestBody))
                .andRespond(MockRestResponseCreators.withSuccess(responseBody, MediaType.APPLICATION_JSON));

        restClient.build().post()
                .uri("/api/data")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Secret-Header", "my-secret")
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();

        mockServer.verify();

        assertThat(output).contains("→ Outbound Request");
        assertThat(output).contains("Caller   : my-method");
        assertThat(output).contains("Method   : POST");
        assertThat(output).contains("URI      : http://localhost:8080/api/data");
        assertThat(output).contains("X-Secret-Header: ***"); // Anonymized header
        assertThat(output).contains("Body (request, type=application/json :");
        assertThat(output).contains("""
                {"name":"test","password":"***"}"""); // Anonymized body field
        assertThat(output).contains("← Response Received");
        assertThat(output).contains("Status   : 200");
        assertThat(output).contains("Body (response, type=application/json :");
        assertThat(output).contains(responseBody);
        assertThat(output).contains("Duration : ");
    }

    @Test
    void testOutboundRequestWithoutBodyLogging(CapturedOutput output) {
        OutboundHttpLoggingProperties noBodyLoggingProperties = new OutboundHttpLoggingProperties();
        noBodyLoggingProperties.setLogRequestBody(false); // Disable body logging
        noBodyLoggingProperties.setLogResponseBody(false);

        var factory = new RestClientLoggingInterceptorFactory(new AnonymizeProperties(), noBodyLoggingProperties);
        RestClientLoggingInterceptor noBodyLoggingInterceptor = factory.create("no-body-caller");

        RestClient.Builder noBodyRestClient = RestClient.builder()
                .baseUrl("http://localhost:8080")
                .requestInterceptor(noBodyLoggingInterceptor);

        MockRestServiceServer noBodyMockServer = MockRestServiceServer.createServer(noBodyRestClient);

        String requestBody = "{\"data\":\"some data\"}";
        String responseBody = "{\"result\":\"ok\"}";

        noBodyMockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8080/api/no-body"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(responseBody, MediaType.APPLICATION_JSON));

        noBodyRestClient.build().post()
                .uri("/api/no-body")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();

        noBodyMockServer.verify();

        assertThat(output).contains("→ Outbound Request");
        assertThat(output).contains("Caller   : no-body-caller");
        assertThat(output).contains("Method   : POST");
        assertThat(output).contains("URI      : http://localhost:8080/api/no-body");
        assertThat(output).doesNotContain("Body (request"); // Should not log request body
        assertThat(output).contains("← Response Received");
        assertThat(output).contains("Status   : 200");
        assertThat(output).doesNotContain("Body (response"); // Should not log response body
    }

    @Test
    void testOutboundRequestWithMaxBodySizeExceeded(CapturedOutput output) {
        // Configure OutboundHttpLoggingProperties with a small max body size
        OutboundHttpLoggingProperties smallBodySizeProperties = new OutboundHttpLoggingProperties();
        smallBodySizeProperties.setLogRequestBody(true); // Disable body logging
        smallBodySizeProperties.setLogResponseBody(true);
        smallBodySizeProperties.setMaxBodyLogBytes(20); // Max body size of 20 bytes to ensure truncation

        RestClientLoggingInterceptor smallBodySizeInterceptor = new RestClientLoggingInterceptor("max-body-caller", new AnonymizeProperties(), smallBodySizeProperties);

        RestClient.Builder smallBodySizeRestClient = RestClient.builder()
                .baseUrl("http://localhost:8080")
                .requestInterceptor(smallBodySizeInterceptor);

        MockRestServiceServer smallBodySizeMockServer = MockRestServiceServer.createServer(smallBodySizeRestClient);

        String requestBody = "{\"data\":\"this is a long request body\"}"; // Length ~34 bytes
        String responseBody = "{\"result\":\"this is a long response body\"}"; // Length ~36 bytes

        smallBodySizeMockServer.expect(MockRestRequestMatchers.requestTo("http://localhost:8080/api/large-body"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(responseBody, MediaType.APPLICATION_JSON));

        smallBodySizeRestClient.build().post()
                .uri("/api/large-body")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();

        smallBodySizeMockServer.verify();

        assertThat(output).contains("→ Outbound Request");
        assertThat(output).contains("Caller   : max-body-caller");
        assertThat(output).contains("Body (request, type=application/json :");
        assertThat(output).contains("← Response Received");
        assertThat(output).contains("Body (response, type=application/json :");
        assertThat(output).contains("truncated to 0 KB"); // Assertion updated for 20 bytes / 1024 = 0 KB
    }
}