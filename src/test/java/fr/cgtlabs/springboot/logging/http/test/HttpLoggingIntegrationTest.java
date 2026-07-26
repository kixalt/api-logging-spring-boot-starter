package fr.cgtlabs.springboot.logging.http.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test") // Active le profil 'test' pour charger application-test.yml
public class HttpLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLoggedGetRequest(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/logged-get"))
                .andExpect(status().isOk());

        assertThat(output).contains("→ Inbound Request");
        assertThat(output).contains("Method   : GET");
        assertThat(output).contains("URI      : /test/logged-get");
        assertThat(output).contains("← Response Sent");
        assertThat(output).contains("Status   : 200");
        assertThat(output).contains("Body (response, type=text/plain;charset=UTF-8 :");
        assertThat(output).contains("Logged GET response");
    }

    @Test
    void testLoggedPostRequest(CapturedOutput output) throws Exception {
        String requestBodyJson = "{\"data\":\"some data\",\"secret\":\"my_secret_value\"}";

        mockMvc.perform(post("/test/logged-post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isOk());

        assertThat(output).contains("→ Inbound Request");
        assertThat(output).contains("Method   : POST");
        assertThat(output).contains("URI      : /test/logged-post");
        assertThat(output).contains("Body (request, type=application/json :");
        assertThat(output).contains("\"data\":\"some data\"");
        assertThat(output).contains("\"secret\":\"my_secret_value\""); // Should be logged as is without anonymization properties
        assertThat(output).contains("← Response Sent");
        assertThat(output).contains("Status   : 200");
        assertThat(output).contains("Body (response, type=application/json :");
        assertThat(output).contains("\"message\":\"Logged POST received\"");
    }

    @Test
    void testUnloggedGetRequest(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/unlogged-get"))
                .andExpect(status().isOk());

        assertThat(output).doesNotContain("→ Inbound Request");
        assertThat(output).doesNotContain("Unlogged GET response");
    }
}
