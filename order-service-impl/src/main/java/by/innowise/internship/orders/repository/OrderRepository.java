package by.innowise.internship.orders.repository;

import by.innowise.internship.orders.model.entity.Order;
import by.innowise.internship.orders.model.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id=:id AND o.userId=:userId")
    Optional<Order> findByIdAndUserIdFetchOrderItems(@Param("id") UUID id,
                                                     @Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.id IN(:orderIds) AND o.userId=:userId")
    Page<Order> findPageByIdsAndUserId(@Param("orderIds") Collection<UUID> orderIds,
                                       @Param("userId") Long userId,
                                       Pageable pageable);

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByIdIn(Collection<UUID> orderIds);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.status=:status AND o.userId=:userId")
    List<Order> findAllByStatusAndUserIdFetchOrderItems(@Param("status") OrderStatus status,
                                                        @Param("userId") Long userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.status=:status")
    List<Order> findAllByStatusWithAllOrderItems(@Param("status") OrderStatus status);

}
