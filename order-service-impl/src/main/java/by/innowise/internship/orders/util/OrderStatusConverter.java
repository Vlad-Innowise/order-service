package by.innowise.internship.orders.util;

import by.innowise.internship.orders.model.entity.OrderStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusConverter implements Converter<String, OrderStatus> {

    @Override
    public OrderStatus convert(String rawStatus) {
        return OrderStatus.fromString(rawStatus);
    }
}
