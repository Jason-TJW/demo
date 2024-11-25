package org.example.trafficbilling.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    @KafkaListener(topics = "traffic-log", groupId = "traffic-group")
    public void consume(String message) {
        // 解析消息并进行实时统计
        System.out.println("Consumed message: " + message);
        // 实现具体的统计逻辑，如存储到数据库、实时分析等
    }
}
