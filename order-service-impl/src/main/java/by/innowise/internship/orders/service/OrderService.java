package by.innowise.internship.orders.service;

import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;

public interface OrderService {

    OrderResponseDto create(OrderCreateDto createDto, Long userId);
}
