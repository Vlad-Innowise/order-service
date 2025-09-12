package by.innowise.internship.orders.service.impl;

import by.innowise.internship.orders.exception.ItemNotFoundException;
import by.innowise.internship.orders.exception.NotUniqueOrderItemException;
import by.innowise.internship.orders.mapper.OrderItemMapper;
import by.innowise.internship.orders.mapper.OrderMapper;
import by.innowise.internship.orders.model.dto.UserProfileDto;
import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoRequest;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoResponse;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.entity.Order;
import by.innowise.internship.orders.model.entity.OrderItem;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.repository.OrderRepository;
import by.innowise.internship.orders.service.OrderCalculator;
import by.innowise.internship.orders.service.OrderService;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import by.innowise.internship.orders.service.facade.ItemFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderCalculator orderCalculator;
    private final ItemFacade itemFacade;

    @Override
    public OrderResponseDto create(OrderCreateDto createDto, Long userId) {
        log.info("Creating order: {} for userId: {}", createDto, userId);
        Order toSave = orderMapper.toEntity(createDto, userId);
        log.info("Mapped to order entity: {}", toSave);
        convertAndAssignOrderItems(createDto, toSave);
        log.info("Added order items:{}, to order id: {}, items: {}", toSave.getOrderItems().size(), toSave.getId(),
                 toSave.getOrderItems());
        toSave.setStatus(OrderStatus.PENDING);
        repository.saveAndFlush(toSave);
        log.info("Order: {} pre-saved with status: {}", toSave.getId(), toSave.getStatus());
        return calculateTotalsAndGetOrderResponse(toSave);
    }

    private OrderResponseDto calculateTotalsAndGetOrderResponse(Order order) {
        List<OrderItemDtoResponse> calculatedOrderItemResponses = getOrderItemResponses(order);
        BigDecimal orderTotal = orderCalculator.calculateOrderTotal(order);
        return orderMapper.toDto(order,
                                 new UserProfileDto(order.getUserId()),
                                 calculatedOrderItemResponses,
                                 orderTotal);
    }

    private List<OrderItemDtoResponse> getOrderItemResponses(Order order) {
        Map<Long, BigDecimal> subtotalsByItemId = orderCalculator.calculateSubtotals(order);
        return order.getOrderItems()
                    .stream()
                    .map(oi -> mapToOrderItemResponse(oi, subtotalsByItemId))
                    .toList();
    }

    private OrderItemDtoResponse mapToOrderItemResponse(OrderItem oi, Map<Long, BigDecimal> subtotalsByItemId) {
        return orderItemMapper.toResponseDto(oi, subtotalsByItemId.get(oi.getItem().getItemId()));
    }

    private void convertAndAssignOrderItems(OrderCreateDto createDto, Order order) {
        Map<Long, ItemSnapshot> existingItems = validateAndGetItemsByIds(createDto);
        createDto.items()
                 .forEach(orderItemDto -> {
                     OrderItem orderItem = convertToOrderItem(orderItemDto, existingItems);
                     order.addOrderItem(orderItem);
                 });
    }

    private Map<Long, ItemSnapshot> validateAndGetItemsByIds(OrderCreateDto createDto) {
        Set<Long> incomingItemIds = getUniqueIncomingItemIds(createDto);
        checkIfNoDuplicatedOrderItems(createDto, incomingItemIds);
        Set<ItemSnapshot> items = itemFacade.getByIds(incomingItemIds);
        Map<Long, ItemSnapshot> existingItems = getSnapshotsMap(items);
        checkIfAllItemsWereFound(incomingItemIds, existingItems);
        return existingItems;
    }

    private Set<Long> getUniqueIncomingItemIds(OrderCreateDto createDto) {
        return createDto.items().stream()
                        .map(OrderItemDtoRequest::itemId)
                        .collect(Collectors.toSet());
    }

    private void checkIfAllItemsWereFound(Set<Long> incomingItemIds, Map<Long, ItemSnapshot> existingItems) {
        List<Long> notFoundItemIds = incomingItemIds.stream()
                                                    .filter(itemId -> !existingItems.containsKey(itemId))
                                                    .toList();
        if (!notFoundItemIds.isEmpty()) {
            throw new ItemNotFoundException("%s item ids were not found in DB".formatted(notFoundItemIds),
                                            HttpStatus.BAD_REQUEST);
        }
    }

    private void checkIfNoDuplicatedOrderItems(OrderCreateDto createDto, Set<Long> incomingItemIds) {
        if (isNotUniqueOrderItems(incomingItemIds, createDto.items())) {
            throw new NotUniqueOrderItemException(
                    "The provided order item dto list contains duplicated order items: %s".formatted(createDto.items()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isNotUniqueOrderItems(Set<Long> incomingItemIds, List<OrderItemDtoRequest> orderItemDtos) {
        List<Long> initialItemIds = orderItemDtos.stream()
                                                 .map(OrderItemDtoRequest::itemId)
                                                 .toList();
        return !incomingItemIds.containsAll(initialItemIds) || incomingItemIds.size() != initialItemIds.size();
    }

    private OrderItem convertToOrderItem(OrderItemDtoRequest itemDto, Map<Long, ItemSnapshot> existingItems) {
        Long itemId = itemDto.itemId();
        return Optional.ofNullable(existingItems.get(itemId))
                       .map(itemSnap ->
                                    orderItemMapper.toEntity(itemDto, itemSnap))
                       .orElseThrow(
                               () -> new IllegalStateException(
                                       "Not found the item with id: [%s]".formatted(itemId))
                       );
    }

    private Map<Long, ItemSnapshot> getSnapshotsMap(Set<ItemSnapshot> itemSnaphots) {
        return itemSnaphots.stream()
                           .collect(Collectors.toMap(ItemSnapshot::getItemId, Function.identity()));
    }
}
