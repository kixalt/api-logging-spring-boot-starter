package fr.cgtlabs.springboot.logging.http.outbound;

import fr.cgtlabs.springboot.logging.http.common.Direction;
import fr.cgtlabs.springboot.logging.http.common.HttpExchangeInfoProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

/**
 * Implementation of {@link HttpExchangeInfoProvider} for outbound HTTP requests.
 * This class extracts the necessary logging information from
 * {@link HttpRequest} and {@link ClientHttpResponse} objects.
 */
public class OutboundHttpExchangeInfoProvider implements HttpExchangeInfoProvider {

    private final HttpRequest request;
    private final byte[] requestBody;
    private final ClientHttpResponse response;
    private final String callerName;
    private final long elapsedTime;

    /**
     * Constructs a new instance of {@code OutboundHttpExchangeInfoProvider}.
     *
     * @param request     The outbound HTTP request.
     * @param requestBody The body of the outbound request.
     * @param response    The received HTTP response.
     * @param callerName  The name of the caller.
     * @param elapsedTime The processing time of the request in milliseconds.
     */
    public OutboundHttpExchangeInfoProvider(HttpRequest request, byte[] requestBody, ClientHttpResponse response, String callerName, long elapsedTime) {
        this.request = request;
        this.requestBody = requestBody;
        this.response = response;
        this.callerName = callerName;
        this.elapsedTime = elapsedTime;
    }

    @Override
    public Direction getDirection() {
        return Direction.OUTBOUND;
    }

    @Override
    public String getExchangeDescriptor() {
        return callerName;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return request.getMethod();
    }

    @Override
    public URI getUri() {
        return request.getURI();
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        return request.getHeaders();
    }

    @Override
    public byte[] getRequestBody() {
        return requestBody;
    }

    @Override
    public int getResponseStatus() {
        try {
            return response.getStatusCode().value();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public HttpHeaders getResponseHeaders() {
        return response.getHeaders();
    }

    @Override
    public byte[] getResponseBody() throws IOException {
        return response.getBody().readAllBytes();
    }

    @Override
    public long getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public MediaType getRequestContentType() {
        return request.getHeaders().getContentType();
    }

    @Override
    public MediaType getResponseContentType() {
        return response.getHeaders().getContentType();
    }
}