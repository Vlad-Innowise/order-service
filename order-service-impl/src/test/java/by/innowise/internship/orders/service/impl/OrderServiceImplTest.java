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
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import by.innowise.internship.orders.service.facade.ItemFacade;
import by.innowise.internship.orders.service.util.TestUtil;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long MISSING_USER_ID = 999L;
    private static final UUID ORDER_1_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_2_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ORDER_3_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private ItemSnapshot macbook;
    private ItemSnapshot iphone;
    private ItemSnapshot airpods;
    private UserProfileDto userProfile;

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderMapper mapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private OrderCalculator orderCalculator;

    @Mock
    private ItemFacade itemFacade;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void prepareTest() {

        macbook = TestUtil.getItem(1L, "Macbook", BigDecimal.valueOf(2100));
        iphone = TestUtil.getItem(2L, "Iphone", BigDecimal.valueOf(1100));
        airpods = TestUtil.getItem(3L, "Airpods", BigDecimal.valueOf(250));
        userProfile = TestUtil.getUserProfile(USER_ID,
                                              "Test",
                                              "Testorson",
                                              LocalDate.of(1990, 7, 22),
                                              "testorson@email.com");
    }

    @Test
    void createOrderHappyPass() {

        int macbookQuantity = 1;
        int airpodsQuantity = 2;

        OrderCreateDto orderCreateDto = new OrderCreateDto(
                LocalDateTime.now(),
                List.of(
                        TestUtil.getOrderItemDtoRequest(macbook.getItemId(), macbookQuantity),
                        TestUtil.getOrderItemDtoRequest(airpods.getItemId(), airpodsQuantity))
        );

        Order order = TestUtil.getOrderWithoutOrderItems(ORDER_1_ID,
                                                         USER_ID,
                                                         OrderStatus.PENDING,
                                                         orderCreateDto.creationDate());

        // for orderItemMapper.toEntity() and OrderItemResponse map
        Map<Long, OrderItem> addedOrderItems = Map.of(
                macbook.getItemId(), TestUtil.getOrderItem(UUID.randomUUID(), macbook, macbookQuantity),
                airpods.getItemId(), TestUtil.getOrderItem(UUID.randomUUID(), airpods, airpodsQuantity)
        );

        BigDecimal macbookExpectedSubtotal =
                TestUtil.calculateOrderItemSubtotal(macbook.getItemPrice(), macbookQuantity);
        BigDecimal airpodsExpectedSubtotal =
                TestUtil.calculateOrderItemSubtotal(macbook.getItemPrice(), airpodsQuantity);

        BigDecimal orderExpectedTotal = macbookExpectedSubtotal.add(airpodsExpectedSubtotal);

        // for orderItemMapper.toDto() and expectedResult
        Map<Long, OrderItemDtoResponse> orderItemResponseByItemIds = Map.of(
                macbook.getItemId(),
                TestUtil.mapToOrderItemResponse(addedOrderItems.get(macbook.getItemId()), macbookExpectedSubtotal),
                airpods.getItemId(),
                TestUtil.mapToOrderItemResponse(addedOrderItems.get(airpods.getItemId()), airpodsExpectedSubtotal)
        );

        OrderResponseDto expectedResult =
                TestUtil.mapToOrderResponseDto(order,
                                               userProfile,
                                               List.of(orderItemResponseByItemIds.get(macbook.getItemId()),
                                                       orderItemResponseByItemIds.get(airpods.getItemId())),
                                               orderExpectedTotal);

        doReturn(order)
                .when(mapper).toEntity(orderCreateDto, USER_ID);

        doReturn(Set.of(macbook, airpods))
                .when(itemFacade).getByIds(anyCollection());

        doAnswer(invocation -> {
            OrderItemDtoRequest req = invocation.getArgument(0, OrderItemDtoRequest.class);
            return Optional.ofNullable(addedOrderItems.get(req.itemId()))
                           .orElseThrow(() -> new NoSuchElementException(
                                   "Item with id {%s} should be added to order".formatted(req.itemId())));
        }).when(orderItemMapper).toEntity(any(OrderItemDtoRequest.class), any(ItemSnapshot.class));

        doReturn(order)
                .when(repository).saveAndFlush(any(Order.class));

        doReturn(
                Map.of(macbook.getItemId(), macbookExpectedSubtotal,
                       airpods.getItemId(), airpodsExpectedSubtotal)
        ).when(orderCalculator).calculateSubtotals(any(Order.class));

        doReturn(orderExpectedTotal)
                .when(orderCalculator).calculateOrderTotal(any(Order.class));

        doAnswer(invocation ->
                         convertOrderItemEntityToResponseDto(invocation, orderItemResponseByItemIds))
                .when(orderItemMapper).toResponseDto(any(OrderItem.class), any(BigDecimal.class));

        doReturn(userProfile)
                .when(userServiceClient).getUserById(USER_ID);

        doReturn(expectedResult)
                .when(mapper).toDto(any(Order.class), any(UserProfileDto.class), anyList(), any(BigDecimal.class));

        OrderResponseDto actualResult = orderService.create(orderCreateDto, USER_ID);

        assertThat(actualResult).isEqualTo(expectedResult);
        verify(userServiceClient).getUserById(anyLong());
        verify(mapper).toEntity(any(), any());
        verify(itemFacade).getByIds(anyCollection());
        verify(orderItemMapper, times(orderCreateDto.items().size())).toEntity(any(), any());
        verify(repository).saveAndFlush(any());
        verify(orderCalculator).calculateSubtotals(any());
        verify(orderCalculator).calculateOrderTotal(any());
        verify(orderItemMapper, times(orderCreateDto.items().size())).toResponseDto(any(), any());
        verify(mapper).toDto(any(), any(), anyList(), any());

    }

    @Test
    void createOrderShouldThrowExceptionInCaseDuplicatedOrderItemRequestIds() {
        OrderCreateDto orderCreateDto = new OrderCreateDto(
                LocalDateTime.now(),
                List.of(
                        TestUtil.getOrderItemDtoRequest(airpods.getItemId(), 1),
                        TestUtil.getOrderItemDtoRequest(airpods.getItemId(), 1))
        );

        Order order = TestUtil.getOrderWithoutOrderItems(ORDER_1_ID,
                                                         USER_ID,
                                                         OrderStatus.PENDING,
                                                         orderCreateDto.creationDate());

        doReturn(order)
                .when(mapper).toEntity(orderCreateDto, USER_ID);

        assertThrowsExactly(NotUniqueOrderItemException.class, () -> orderService.create(orderCreateDto, USER_ID));
        verify(userServiceClient).getUserById(anyLong());
        verify(mapper).toEntity(any(), any());
    }

    @Test
    void createShouldThrowWhenAnyItemFromRequestNotFound() {
        long missingItemId = 999L;
        OrderCreateDto orderCreateDto = new OrderCreateDto(
                LocalDateTime.now(),
                List.of(
                        TestUtil.getOrderItemDtoRequest(iphone.getItemId(), 1),
                        TestUtil.getOrderItemDtoRequest(missingItemId, 3))
        );

        Order order = TestUtil.getOrderWithoutOrderItems(ORDER_1_ID,
                                                         USER_ID,
                                                         OrderStatus.PENDING,
                                                         orderCreateDto.creationDate());

        doReturn(order)
                .when(mapper).toEntity(orderCreateDto, USER_ID);

        doReturn(Set.of(iphone))
                .when(itemFacade).getByIds(anyCollection());

        assertThrowsExactly(ItemNotFoundException.class, () -> orderService.create(orderCreateDto, USER_ID));
        verify(userServiceClient).getUserById(anyLong());
        verify(mapper).toEntity(any(), any());
        verify(itemFacade).getByIds(anyCollection());
    }

    @Test
    void allMethodsShouldThrowUserNotFoundWhenUserMissing() {
        OrderCreateDto createDto = new OrderCreateDto(LocalDateTime.now(), List.of());
        UUID someOrderId = UUID.randomUUID();
        OrderUpdateDto updateDto = new OrderUpdateDto(someOrderId,
                                                      LocalDateTime.now(),
                                                      Collections.emptyList(),
                                                      OrderStatus.PENDING);

        Pageable pageable = Pageable.unpaged();

        doThrow(FeignException.NotFound.class)
                .when(userServiceClient).getUserById(MISSING_USER_ID);

        assertAll(
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.create(createDto, MISSING_USER_ID)),
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.getById(someOrderId, MISSING_USER_ID)),
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.getAllByIds(List.of(someOrderId), MISSING_USER_ID,
                                                                         pageable)),
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.getAllByStatus(MISSING_USER_ID, OrderStatus.PENDING)),
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.update(updateDto, MISSING_USER_ID))
        );
    }


    @Test
    void getOrderByIdHappyPass() {
        LocalDateTime orderCreationDate = LocalDateTime.of(LocalDate.of(2025, 9, 11), LocalTime.NOON);
        Order order = TestUtil.getOrderWithoutOrderItems(ORDER_2_ID,
                                                         USER_ID,
                                                         OrderStatus.FINISHED,
                                                         orderCreationDate);
        int macbookQuantity = 1;
        int iphoneQuantity = 1;
        int airpodsQuantity = 2;

        // for OrderItemResponse map
        Map<Long, OrderItem> addedOrderItems = Map.of(
                macbook.getItemId(), TestUtil.getOrderItem(UUID.randomUUID(), macbook, macbookQuantity),
                iphone.getItemId(), TestUtil.getOrderItem(UUID.randomUUID(), iphone, iphoneQuantity),
                airpods.getItemId(), TestUtil.getOrderItem(UUID.randomUUID(), airpods, airpodsQuantity)
        );

        //assigning OrderItems to Order
        addedOrderItems.values().forEach(order::addOrderItem);

        //Calculating Order subTotals and totals
        BigDecimal macbookExpectedSubtotal =
                TestUtil.calculateOrderItemSubtotal(macbook.getItemPrice(), macbookQuantity);
        BigDecimal iphoneExpectedSubtotal =
                TestUtil.calculateOrderItemSubtotal(iphone.getItemPrice(), iphoneQuantity);
        BigDecimal airpodsExpectedSubtotal =
                TestUtil.calculateOrderItemSubtotal(macbook.getItemPrice(), airpodsQuantity);
        BigDecimal orderExpectedTotal = macbookExpectedSubtotal
                .add(iphoneExpectedSubtotal)
                .add(airpodsExpectedSubtotal);

        // for orderItemMapper.toDto() and expectedResult
        Map<Long, OrderItemDtoResponse> orderItemResponseByItemIds = Map.of(
                macbook.getItemId(),
                TestUtil.mapToOrderItemResponse(addedOrderItems.get(macbook.getItemId()), macbookExpectedSubtotal),
                iphone.getItemId(),
                TestUtil.mapToOrderItemResponse(addedOrderItems.get(iphone.getItemId()), iphoneExpectedSubtotal),
                airpods.getItemId(),
                TestUtil.mapToOrderItemResponse(addedOrderItems.get(airpods.getItemId()), airpodsExpectedSubtotal)
        );

        OrderResponseDto expectedResult =
                TestUtil.mapToOrderResponseDto(order,
                                               userProfile,
                                               List.of(orderItemResponseByItemIds.get(macbook.getItemId()),
                                                       orderItemResponseByItemIds.get(iphone.getItemId()),
                                                       orderItemResponseByItemIds.get(airpods.getItemId())
                                               ),
                                               orderExpectedTotal);

        doReturn(userProfile)
                .when(userServiceClient).getUserById(USER_ID);

        doReturn(Optional.of(order))
                .when(repository).findByIdAndUserIdFetchOrderItems(any(UUID.class), anyLong());

        doReturn(
                Map.of(macbook.getItemId(), macbookExpectedSubtotal,
                       iphone.getItemId(), iphoneExpectedSubtotal,
                       airpods.getItemId(), airpodsExpectedSubtotal)
        ).when(orderCalculator).calculateSubtotals(any(Order.class));

        doReturn(orderExpectedTotal)
                .when(orderCalculator).calculateOrderTotal(any(Order.class));

        doAnswer(invocation ->
                         convertOrderItemEntityToResponseDto(invocation, orderItemResponseByItemIds))
                .when(orderItemMapper).toResponseDto(any(OrderItem.class), any(BigDecimal.class));

        doReturn(expectedResult)
                .when(mapper).toDto(any(Order.class), any(UserProfileDto.class), anyList(), any(BigDecimal.class));

        OrderResponseDto actualResult = orderService.getById(ORDER_2_ID, USER_ID);

        assertThat(actualResult).isEqualTo(expectedResult);
        verify(userServiceClient).getUserById(anyLong());
        verify(repository).findByIdAndUserIdFetchOrderItems(any(UUID.class), anyLong());
        verify(orderCalculator).calculateSubtotals(any(Order.class));
        verify(orderCalculator).calculateOrderTotal(any(Order.class));
        verify(orderItemMapper, times(addedOrderItems.size())).toResponseDto(any(OrderItem.class),
                                                                             any(BigDecimal.class));
        verify(mapper).toDto(any(Order.class), any(UserProfileDto.class), anyList(), any(BigDecimal.class));

    }

    @Test
    void getOrderByShouldThrowOrderNotFound() {
        doReturn(userProfile)
                .when(userServiceClient).getUserById(USER_ID);
        doReturn(Optional.empty())
                .when(repository).findByIdAndUserIdFetchOrderItems(any(UUID.class), anyLong());

        assertThrowsExactly(OrderNotFoundException.class, () -> orderService.getById(ORDER_2_ID, USER_ID));
        verify(userServiceClient).getUserById(anyLong());
    }

    @Test
    void deleteHappyPass() {
        LocalDateTime orderCreationDate = LocalDateTime.of(LocalDate.of(2025, 9, 11), LocalTime.NOON);
        Order order = TestUtil.getOrderWithoutOrderItems(ORDER_2_ID,
                                                         USER_ID,
                                                         OrderStatus.PENDING,
                                                         orderCreationDate);
        Stream.of(
                TestUtil.getOrderItem(UUID.randomUUID(), macbook, 1),
                TestUtil.getOrderItem(UUID.randomUUID(), iphone, 1),
                TestUtil.getOrderItem(UUID.randomUUID(), airpods, 2)
        ).forEach(order::addOrderItem);

        doReturn(Optional.of(order))
                .when(repository).findByIdAndUserIdFetchOrderItems(any(UUID.class), anyLong());

        doNothing()
                .when(repository).delete(any(Order.class));

        orderService.delete(ORDER_2_ID, USER_ID);

        verify(repository).findByIdAndUserIdFetchOrderItems(any(UUID.class), anyLong());
        verify(repository).delete(any(Order.class));
    }

    @Test
    void deleteShouldThrowOrderModificationExceptionForOrderInFinishedStatus() {
        LocalDateTime orderCreationDate = LocalDateTime.of(LocalDate.of(2025, 9, 11), LocalTime.NOON);
        Order order = TestUtil.getOrderWithoutOrderItems(ORDER_2_ID,
                                                         USER_ID,
                                                         OrderStatus.FINISHED,
                                                         orderCreationDate);
        Stream.of(
                TestUtil.getOrderItem(UUID.randomUUID(), macbook, 1),
                TestUtil.getOrderItem(UUID.randomUUID(), iphone, 1),
                TestUtil.getOrderItem(UUID.randomUUID(), airpods, 2)
        ).forEach(order::addOrderItem);

        doReturn(Optional.of(order))
                .when(repository).findByIdAndUserIdFetchOrderItems(any(UUID.class), anyLong());

        assertThrowsExactly(OrderModificationDeniedException.class, () -> orderService.delete(ORDER_2_ID, USER_ID));

        verify(repository).findByIdAndUserIdFetchOrderItems(any(UUID.class), anyLong());
    }

    private OrderItemDtoResponse convertOrderItemEntityToResponseDto(InvocationOnMock invocation,
                                                                     Map<Long, OrderItemDtoResponse> orderItemResponseByItemIds) {
        OrderItem orderItem = invocation.getArgument(0, OrderItem.class);
        return Optional.ofNullable(orderItemResponseByItemIds.get(orderItem.getItem().getItemId()))
                       .orElseThrow();
    }
}