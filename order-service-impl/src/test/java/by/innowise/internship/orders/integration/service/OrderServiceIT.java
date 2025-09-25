package by.innowise.internship.orders.integration.service;

import by.innowise.common.library.dto.UserProfileDto;
import by.innowise.common.library.exception.UserNotFoundException;
import by.innowise.internship.orders.exception.ItemNotFoundException;
import by.innowise.internship.orders.exception.OrderModificationDeniedException;
import by.innowise.internship.orders.integration.core.IntegrationTestBase;
import by.innowise.internship.orders.integration.util.WireMockUtils;
import by.innowise.internship.orders.model.dto.order.OrderCreateDto;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoRequest;
import by.innowise.internship.orders.model.dto.order.OrderItemDtoResponse;
import by.innowise.internship.orders.model.dto.order.OrderResponseDto;
import by.innowise.internship.orders.model.dto.order.OrderUpdateDto;
import by.innowise.internship.orders.model.entity.Order;
import by.innowise.internship.orders.model.entity.OrderItem;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.repository.OrderRepository;
import by.innowise.internship.orders.service.OrderService;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import by.innowise.internship.orders.service.facade.ItemFacade;
import by.innowise.internship.orders.util.TestUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@Slf4j
@AutoConfigureWireMock(port = 0)
public class OrderServiceIT extends IntegrationTestBase {

    private static final UUID FRODO_ORDER_1_ID_FINISHED = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FRODO_ORDER_2_ID_FINISHED = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID FRODO_ORDER_3_ID_PENDING = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID SAM_ORDER_1_ID_FINISHED = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID SAM_ORDER_2_ID_FINISHED = UUID.fromString("00000000-0000-0000-0000-000000000005");

    private Map<UUID, Order> ordersByIds;

    private UserProfileDto frodoProfile;
    private UserProfileDto samProfile;
    private UserProfileDto aryaProfile;

    private ItemSnapshot macbook;
    private ItemSnapshot iphone;
    private ItemSnapshot airpods;

    @Autowired
    private ItemFacade itemFacade;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WireMockUtils wireMockUtils;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void prepareTest() {
        prepareUserProfiles();
        prepareOrders();
        prepareItems();
    }

