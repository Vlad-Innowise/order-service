package by.innowise.internship.orders.service.facade;

import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.dto.order.OrderUpdateDto;
import by.innowise.internship.orders.model.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderFacade {

    OrderResponseDto create(OrderCreateDto createDto, Long userId);

    OrderResponseDto getById(UUID id, Long userId);

    List<OrderResponseDto> getAll(Long userId);

    List<OrderResponseDto> getAllByIds(List<UUID> orderIds, Long userId, Pageable pageable);

    List<OrderResponseDto> getAllByStatus(Long userId, OrderStatus status);

    OrderResponseDto update(OrderUpdateDto updateDto, Long userId);

    OrderResponseDto updateStatus(UUID orderId, Long userId, OrderStatus newStatus);

    void delete(UUID orderId, Long userId);
}
