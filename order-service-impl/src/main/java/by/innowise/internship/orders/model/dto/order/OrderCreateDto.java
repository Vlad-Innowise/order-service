package by.innowise.internship.orders.model.dto.order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreateDto(
        LocalDateTime creationDate,
        List<OrderItemDtoRequest> items
) {
}
