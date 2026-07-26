package fr.cgtlabs.springboot.logging.http.inbound;

import fr.cgtlabs.springboot.logging.http.inbound.LoggedRestEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/logged-get")
    @LoggedRestEndpoint
    public ResponseEntity<String> loggedGet() {
        return ResponseEntity.ok("Logged GET response");
    }

    @PostMapping("/logged-post")
    @LoggedRestEndpoint
    public ResponseEntity<Map<String, String>> loggedPost(@RequestBody Map<String, String> requestBody) {
        return ResponseEntity.ok(Map.of("message", "Logged POST received", "data", requestBody.get("data")));
    }

    @GetMapping("/unlogged-get")
    public ResponseEntity<String> unloggedGet() {
        return ResponseEntity.ok("Unlogged GET response");
    }
}
