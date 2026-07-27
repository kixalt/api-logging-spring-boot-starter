package fr.cgtlabs.springboot.logging.http.outbound;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Simple {@link ClientHttpResponse} wrapper that buffers the response body
 * and returns a fresh {@link InputStream} on each {@link #getBody()} call.
 */
public class BufferedClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse clientHttpResponse;
    private byte[] body;

    public BufferedClientHttpResponse(ClientHttpResponse clientHttpResponse) throws IOException {
        this.clientHttpResponse = clientHttpResponse;
        this.body =  clientHttpResponse.getBody().readAllBytes();
    }

    @Override
    public HttpStatusCode getStatusCode() throws IOException {
        return clientHttpResponse.getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return clientHttpResponse.getStatusText();
    }

    @Override
    public void close() {
        clientHttpResponse.close();
    }

    @Override
    public InputStream getBody() throws IOException {
        return new ByteArrayInputStream(body);
    }

    @Override
    public HttpHeaders getHeaders() {
        return clientHttpResponse.getHeaders();
    }
}
