package by.innowise.internship.orders.kafka.listener;

import by.innowise.common.library.kafka.KafkaTopics;
import by.innowise.common.library.kafka.event.PaymentCreatedEvent;
import by.innowise.common.library.kafka.event.PaymentEventStatus;
import by.innowise.internship.orders.config.KafkaConfig;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentListener {

    private final OrderService orderService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_CREATED_TOPIC, groupId = KafkaConfig.KAFKA_ORDER_CONSUMER_GROUP)
    public void processEvent(PaymentCreatedEvent event, Acknowledgment ack) {

        log.info("Received payment-created event: {}", event);
        try {
            OrderStatus newStatus = convertPaymentEventStatusToOrderStatus(event.status());
            OrderResponseDto orderResponse = orderService.updateStatus(event.orderId(), event.userId(), newStatus);
            log.info("Order was successfully updated with new status: {}", orderResponse);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment-created-event: {}", event, e);
        }
    }

    public OrderStatus convertPaymentEventStatusToOrderStatus(PaymentEventStatus status) {
        return isPaymentSucceed(status) ? OrderStatus.FINISHED : OrderStatus.PENDING;
    }

    private boolean isPaymentSucceed(PaymentEventStatus status) {
        return PaymentEventStatus.SUCCEED == status;
    }
}
