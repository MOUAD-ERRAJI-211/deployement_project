package orthoproconnect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class TestController {

    @GetMapping("/api/test")
    public ResponseEntity<?> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "API connection successful");
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/api/test", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptionsForTest() {
        return ResponseEntity.ok().build();
    }
}