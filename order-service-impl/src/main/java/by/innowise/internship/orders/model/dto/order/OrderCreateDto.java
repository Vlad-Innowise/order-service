package by.innowise.internship.orders.model.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreateDto(
        @NotNull(message = "Order creation date can't be null")
        @FutureOrPresent(message = "Order creation date must be the current or the future date")
        LocalDateTime creationDate,

        @NotEmpty(message = "Order can't be empty. It should contain order items!")
        List<@Valid OrderItemDtoRequest> items
) {
}
