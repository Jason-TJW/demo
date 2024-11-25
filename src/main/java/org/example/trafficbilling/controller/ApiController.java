package org.example.trafficbilling.controller;

import org.example.trafficbilling.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/traffic")
public class ApiController {
    @Autowired
    private RateLimiterService rateLimiterService;

    @GetMapping("/api1")
    public ResponseEntity<String> handleApi1(@RequestHeader("userId") String userId) {
        return processRequest(userId, "api1");
    }

    @PostMapping("/api2")
    public ResponseEntity<String> handleApi2(@RequestHeader("userId") String userId) {
        return processRequest(userId, "api2");
    }

    @PutMapping("/api3")
    public ResponseEntity<String> handleApi3(@RequestHeader("userId") String userId) {
        return processRequest(userId, "api3");
    }

    private ResponseEntity<String> processRequest(String userId, String apiId) {
        if (!rateLimiterService.isAllowed(userId, apiId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Try again later.");
        }
        //rateLimiterService.logRequestAsync(userId, apiId);
        return ResponseEntity.ok(apiId + " request processed successfully!");
    }
}
