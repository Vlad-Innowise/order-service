package by.innowise.internship.orders.repository;

import by.innowise.internship.orders.model.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Set;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Set<Item> findDistinctByIdIn(Collection<Long> ids);

}
