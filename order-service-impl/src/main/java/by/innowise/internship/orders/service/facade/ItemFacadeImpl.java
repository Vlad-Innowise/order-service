package by.innowise.internship.orders.service.facade;

import by.innowise.internship.orders.service.ItemService;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemFacadeImpl implements ItemFacade {

    private final ItemService itemService;

    @Transactional(readOnly = true)
    @Override
    public Set<ItemSnapshot> getByIds(Collection<Long> itemIds) {
        log.info("Invoking item service to get {} items", itemIds.size());
        Set<ItemSnapshot> found = itemService.getAllByIds(itemIds);
        log.info("Retrieved item snapshots: {}", found);
        return found;
    }

}
