package by.innowise.internship.orders.service;

import by.innowise.internship.orders.model.entity.Order;
import by.innowise.internship.orders.model.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderCalculator {

    public Map<Long, BigDecimal> calculateSubtotals(Order order) {
        return order.getOrderItems()
                    .stream()
                    .collect(Collectors.toMap(
                            oi -> oi.getItem().getItemId(),
                            this::calculateSubtotal,
                            BigDecimal::add
                    ));
    }

    public BigDecimal calculateSubtotal(OrderItem oi) {
        return oi.getItem().getItemPrice()
                 .multiply(BigDecimal.valueOf(oi.getQuantity()));
    }

    public BigDecimal calculateOrderTotal(Order order) {
        return order.getOrderItems()
                    .stream()
                    .map(this::calculateSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


}
