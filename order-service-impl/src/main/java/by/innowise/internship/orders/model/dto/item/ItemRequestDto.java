package by.innowise.internship.orders.model.dto.item;

import by.innowise.internship.orders.validation.OnCreate;
import by.innowise.internship.orders.validation.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemRequestDto(
        @Null(message = "Item id should be null", groups = {OnCreate.class})
        @NotNull(message = "Item id can't be null", groups = {OnUpdate.class})
        @Positive(message = "Item id can't be negative or zero", groups = {OnUpdate.class})
        Long id,

        @NotBlank(message = "Item name can't be blank", groups = {OnCreate.class, OnUpdate.class})
        @Size(max = 255, message = "Item name can't exceed 255 symbols", groups = {OnCreate.class, OnUpdate.class})
        String name,

        @NotNull(message = "Item price can't be null", groups = {OnCreate.class, OnUpdate.class})
        @Positive(message = "Item price can't negative or zero", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal price
) {
}
