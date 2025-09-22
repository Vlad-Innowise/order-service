package by.innowise.internship.orders.service.impl;

import by.innowise.common.library.dto.UserProfileDto;
import by.innowise.internship.orders.feign.UserServiceClient;
import by.innowise.internship.orders.mapper.OrderItemMapper;
import by.innowise.internship.orders.mapper.OrderMapper;
import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoRequest;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoResponse;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.entity.Order;
import by.innowise.internship.orders.model.entity.OrderItem;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.repository.OrderRepository;
import by.innowise.internship.orders.service.OrderCalculator;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import by.innowise.internship.orders.service.facade.ItemFacade;
import by.innowise.internship.orders.service.util.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
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

        Order order = TestUtil.getOrder(ORDER_1_ID,
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

        doAnswer(invocation -> {
            OrderItem orderItem = invocation.getArgument(0, OrderItem.class);
            return Optional.ofNullable(orderItemResponseByItemIds.get(orderItem.getItem().getItemId()))
                           .orElseThrow();
        }).when(orderItemMapper).toResponseDto(any(OrderItem.class), any(BigDecimal.class));

        doReturn(userProfile)
                .when(userServiceClient).getUserById(USER_ID);


        doReturn(expectedResult)
                .when(mapper).toDto(any(Order.class), any(UserProfileDto.class), anyList(), any(BigDecimal.class));


        OrderResponseDto actualResult = orderService.create(orderCreateDto, USER_ID);

        assertThat(actualResult).isEqualTo(expectedResult);
        verify(mapper).toEntity(any(), any());
        verify(itemFacade).getByIds(anyCollection());
        verify(orderItemMapper, times(orderCreateDto.items().size())).toEntity(any(), any());
        verify(repository).saveAndFlush(any());
        verify(orderCalculator).calculateSubtotals(any());
        verify(orderCalculator).calculateOrderTotal(any());
        verify(orderItemMapper, times(orderCreateDto.items().size())).toResponseDto(any(), any());
        verify(userServiceClient).getUserById(anyLong());
        verify(mapper).toDto(any(), any(), anyList(), any());

    }


}