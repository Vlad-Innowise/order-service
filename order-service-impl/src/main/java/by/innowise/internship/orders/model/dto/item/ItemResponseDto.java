package by.innowise.internship.orders.model.dto.item;

import java.math.BigDecimal;

public record ItemResponseDto(
        Long id,
        String name,
        BigDecimal price,
        Long version
) {
}
