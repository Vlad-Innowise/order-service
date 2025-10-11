package by.innowise.internship.orders.controller;

import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.dto.order.OrderUpdateDto;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.service.facade.OrderFacade;
import by.innowise.internship.security.dto.UserHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody @Valid OrderCreateDto createDto,
                                                        @AuthenticationPrincipal UserHolder userHolder) {
        Long authId = userHolder.crossServiceUserId();
        log.info("Requested to create an order: {} for user: {}", createDto, authId);
        OrderResponseDto created = orderFacade.create(createDto, authId);
        log.info("Created an order: {}", created);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrder(@PathVariable UUID orderId,
                                                     @AuthenticationPrincipal UserHolder userHolder) {
        Long authId = userHolder.crossServiceUserId();
        log.info("Requested to get the order by id: {} for user: {}", orderId, authId);
        OrderResponseDto found = orderFacade.getById(orderId, authId);
        log.info("Retrieved the order: {}", found);
        return ResponseEntity.ok(found);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders(@AuthenticationPrincipal UserHolder userHolder) {
        Long authId = userHolder.crossServiceUserId();
        log.info("Requested to get all orders for user: {}", authId);
        List<OrderResponseDto> orders = orderFacade.getAll(authId);
        log.info("Sending orders: [{}] to the client ", orders.size());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/by-ids")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByIds(@RequestParam("id") @NotEmpty List<UUID> ids,
                                                                 @AuthenticationPrincipal UserHolder userHolder,
                                                                 @PageableDefault(sort = {"creationDate"},
                                                                                  direction = Sort.Direction.DESC)
                                                                 Pageable pageable) {
        Long authId = userHolder.crossServiceUserId();
        log.info("Requested to get orders: {} by for user: {}", ids, authId);
        List<OrderResponseDto> orders = orderFacade.getAllByIds(ids, authId, pageable);
        log.info("Sending the order responses: {}", orders);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByStatus(@RequestParam OrderStatus status,
                                                                    @AuthenticationPrincipal UserHolder userHolder) {
        Long authId = userHolder.crossServiceUserId();
        log.info("Requested to get orders by status:{} for user: {}", status, authId);
        List<OrderResponseDto> orders = orderFacade.getAllByStatus(authId, status);
        List<UUID> orderIds = orders.stream()
                                    .map(OrderResponseDto::id)
                                    .toList();
        log.info("Sending all orders: {} for userId: {} with status: {}", orderIds, authId, status);
        return ResponseEntity.ok(orders);
    }

    @PutMapping
    public ResponseEntity<OrderResponseDto> updateOrder(@RequestBody @Valid OrderUpdateDto updateDto,
                                                        @AuthenticationPrincipal UserHolder userHolder) {
        Long authId = userHolder.crossServiceUserId();
        log.info("Requested to update order with id: {} for user: {}", updateDto.id(), authId);
        OrderResponseDto updated = orderFacade.update(updateDto, authId);
        log.info("Sending updated order to a client: {}", updated);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(@PathVariable UUID orderId,
                                                              @RequestParam OrderStatus newStatus,
                                                              @AuthenticationPrincipal UserHolder userHolder) {
        Long authId = userHolder.crossServiceUserId();
        log.info("User: {} initiated request to update order: {} to status: [{}]", authId, orderId, newStatus);
        OrderResponseDto updated = orderFacade.updateStatus(orderId, authId, newStatus);
        log.info("Order: {} successfully updated to status: [{}]", updated.id(), updated.status());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID orderId,
                                           @AuthenticationPrincipal UserHolder userHolder) {
        Long authId = userHolder.crossServiceUserId();
        log.info("Requested to delete the order: {} for user: {}", orderId, authId);
        orderFacade.delete(orderId, authId);
        log.info("Order: {} deleted successfully", orderId);
        return ResponseEntity.ok()
                             .build();
    }

}
