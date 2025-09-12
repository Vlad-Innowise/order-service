package by.innowise.internship.orders.service;

import by.innowise.internship.orders.model.dto.item.ItemRequestDto;
import by.innowise.internship.orders.model.dto.item.ItemResponseDto;
import by.innowise.internship.orders.service.dto.ItemSnapshot;

import java.util.Collection;
import java.util.Set;

public interface ItemService {

    ItemResponseDto create(ItemRequestDto dto);

    ItemResponseDto getById(Long id);

    Set<ItemSnapshot> getAllByIds(Collection<Long> idsToFind);

    ItemResponseDto update(ItemRequestDto dto);

    void delete(Long id);
}
