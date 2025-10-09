package by.innowise.internship.orders.kafka.publisher;

import by.innowise.common.library.kafka.KafkaTopics;
import by.innowise.common.library.kafka.event.OrderCreatedEvent;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderService orderService;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("Requested to send order-created event: {}", event);
            orderService.updateStatus(event.orderId(), event.userId(), OrderStatus.PROCESSING);
            String key = event.orderId().toString();
            kafkaTemplate.send(KafkaTopics.ORDER_CREATED_TOPIC, key, event);
            log.info("Order created event was send with key: {}", key);
        } catch (Exception e) {
            log.error("Failed to publish event: {} to topic: [{}]", event, KafkaTopics.ORDER_CREATED_TOPIC, e);
        }
    }

}
