package by.innowise.internship.orders.mapper;

import by.innowise.internship.orders.model.dto.order.OrderItemDtoRequest;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoResponse;
import by.innowise.internship.orders.model.entity.OrderItem;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(config = BaseMapper.class)
public interface OrderItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderItem toEntity(OrderItemDtoRequest dto, @Context ItemSnapshot itemSnapshot);

    @AfterMapping
    default void finishEntityMapping(@MappingTarget OrderItem orderItem,
                                     @Context ItemSnapshot itemSnapshot) {
        if (orderItem != null) {
            orderItem.setId(UUID.randomUUID());
            orderItem.setItem(itemSnapshot);
        }
    }

    @Mapping(source = "item.itemId", target = "itemId")
    @Mapping(source = "item.itemName", target = "itemName")
    @Mapping(source = "item.itemPrice", target = "itemPrice")
    @Mapping(target = "subTotal", expression = "java(subtotal)")
    OrderItemDtoResponse toResponseDto(OrderItem e, @Context BigDecimal subtotal);

}
