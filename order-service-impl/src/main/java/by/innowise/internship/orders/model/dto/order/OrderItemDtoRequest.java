package by.innowise.internship.orders.model.dto.order;

public record OrderItemDtoRequest(
        Long itemId,
        Integer quantity
) {
}
