package org.example.trafficbilling.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    private static final String TOPIC = "traffic-log";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendLog(String userId, String apiId) {
        String message = String.format("User:%s API:%s Timestamp:%d", userId, apiId, System.currentTimeMillis());
        kafkaTemplate.send(TOPIC, message);
    }
}
