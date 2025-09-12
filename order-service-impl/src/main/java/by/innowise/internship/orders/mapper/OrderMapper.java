package by.innowise.internship.orders.mapper;

import by.innowise.internship.orders.model.dto.UserProfileDto;
import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoResponse;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.entity.Order;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper(config = BaseMapper.class)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    Order toEntity(OrderCreateDto d, @Context Long userId);

    @AfterMapping
    default Order mapAdditionalProperties(@MappingTarget Order order, @Context Long userId) {
        if (order != null) {
            order.setId(UUID.randomUUID());
            order.setUserId(userId);
        }
        return order;
    }

    @Mapping(target = "user", expression = "java(user)")
    @Mapping(target = "orderItems", expression = "java(orderItems)")
    @Mapping(target = "total", expression = "java(total)")
    OrderResponseDto toDto(Order order,
                           @Context UserProfileDto user,
                           @Context List<OrderItemDtoResponse> orderItems,
                           @Context BigDecimal total);

}
