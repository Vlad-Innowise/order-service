package by.innowise.internship.orders.service.impl;

import by.innowise.common.library.dto.UserProfileDto;
import by.innowise.common.library.exception.UserNotFoundException;
import by.innowise.internship.orders.exception.ItemNotFoundException;
import by.innowise.internship.orders.exception.NotUniqueOrderItemException;
import by.innowise.internship.orders.exception.OrderModificationDeniedException;
import by.innowise.internship.orders.exception.OrderNotFoundException;
import by.innowise.internship.orders.feign.UserServiceClient;
import by.innowise.internship.orders.mapper.OrderItemMapper;
import by.innowise.internship.orders.mapper.OrderMapper;
import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoRequest;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoResponse;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.dto.order.OrderUpdateDto;
import by.innowise.internship.orders.model.entity.Order;
import by.innowise.internship.orders.model.entity.OrderItem;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.repository.OrderRepository;
import by.innowise.internship.orders.service.OrderCalculator;
import by.innowise.internship.orders.service.OrderService;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import by.innowise.internship.orders.service.dto.ItemsDiffResult;
import by.innowise.internship.orders.service.facade.ItemFacade;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    private final UserServiceClient userServiceClient;

    @Override
    public OrderResponseDto create(OrderCreateDto createDto, Long userId) {
        UserProfileDto userProfileDto = retrieveUserProfile(userId);
        log.info("Creating order: {} for userId: {}", createDto, userId);
        Order toSave = orderMapper.toEntity(createDto, userId);
        log.info("Mapped to order entity: {}", toSave);
        convertAndAssignOrderItems(createDto, toSave);
        log.info("Added order items:{}, to order id: {}, items: {}", toSave.getOrderItems().size(), toSave.getId(),
                 toSave.getOrderItems());
        toSave.setStatus(OrderStatus.PENDING);
        repository.saveAndFlush(toSave);
        log.info("Order: {} pre-saved with status: {}", toSave.getId(), toSave.getStatus());
        return calculateTotalsAndGetOrderResponse(toSave, userProfileDto);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponseDto getById(UUID id, Long userId) {
        UserProfileDto userProfileDto = retrieveUserProfile(userId);
        Order found = getOrderByIdAndUserId(id, userId);
        return calculateTotalsAndGetOrderResponse(found, userProfileDto);
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrderResponseDto> getAllByIds(List<UUID> orderIds, Long userId, Pageable pageable) {
        UserProfileDto userProfileDto = retrieveUserProfile(userId);
        Set<UUID> idsToFind = new HashSet<>(orderIds);
        log.info("Invoking order repository for ids: [{}]", idsToFind);
        Page<Order> ordersPage = repository.findPageByIdsAndUserId(orderIds, userId, pageable);
        Set<UUID> retrievedOrdersId = ordersPage.getContent()
                                                .stream()
                                                .map(Order::getId)
                                                .collect(Collectors.toSet());

        List<UUID> missingOrderIds = idsToFind.stream()
                                              .filter(initId -> !retrievedOrdersId.contains(initId))
                                              .toList();

        log.warn("{} order ids were not found", missingOrderIds);
        //fetching order items only -> can ignore a result list
        repository.findByIdIn(retrievedOrdersId);
        log.info("Retrieved orders: {} for userId: {}, page {} out of {}", ordersPage.getContent().size(), userId,
                 ordersPage.getNumber() + 1, ordersPage.getTotalPages());
        return ordersPage.getContent()
                         .stream()
                         .map(order -> calculateTotalsAndGetOrderResponse(order, userProfileDto))
                         .toList();
    }

    @Override
    public List<OrderResponseDto> getAllByStatus(Long userId, OrderStatus status) {
        UserProfileDto userProfileDto = retrieveUserProfile(userId);
        log.info("Getting orders with status: {} for user id: [{}]", status.name(), userId);
        List<Order> foundOrders = repository.findAllByStatusAndUserIdFetchOrderItems(status, userId);
        Set<UUID> retrievedOrdersId = foundOrders.stream()
                                                 .map(Order::getId)
                                                 .collect(Collectors.toSet());
        log.info("Retrieved orders: {} for userId: {}", retrievedOrdersId, userId);
        return foundOrders.stream()
                          .map(order -> calculateTotalsAndGetOrderResponse(order, userProfileDto))
                          .toList();
    }

    @Transactional
    @Override
    public OrderResponseDto update(OrderUpdateDto updateDto, Long userId) {
        log.info("Updating order: {} for userId: {}", updateDto.id(), userId);

        UserProfileDto userProfileDto = retrieveUserProfile(userId);

        Order orderToUpdate = getOrderByIdAndUserId(updateDto.id(), userId);
        checkIfModificationAllowed(orderToUpdate);

        ItemsDiffResult preUpdateDiffResult = getItemsDiffResult(updateDto, orderToUpdate);

        createMissingOrderItems(updateDto, orderToUpdate, preUpdateDiffResult);
        updateExistingOrderItems(updateDto, orderToUpdate, preUpdateDiffResult);
        clearRemovedOrderItems(orderToUpdate, preUpdateDiffResult);

        Order updatedOrder = orderMapper.updateEntity(updateDto, orderToUpdate, userId);
        log.info("Merged order entity with order update dto: {}", updatedOrder);

        repository.saveAndFlush(updatedOrder);
        log.info("Updated order: {} pre-saved in DB", updatedOrder);
        return calculateTotalsAndGetOrderResponse(updatedOrder, userProfileDto);
    }

    @Override
    public OrderResponseDto updateStatus(UUID orderId, Long userId, OrderStatus newStatus) {
        log.info("Requested to update order: {} with status: {}", orderId, newStatus);
        Order order = getOrderByIdAndUserId(orderId, userId);
        checkIfModificationAllowed(order);
        order.setStatus(newStatus);
        repository.saveAndFlush(order);
        log.info("Updated order with new status: {} pre-saved in DB", order);
        UserProfileDto userProfileDto = retrieveUserProfile(userId);
        return calculateTotalsAndGetOrderResponse(order, userProfileDto);
    }

    @Transactional
    @Override
    public void delete(UUID orderId, Long userId) {
        log.info("Requested to delete the order with id {} for userid: {}", orderId, userId);
        Order toDelete = getOrderByIdAndUserId(orderId, userId);
        log.info("Invoking item repository to delete the order: {}", toDelete);
        checkIfModificationAllowed(toDelete);
        repository.delete(toDelete);
    }

    private ItemsDiffResult getItemsDiffResult(OrderUpdateDto updateDto, Order orderToUpdate) {
        Set<Long> incomingUniqueItemIds = getUniqueIncomingItemIds(updateDto.items());
        checkIfNoDuplicatedOrderItems(updateDto.items(), incomingUniqueItemIds);

        Set<Long> existingItemIds = orderToUpdate.getOrderItems()
                                                 .stream()
                                                 .map(oi -> oi.getItem().getItemId())
                                                 .collect(Collectors.toSet());

        ItemsDiffResult preUpdateDiffResult = new ItemsDiffResult(
                calculateItemIdsToAdd(updateDto, existingItemIds),
                calculateItemIdsToUpdate(updateDto, orderToUpdate),
                calculateItemIdsToRemove(orderToUpdate, incomingUniqueItemIds)
        );
        log.info("Calculated order modification result. Order items to add [{}], to update: [{}], to remove: [{}]",
                 preUpdateDiffResult.toAdd().size(),
                 preUpdateDiffResult.toUpdate().size(),
                 preUpdateDiffResult.toRemove().size());
        return preUpdateDiffResult;
    }

    private void createMissingOrderItems(OrderUpdateDto updateDto, Order toUpdate,
                                         ItemsDiffResult preUpdateDiffResult) {

        if (needToCreateAdditionalOrderItems(preUpdateDiffResult.toAdd())) {
            updateOrderWithAdditionalOrderItems(updateDto, toUpdate, preUpdateDiffResult.toAdd());
        }
    }

    private void updateExistingOrderItems(OrderUpdateDto updateDto, Order order, ItemsDiffResult preUpdateDiffResult) {
        if (needToUpdateExistingOrderItems(preUpdateDiffResult.toUpdate())) {
            updateExistingOrderItemsForOrder(updateDto, order, preUpdateDiffResult.toUpdate());
        }
    }

    private void clearRemovedOrderItems(Order order, ItemsDiffResult preUpdateDiffResult) {
        order.getOrderItems()
             .stream()
             .filter(oi -> preUpdateDiffResult.toRemove().contains(oi.getItem().getItemId()))
             .toList()
             .forEach(order::removeOrderItem);
    }

    private void updateExistingOrderItemsForOrder(OrderUpdateDto updateDto, Order order, Set<Long> itemsToUpdate) {
        Map<Long, Integer> incomingQuantityByItemId =
                updateDto.items()
                         .stream()
                         .collect(Collectors.toMap(OrderItemDtoRequest::itemId,
                                                   OrderItemDtoRequest::quantity)
                         );

        order.getOrderItems()
             .stream()
             .filter(oi -> itemsToUpdate.contains(oi.getItem().getItemId()))
             .forEach(oi -> oi.setQuantity(incomingQuantityByItemId.get(oi.getItem().getItemId())));
    }

    private void updateOrderWithAdditionalOrderItems(OrderUpdateDto updateDto, Order toUpdate,
                                                     Set<Long> itemsIdsToAdd) {
        Map<Long, ItemSnapshot> itemSnapshotsByItemId = fetchItemSnaphotsMapByItemIds(itemsIdsToAdd);

        updateDto.items()
                 .stream()
                 .filter(oiDto -> itemsIdsToAdd.contains(oiDto.itemId()))
                 .forEach(oiDto ->
                                  mapToOrderItemAndAssign(toUpdate, oiDto, itemSnapshotsByItemId));
    }

    private boolean needToCreateAdditionalOrderItems(Set<Long> idsToAdd) {
        return !idsToAdd.isEmpty();
    }

    private boolean needToUpdateExistingOrderItems(Set<Long> itemsToUpdate) {
        return !itemsToUpdate.isEmpty();
    }

    private Set<Long> calculateItemIdsToRemove(Order toUpdate, Set<Long> incomingUniqueItemIds) {
        return toUpdate.getOrderItems()
                       .stream()
                       .map(oi -> oi.getItem().getItemId())
                       .filter(itemId -> !incomingUniqueItemIds.contains(itemId))
                       .collect(Collectors.toSet());
    }

    private Set<Long> calculateItemIdsToUpdate(OrderUpdateDto updateDto, Order orderToUpdate) {
        Map<Long, Integer> orderItemsInitQuantity =
                orderToUpdate.getOrderItems()
                             .stream()
                             .collect(Collectors.toMap(
                                     oi -> oi.getItem().getItemId(),
                                     OrderItem::getQuantity)
                             );
        return updateDto.items()
                        .stream()
                        .filter(oiReq ->
                                        orderItemsInitQuantity.containsKey(oiReq.itemId()) &&
                                                oiReq.quantity()
                                                     .compareTo(orderItemsInitQuantity.get(oiReq.itemId())) != 0
                        )
                        .map(OrderItemDtoRequest::itemId)
                        .collect(Collectors.toSet());
    }

    private Set<Long> calculateItemIdsToAdd(OrderUpdateDto updateDto, Set<Long> existingItemIds) {
        return updateDto.items()
                        .stream()
                        .map(OrderItemDtoRequest::itemId)
                        .filter(reqItem -> !existingItemIds.contains(reqItem))
                        .collect(Collectors.toSet());
    }

    private OrderResponseDto calculateTotalsAndGetOrderResponse(Order order, UserProfileDto userProfileDto) {
        List<OrderItemDtoResponse> calculatedOrderItemResponses = getOrderItemResponses(order);
        BigDecimal orderTotal = orderCalculator.calculateOrderTotal(order);
        return orderMapper.toDto(order,
                                 userProfileDto,
                                 calculatedOrderItemResponses,
                                 orderTotal);
    }

    private Order getOrderByIdAndUserId(UUID orderId, Long userId) {
        log.info("Invoking DB to find order: {} for userid: {}", orderId, userId);
        Order order = repository.findByIdAndUserIdFetchOrderItems(orderId, userId)
                                .orElseThrow(() -> new OrderNotFoundException(
                                        "Not found an order: {%s} for userid: {%s}".formatted(orderId, userId),
                                        HttpStatus.BAD_REQUEST));
        log.info("Found order: {}", order);
        return order;
    }

    private UserProfileDto retrieveUserProfile(Long userId) {
        try {
            return userServiceClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            throw new UserNotFoundException(
                    "Cannot retrieve the user: {%s} from user-service".formatted(userId),
                    HttpStatus.NOT_FOUND, e);
        }
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
                     mapToOrderItemAndAssign(order, orderItemDto, existingItems);
                 });
    }

    private void mapToOrderItemAndAssign(Order order,
                                         OrderItemDtoRequest orderItemDto,
                                         Map<Long, ItemSnapshot> existingItems) {
        OrderItem orderItem = convertToOrderItem(orderItemDto, existingItems);
        order.addOrderItem(orderItem);
    }

    private Map<Long, ItemSnapshot> validateAndGetItemsByIds(OrderCreateDto createDto) {
        Set<Long> uniqueIncomingItemIds = getUniqueIncomingItemIds(createDto.items());
        checkIfNoDuplicatedOrderItems(createDto.items(), uniqueIncomingItemIds);
        return fetchItemSnaphotsMapByItemIds(uniqueIncomingItemIds);
    }

    private Map<Long, ItemSnapshot> fetchItemSnaphotsMapByItemIds(Set<Long> uniqueIncomingItemIds) {
        Set<ItemSnapshot> items = itemFacade.getByIds(uniqueIncomingItemIds);
        Map<Long, ItemSnapshot> existingItems = getSnapshotsMap(items);
        checkIfAllItemsWereFound(uniqueIncomingItemIds, existingItems);
        return existingItems;
    }

    private Set<Long> getUniqueIncomingItemIds(List<OrderItemDtoRequest> incomingOrderItems) {
        return incomingOrderItems.stream()
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

    private void checkIfNoDuplicatedOrderItems(List<OrderItemDtoRequest> incomingOrderItems,
                                               Set<Long> uniqueIncomingItemIds) {
        if (isNotUniqueOrderItems(uniqueIncomingItemIds, incomingOrderItems)) {
            throw new NotUniqueOrderItemException(
                    "The provided order item dto list contains duplicated order items: %s".formatted(
                            incomingOrderItems),
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

    private void checkIfModificationAllowed(Order order) {
        log.info("Checking the order's: {} status prior to modifying", order.getId());
        if (order.getStatus() == OrderStatus.FINISHED) {
            throw new OrderModificationDeniedException(
                    "Modifying of the order {%s} cannot be performed! Order is in [%s] status"
                            .formatted(order.getId(), OrderStatus.FINISHED.name()), HttpStatus.BAD_REQUEST);
        }
    }
}
