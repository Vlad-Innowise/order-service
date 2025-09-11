package by.innowise.internship.orders.model.dto.item;

import java.math.BigDecimal;

public record ItemRequestDto(
        Long id,
        String name,
        BigDecimal price
) {
}
