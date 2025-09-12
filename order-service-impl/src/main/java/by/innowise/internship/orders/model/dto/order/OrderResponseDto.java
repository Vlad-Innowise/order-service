package by.innowise.internship.orders.model.dto.order;

import by.innowise.internship.orders.model.UserProfileDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponseDto(
        UUID id,
        LocalDateTime creationDate,
        List<OrderItemDtoResponse> orderItems,
        BigDecimal total,
        UserProfileDto user
) {
}
