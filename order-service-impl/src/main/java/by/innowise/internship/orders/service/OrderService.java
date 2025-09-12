package by.innowise.internship.orders.service;

import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;

import java.util.UUID;

public interface OrderService {

    OrderResponseDto create(OrderCreateDto createDto, Long userId);

    OrderResponseDto getById(UUID id, Long userId);
}
