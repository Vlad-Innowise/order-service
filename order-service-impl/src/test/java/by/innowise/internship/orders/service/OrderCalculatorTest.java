package by.innowise.internship.orders.service;

import by.innowise.internship.orders.model.entity.Order;
import by.innowise.internship.orders.model.entity.OrderItem;
import by.innowise.internship.orders.model.entity.OrderStatus;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import by.innowise.internship.orders.util.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrderCalculatorTest {

    private OrderCalculator orderCalculator = new OrderCalculator();
    private ItemSnapshot macbook;
    private ItemSnapshot iphone;
    private ItemSnapshot airpods;
    private OrderItem macbookOrderItem;
    private OrderItem iphoneOrderItem;
    private OrderItem airpodsOrderItem;
    private Order order;

    @BeforeEach
    void prepareTest() {
        macbook = TestUtil.getItemSnapshot(1L, "Macbook", BigDecimal.valueOf(2100));
        iphone = TestUtil.getItemSnapshot(2L, "Iphone", BigDecimal.valueOf(1100));
        airpods = TestUtil.getItemSnapshot(3L, "Airpods", BigDecimal.valueOf(250));

        macbookOrderItem = TestUtil.getOrderItem(UUID.randomUUID(), macbook, 1);
        iphoneOrderItem = TestUtil.getOrderItem(UUID.randomUUID(), iphone, 2);
        airpodsOrderItem = TestUtil.getOrderItem(UUID.randomUUID(), airpods, 2);

        order = TestUtil.getOrderWithoutOrderItems(UUID.randomUUID(), 1L, OrderStatus.PENDING, LocalDateTime.now());

        order.addOrderItem(macbookOrderItem);
        order.addOrderItem(iphoneOrderItem);
        order.addOrderItem(airpodsOrderItem);
    }

    @Test
    void calculateSubtotals_ShouldReturnCorrectMap() {

        BigDecimal macbookSubtotal = BigDecimal.valueOf(2100);
        BigDecimal iphoneSubtotal = BigDecimal.valueOf(2200); // 2 * 1100
        BigDecimal airpodsSubtotal = BigDecimal.valueOf(500); // 2 * 250

        Map<Long, BigDecimal> expectedResult = Map.of(macbook.getItemId(), macbookSubtotal,
                                                      iphone.getItemId(), iphoneSubtotal,
                                                      airpods.getItemId(), airpodsSubtotal);

        Map<Long, BigDecimal> actualResult = orderCalculator.calculateSubtotals(order);

        assertThat(actualResult)
                .containsExactlyInAnyOrderEntriesOf(expectedResult);
    }

    @Test
    void checkOrderTotalCalculation() {
        BigDecimal expectedResult = BigDecimal.valueOf(4800); //2100 + 2200 + 500

        BigDecimal actualResult = orderCalculator.calculateOrderTotal(order);

        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    void orderTotalShouldEqualsToZeroWhenNoOrderItems() {
        Order zeroOrder = new Order();

        BigDecimal actualResult = orderCalculator.calculateOrderTotal(zeroOrder);

        assertThat(actualResult).isEqualTo(BigDecimal.ZERO);
    }


}