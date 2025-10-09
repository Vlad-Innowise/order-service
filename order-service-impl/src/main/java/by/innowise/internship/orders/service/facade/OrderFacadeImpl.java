package by.innowise.internship.orders.service.facade;

import by.innowise.common.library.kafka.event.OrderCreatedEvent;
import by.innowise.internship.orders.kafka.publisher.OrderPublisher;
import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.dto.order.OrderUpdateDto;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;
    private final OrderPublisher orderPublisher;

    @Override
    public OrderResponseDto create(OrderCreateDto createDto, Long userId) {
        OrderResponseDto orderResponse = orderService.create(createDto, userId);
        OrderCreatedEvent event = new OrderCreatedEvent(orderResponse.user().authId(), orderResponse.id(),
                                                        orderResponse.total());
        orderPublisher.publishOrderCreated(event);
        return orderResponse;
    }

    @Override
    public OrderResponseDto getById(UUID id, Long userId) {
        return orderService.getById(id, userId);
    }

    @Override
    public List<OrderResponseDto> getAllByIds(List<UUID> orderIds, Long userId, Pageable pageable) {
        return orderService.getAllByIds(orderIds, userId, pageable);
    }

    @Override
    public List<OrderResponseDto> getAllByStatus(Long userId, OrderStatus status) {
        return orderService.getAllByStatus(userId, status);
    }

    @Override
    public OrderResponseDto update(OrderUpdateDto updateDto, Long userId) {
        return orderService.update(updateDto, userId);
    }

    @Override
    public void delete(UUID orderId, Long userId) {
        orderService.delete(orderId, userId);
    }
}
