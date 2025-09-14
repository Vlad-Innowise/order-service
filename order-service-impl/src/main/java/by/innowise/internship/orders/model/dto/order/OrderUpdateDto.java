package by.innowise.internship.orders.model.dto.order;

import by.innowise.internship.orders.model.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderUpdateDto(
        @NotNull(message = "Order id can't be null")
        UUID id,

        @NotNull(message = "Order creation date can't be null")
        LocalDateTime creationDate,

        @NotEmpty(message = "Order can't be empty. It should contain order items!")
        List<@Valid OrderItemDtoRequest> items,

        @NotNull(message = "Order status can't be null")
        OrderStatus status
) {
}
