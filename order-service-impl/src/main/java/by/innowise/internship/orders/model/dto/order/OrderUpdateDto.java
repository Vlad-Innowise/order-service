package by.innowise.internship.orders.model.dto.order;

import by.innowise.internship.orders.model.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderUpdateDto(
        UUID id,
        LocalDateTime creationDate,
        List<OrderItemDtoRequest> items,
        OrderStatus status
) {
}
