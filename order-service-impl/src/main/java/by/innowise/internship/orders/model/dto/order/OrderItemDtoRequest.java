package by.innowise.internship.orders.model.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemDtoRequest(
        @NotNull(message = "Item id can't be null")
        @Positive(message = "Item id can't be negative or zero")
        Long itemId,

        @NotNull(message = "Quantity can't be null")
        @Positive(message = "Quantity can't be negative or zero")
        Integer quantity
) {
}
