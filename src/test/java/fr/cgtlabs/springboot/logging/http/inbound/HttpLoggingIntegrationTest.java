package fr.cgtlabs.springboot.logging.http.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test")
class HttpLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLoggedGetRequest(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/logged-get").headers(
                        httpHeaders -> httpHeaders.add("authentication", "Bearer secret-key")
                ))
                .andExpect(status().isOk());

        assertThat(output).contains("→ Inbound Request")
                .contains("Method   : GET")
                .contains("URI      : /test/logged-get")
                .contains("authentication: ***")
                .contains("← Response Sent")
                .contains("Status   : 200")
                .contains("Body (response, type=text/plain;charset=UTF-8 :")
                .contains("Logged GET response");
    }

    @Test
    void testLoggedPostRequest(CapturedOutput output) throws Exception {
        String requestBodyJson = """
                                {
                  "data": "some data",
                  "secret": "my_secret_value"
                }""";

        mockMvc.perform(post("/test/logged-post").
                        headers(
                                httpHeaders -> httpHeaders.add("authentication", "Bearer secret-key"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isOk());

        assertThat(output).contains("→ Inbound Request")
                .contains("Method   : POST")
                .contains("URI      : /test/logged-post")
                .contains("authentication: ***")
                .contains("Body (request, type=application/json :")
                .contains(requestBodyJson.replace("my_secret_value", "***"))
                .contains("← Response Sent")
                .contains("Status   : 200")
                .contains("Body (response, type=application/json :")
                .contains("Logged POST received");
    }

    @Test
    void testUnloggedGetRequest(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/unlogged-get"))
                .andExpect(status().isOk());

        assertThat(output).doesNotContain("→ Inbound Request")
                .doesNotContain("← Response Sent");
    }
}
