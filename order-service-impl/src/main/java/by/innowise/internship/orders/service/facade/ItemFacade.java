package by.innowise.internship.orders.service.facade;

import by.innowise.internship.orders.service.dto.ItemSnapshot;

import java.util.Collection;
import java.util.Set;

public interface ItemFacade {

    Set<ItemSnapshot> getByIds(Collection<Long> itemIds);

}
