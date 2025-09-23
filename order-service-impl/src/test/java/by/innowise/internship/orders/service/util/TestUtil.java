package by.innowise.internship.orders.service.util;

import by.innowise.common.library.dto.UserProfileDto;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoRequest;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoResponse;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.entity.Order;
import by.innowise.internship.orders.model.entity.OrderItem;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class TestUtil {

    public Order getOrderWithoutOrderItems(UUID id, Long userId, OrderStatus status, LocalDateTime orderDate) {
        LocalDateTime created = LocalDateTime.now();
        return Order.builder()
                    .id(id)
                    .userId(userId)
                    .status(status)
                    .creationDate(orderDate)
                    .createdAt(created)
                    .updatedAt(created)
                    .version(0L)
                    .build();
    }

    public OrderItem getOrderItem(UUID id, ItemSnapshot item, Integer quantity) {
        LocalDateTime created = LocalDateTime.now();
        return OrderItem.builder()
                        .id(id)
                        .item(item)
                        .quantity(quantity)
                        .createdAt(created)
                        .updatedAt(created)
                        .version(0L)
                        .build();
    }

    public ItemSnapshot getItem(Long itemId, String itemName, BigDecimal itemPrice) {
        return ItemSnapshot.builder()
                           .itemId(itemId)
                           .itemName(itemName)
                           .itemPrice(itemPrice)
                           .build();
    }

    public OrderItemDtoRequest getOrderItemDtoRequest(Long itemId, Integer quantity) {
        return new OrderItemDtoRequest(itemId, quantity);
    }

    public UserProfileDto getUserProfile(Long userId, String name, String surname, LocalDate birthDate, String email) {
        return new UserProfileDto(userId, name, surname, birthDate, email);
    }

    public OrderItemDtoResponse mapToOrderItemResponse(OrderItem orderItem, BigDecimal orderItemSubtotal) {
        return new OrderItemDtoResponse(orderItem.getItem().getItemId(),
                                        orderItem.getItem().getItemName(),
                                        orderItem.getItem().getItemPrice(),
                                        orderItem.getQuantity(),
                                        orderItemSubtotal);
    }

    public OrderResponseDto mapToOrderResponseDto(Order order, UserProfileDto userProfile,
                                                  List<OrderItemDtoResponse> orderItems, BigDecimal orderTotal) {
        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getCreationDate(),
                orderItems,
                orderTotal,
                userProfile);
    }

}