    @Test
    void allMethodsShouldThrowUserNotFoundWhenUserMissing() {
        long missingUserId = 999L;

        OrderCreateDto createDto = new OrderCreateDto(LocalDateTime.now(), List.of());
        UUID someOrderId = UUID.randomUUID();
        OrderUpdateDto updateDto = new OrderUpdateDto(someOrderId,
                                                      LocalDateTime.now(),
                                                      Collections.emptyList(),
                                                      OrderStatus.PENDING);
        Pageable pageable = Pageable.unpaged();

        stubFor(get(urlEqualTo("/api/v1/internal/users/" + missingUserId))
                        .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value()))
        );

        assertAll(
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.create(createDto, missingUserId)),
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.getById(someOrderId, missingUserId)),
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.getAllByIds(List.of(someOrderId), missingUserId,
                                                                         pageable)),
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.getAllByStatus(missingUserId, OrderStatus.FINISHED)),
                () -> assertThrowsExactly(UserNotFoundException.class,
                                          () -> orderService.update(updateDto, missingUserId))
        );
    }

    @Test
    void createOrderHappyPass() {
        OrderCreateDto orderCreateDto = new OrderCreateDto(
                LocalDateTime.now(),
                List.of(
                        TestUtil.getOrderItemDtoRequest(macbook.getItemId(), 1),
                        TestUtil.getOrderItemDtoRequest(airpods.getItemId(), 2))
        );

        Map<Long, Integer> expectedItems = getExpectedQtyByItemIdForUpdate(orderCreateDto.items());

        mockUserClientCallForUserProfile(aryaProfile);

        OrderResponseDto actualResult = orderService.create(orderCreateDto, aryaProfile.authId());

        assertAll(
                () -> assertThat(actualResult.id()).isNotNull(),
                () -> assertThat(actualResult.status()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(actualResult.creationDate()).isNotNull(),
                () -> assertThat(actualResult.total()).isNotNull(),
                () -> assertThat(actualResult.user()).isEqualTo(aryaProfile),
                () -> assertThat(actualResult.orderItems())
                        .hasSize(orderCreateDto.items().size())
                        .allSatisfy(oi -> {
                            assertThat(actualResult.orderItems())
                                    .extracting(OrderItemDtoResponse::itemId)
                                    .containsExactlyInAnyOrderElementsOf(expectedItems.keySet());
                            assertThat(oi.quantity()).isEqualTo(expectedItems.get(oi.itemId()));
                        })
        );
    }

    @Test
    void createShouldThrowWhenAnyItemFromRequestNotFound() {
        long missingItemId = 999L;
        OrderCreateDto orderCreateDto = new OrderCreateDto(
                LocalDateTime.now(),
                List.of(TestUtil.getOrderItemDtoRequest(missingItemId, 1))
        );

        Set<ItemSnapshot> expectedEmptyItems = itemFacade.getByIds(Set.of(missingItemId));

        mockUserClientCallForUserProfile(aryaProfile);

        assertAll(
                () -> assertThrowsExactly(ItemNotFoundException.class,
                                          () -> orderService.create(orderCreateDto, aryaProfile.authId())),
                () -> assertThat(expectedEmptyItems).isEmpty(),
                () -> assertThat(orderRepository.findAllByUserId(aryaProfile.authId())).isEmpty()
        );
    }

    @Test
    void getOrderByIdHappyPass() {
        Order orderToCompare = ordersByIds.get(FRODO_ORDER_1_ID_FINISHED);

        mockUserClientCallForUserProfile(frodoProfile);

        OrderResponseDto actualResult = orderService.getById(FRODO_ORDER_1_ID_FINISHED, frodoProfile.authId());

        assertAll(
                () -> assertThat(actualResult.id()).isEqualTo(orderToCompare.getId()),
                () -> assertThat(actualResult.status()).isEqualTo(orderToCompare.getStatus()),
                () -> assertThat(actualResult.creationDate()).isEqualTo(orderToCompare.getCreationDate()),
                () -> assertThat(actualResult.total()).isNotNull(),
                () -> assertThat(actualResult.user()).isEqualTo(frodoProfile),
                () -> assertThat(actualResult.orderItems()).hasSize(orderToCompare.getOrderItems().size())
        );
    }

    @Test
    void getOrdersByIdsFoundAllOrPartially() {
        Order firstOrderToCompare = ordersByIds.get(SAM_ORDER_1_ID_FINISHED);
        Order secondOrderToCompare = ordersByIds.get(SAM_ORDER_2_ID_FINISHED);

        List<UUID> idsToFind = List.of(SAM_ORDER_1_ID_FINISHED, SAM_ORDER_2_ID_FINISHED, UUID.randomUUID());
        Pageable pageable = Pageable.unpaged();

        mockUserClientCallForUserProfile(samProfile);

        List<OrderResponseDto> actualResult = orderService.getAllByIds(idsToFind, samProfile.authId(),
                                                                       pageable);

        assertAll(
                () -> assertThat(actualResult).hasSize(2),
                () -> assertThat(actualResult)
                        .extracting(OrderResponseDto::id)
                        .containsExactlyInAnyOrder(
                                firstOrderToCompare.getId(),
                                secondOrderToCompare.getId()),

                () -> assertThat(actualResult)
                        .filteredOn(orderResp -> orderResp.id().equals(firstOrderToCompare.getId()))
                        .allSatisfy(order -> {
                            assertThat(order.id()).isEqualTo(firstOrderToCompare.getId());
                            assertThat(order.status()).isEqualTo(firstOrderToCompare.getStatus());
                            assertThat(order.creationDate()).isEqualTo(firstOrderToCompare.getCreationDate());
                            assertThat(order.total()).isNotNull();
                            assertThat(order.user()).isEqualTo(samProfile);
                            assertThat(order.orderItems()).hasSize(firstOrderToCompare.getOrderItems().size());
                        }),

                () -> assertThat(actualResult)
                        .filteredOn(orderResp -> orderResp.id().equals(secondOrderToCompare.getId()))
                        .allSatisfy(order -> {
                            assertThat(order.id()).isEqualTo(secondOrderToCompare.getId());
                            assertThat(order.status()).isEqualTo(secondOrderToCompare.getStatus());
                            assertThat(order.creationDate()).isEqualTo(secondOrderToCompare.getCreationDate());
                            assertThat(order.total()).isNotNull();
                            assertThat(order.user()).isEqualTo(samProfile);
                            assertThat(order.orderItems()).hasSize(secondOrderToCompare.getOrderItems().size());
                        })
        );
    }

    @Test
    void getAllOrdersByStatus() {
        Order firstOrderToCompare = ordersByIds.get(FRODO_ORDER_1_ID_FINISHED);
        Order secondOrderToCompare = ordersByIds.get(FRODO_ORDER_2_ID_FINISHED);

        mockUserClientCallForUserProfile(frodoProfile);

        List<OrderResponseDto> actualResult = orderService.getAllByStatus(frodoProfile.authId(), OrderStatus.FINISHED);

        assertAll(
                () -> assertThat(actualResult).hasSize(2),
                () -> assertThat(actualResult)
                        .extracting(OrderResponseDto::id)
                        .containsExactlyInAnyOrder(
                                firstOrderToCompare.getId(),
                                secondOrderToCompare.getId()),

                () -> assertThat(actualResult)
                        .filteredOn(orderResp -> orderResp.id().equals(firstOrderToCompare.getId()))
                        .allSatisfy(order -> {
                            assertThat(order.id()).isEqualTo(firstOrderToCompare.getId());
                            assertThat(order.status()).isEqualTo(firstOrderToCompare.getStatus());
                            assertThat(order.creationDate()).isEqualTo(firstOrderToCompare.getCreationDate());
                            assertThat(order.total()).isNotNull();
                            assertThat(order.user()).isEqualTo(frodoProfile);
                            assertThat(order.orderItems()).hasSize(firstOrderToCompare.getOrderItems().size());
                        }),

                () -> assertThat(actualResult)
                        .filteredOn(orderResp -> orderResp.id().equals(secondOrderToCompare.getId()))
                        .allSatisfy(order -> {
                            assertThat(order.id()).isEqualTo(secondOrderToCompare.getId());
                            assertThat(order.status()).isEqualTo(secondOrderToCompare.getStatus());
                            assertThat(order.creationDate()).isEqualTo(secondOrderToCompare.getCreationDate());
                            assertThat(order.total()).isNotNull();
                            assertThat(order.user()).isEqualTo(frodoProfile);
                            assertThat(order.orderItems()).hasSize(secondOrderToCompare.getOrderItems().size());
                        })
        );
    }

    @Test
    void checkAddingNewOrderItemToOrder() {

        Order order = ordersByIds.get(FRODO_ORDER_3_ID_PENDING);

        List<OrderItemDtoRequest> initOrderItemsToModify =
                order.getOrderItems()
                     .stream()
                     .map(oi ->
                                  TestUtil.getOrderItemDtoRequest(oi.getItem().getItemId(), oi.getQuantity()))
                     .collect(Collectors.toCollection(ArrayList::new));

        // adding new order item
        OrderItemDtoRequest newOrderItemToAdd = TestUtil.getOrderItemDtoRequest(macbook.getItemId(), 1);
        initOrderItemsToModify.add(newOrderItemToAdd);

        OrderUpdateDto updateDto =
                new OrderUpdateDto(order.getId(),
                                   order.getCreationDate(),
                                   initOrderItemsToModify,
                                   order.getStatus());

        Map<Long, Integer> expectedItems = getExpectedQtyByItemIdForUpdate(updateDto.items());

        mockUserClientCallForUserProfile(frodoProfile);

        OrderResponseDto actualResult = orderService.update(updateDto, frodoProfile.authId());

        assertAll(
                () -> assertThat(actualResult.id()).isEqualTo(order.getId()),
                () -> assertThat(actualResult.status()).isEqualTo(order.getStatus()),
                () -> assertThat(actualResult.user()).isEqualTo(frodoProfile),
                () -> assertThat(actualResult.orderItems())
                        .extracting(OrderItemDtoResponse::itemId)
                        .containsExactlyInAnyOrderElementsOf(expectedItems.keySet()),
                () -> assertThat(actualResult.orderItems())
                        .hasSize(updateDto.items().size())
                        .allSatisfy(oi -> {
                            assertThat(oi.quantity()).isEqualByComparingTo(expectedItems.get(oi.itemId()));
                        }),
                () -> assertThat(actualResult.total()).isNotNull()
        );
    }

    @Test
    void checkUpdatingQuantityForExistingOrderItem() {
        Order order = ordersByIds.get(FRODO_ORDER_3_ID_PENDING);

        OrderItem initIphoneOrderItem =
                order.getOrderItems()
                     .stream()
                     .filter(oi -> oi.getItem().getItemId().equals(iphone.getItemId()))
                     .findFirst()
                     .orElseThrow(() -> new NoSuchElementException(
                             "There is no order item [%s] in order [%s]".formatted(
                                     iphone.getItemId(),
                                     order.getId())));

        int initialIphoneQuantity = initIphoneOrderItem.getQuantity();

        List<OrderItemDtoRequest> initOrderItemsToModify = getOrderItemsRequestWithIncreasedIphoneQty(order);

        OrderUpdateDto updateDto = new OrderUpdateDto(order.getId(),
                                                      order.getCreationDate(),
                                                      initOrderItemsToModify,
                                                      order.getStatus());

        Map<Long, Integer> expectedItems = getExpectedQtyByItemIdForUpdate(updateDto.items());

        mockUserClientCallForUserProfile(frodoProfile);

        OrderResponseDto actualResult = orderService.update(updateDto, frodoProfile.authId());

        assertAll(
                () -> assertThat(actualResult.id()).isEqualTo(order.getId()),
                () -> assertThat(actualResult.status()).isEqualTo(order.getStatus()),
                () -> assertThat(actualResult.user()).isEqualTo(frodoProfile),
                () -> assertThat(actualResult.orderItems())
                        .extracting(OrderItemDtoResponse::itemId)
                        .containsExactlyInAnyOrderElementsOf(expectedItems.keySet()),
                () -> assertThat(actualResult.orderItems())
                        .hasSize(updateDto.items().size())
                        .allSatisfy(oi -> {
                            assertThat(oi.quantity()).isEqualByComparingTo(expectedItems.get(oi.itemId()));

                            if (oi.itemId().compareTo(iphone.getItemId()) == 0) {
                                assertThat(oi.quantity()).isGreaterThan(initialIphoneQuantity);
                            }
                        }),
                () -> assertThat(actualResult.total()).isNotNull()
        );
    }

    @Test
    void checkRemovingExistingOrderItemFromOrder() {
        Order order = ordersByIds.get(FRODO_ORDER_3_ID_PENDING);

        OrderItem initIphoneOrderItem =
                order.getOrderItems()
                     .stream()
                     .filter(oi -> oi.getItem().getItemId().equals(iphone.getItemId()))
                     .findFirst()
                     .orElseThrow(() -> new NoSuchElementException(
                             "There is no order item [%s] in order [%s]"
                                     .formatted(iphone.getItemId(), order.getId()))
                     );

        List<OrderItemDtoRequest> modifiedOrderItems =
                order.getOrderItems()
                     .stream()
                     .filter(oi -> !oi.equals(initIphoneOrderItem))
                     .map(oi ->
                                  TestUtil.getOrderItemDtoRequest(oi.getItem().getItemId(), oi.getQuantity())
                     )
                     .toList();

        OrderUpdateDto updateDto = new OrderUpdateDto(order.getId(),
                                                      order.getCreationDate(),
                                                      modifiedOrderItems,
                                                      order.getStatus());

        Map<Long, Integer> expectedItems = getExpectedQtyByItemIdForUpdate(updateDto.items());

        mockUserClientCallForUserProfile(frodoProfile);

        OrderResponseDto actualResult = orderService.update(updateDto, frodoProfile.authId());

        assertAll(
                () -> assertThat(actualResult.id()).isEqualTo(order.getId()),
                () -> assertThat(actualResult.status()).isEqualTo(order.getStatus()),
                () -> assertThat(actualResult.user()).isEqualTo(frodoProfile),

                () -> assertThat(actualResult.orderItems())
                        .extracting(OrderItemDtoResponse::itemId)
                        .containsExactlyInAnyOrderElementsOf(expectedItems.keySet())
                        .doesNotContain(iphone.getItemId()),

                () -> assertThat(actualResult.orderItems())
                        .hasSize(updateDto.items().size())
                        .allSatisfy(oi -> {
                            assertThat(oi.quantity()).isEqualByComparingTo(expectedItems.get(oi.itemId()));
                        }),

                () -> assertThat(actualResult.total()).isNotNull()
        );

    }

    @Test
    void deleteHappyPass() {

        Optional<Order> optionalOrderBeforeDeletion = orderRepository.findById(FRODO_ORDER_3_ID_PENDING);
        orderService.delete(FRODO_ORDER_3_ID_PENDING, frodoProfile.authId());
        Optional<Order> optionalOrderAfterDeletion = orderRepository.findById(FRODO_ORDER_3_ID_PENDING);

        assertAll(
                () -> assertThat(optionalOrderBeforeDeletion.isPresent()).isEqualTo(true),
                () -> assertThat(optionalOrderAfterDeletion.isPresent()).isEqualTo(false)
        );
    }

    @Test
    void deleteShouldThrowOrderModificationExceptionForOrderInFinishedStatus() {
        Order order = ordersByIds.get(FRODO_ORDER_2_ID_FINISHED);

        assertAll(
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.FINISHED),
                () -> assertThrowsExactly(OrderModificationDeniedException.class, () ->
                        orderService.delete(FRODO_ORDER_2_ID_FINISHED, frodoProfile.authId()))
        );
    }

    private void prepareOrders() {
        Set<UUID> existingOrderIds = Set.of(FRODO_ORDER_1_ID_FINISHED,
                                            FRODO_ORDER_2_ID_FINISHED,
                                            FRODO_ORDER_3_ID_PENDING,
                                            SAM_ORDER_1_ID_FINISHED,
                                            SAM_ORDER_2_ID_FINISHED);

        ordersByIds = orderRepository.findByIdIn(existingOrderIds)
                                     .stream()
                                     .collect(Collectors.toMap(Order::getId,
                                                               Function.identity()));

        if (!existingOrderIds.containsAll(ordersByIds.keySet())) {
            log.info("Invalid initial data. Expected order ids: {}, found order ids: {}", existingOrderIds,
                     ordersByIds.keySet());
            throw new IllegalArgumentException("Invalid initial data");
        }
    }

    private void prepareUserProfiles() {
        frodoProfile = TestUtil.getUserProfile(1L, "Frodo", "Baggins", LocalDate.of(1997, 7, 7),
                                               "frodo_baggins@email.com");
        samProfile = TestUtil.getUserProfile(2L, "Samwise", "Gampgie", LocalDate.of(1999, 11, 1),
                                             "sam_gampgie@email.com");
        aryaProfile = TestUtil.getUserProfile(3L, "Arya", "Stark", LocalDate.of(1993, 1, 1),
                                              "arya_stark@email.com");
    }

    private void prepareItems() {
        Map<Long, ItemSnapshot> itemsById =
                itemFacade.getByIds(Set.of(1L, 2L, 4L))
                          .stream()
                          .collect(Collectors.toMap(ItemSnapshot::getItemId,
                                                    Function.identity()));

        macbook = itemsById.get(1L);
        iphone = itemsById.get(2L);
        airpods = itemsById.get(4L);
    }

    private void mockUserClientCallForUserProfile(UserProfileDto profile) {
        stubFor(get(urlEqualTo("/api/v1/internal/users/" + profile.authId()))
                        .willReturn(aResponse()
                                            .withStatus(HttpStatus.OK.value())
                                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                            .withBody(wireMockUtils.convertToJson(profile)))
        );
    }

    private List<OrderItemDtoRequest> getOrderItemsRequestWithIncreasedIphoneQty(Order order) {
        return order.getOrderItems()
                    .stream()
                    .map(oi -> {
                        if (oi.getItem().getItemId().equals(iphone.getItemId())) {
                            int iphoneQuantity = oi.getQuantity();
                            iphoneQuantity++;
                            return TestUtil.getOrderItemDtoRequest(oi.getItem().getItemId(), iphoneQuantity);
                        }
                        return TestUtil.getOrderItemDtoRequest(oi.getItem().getItemId(), oi.getQuantity());
                    })
                    .toList();
    }

    private Map<Long, Integer> getExpectedQtyByItemIdForUpdate(List<OrderItemDtoRequest> updateDto) {
        return updateDto
                .stream()
                .collect(Collectors.toMap(OrderItemDtoRequest::itemId,
                                          OrderItemDtoRequest::quantity));
    }
}
