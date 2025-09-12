package by.innowise.internship.orders.model.dto.order;

import java.math.BigDecimal;

public record OrderItemDtoResponse(
        Long itemId,
        String itemName,
        BigDecimal itemPrice,
        Integer quantity,
        BigDecimal subTotal
) {
}
