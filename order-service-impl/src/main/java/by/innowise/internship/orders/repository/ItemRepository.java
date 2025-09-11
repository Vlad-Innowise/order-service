package by.innowise.internship.orders.repository;

import by.innowise.internship.orders.model.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
