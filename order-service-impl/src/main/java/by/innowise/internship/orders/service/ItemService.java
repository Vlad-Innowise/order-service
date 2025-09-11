package by.innowise.internship.orders.service;

import by.innowise.internship.orders.model.dto.item.ItemRequestDto;
import by.innowise.internship.orders.model.dto.item.ItemResponseDto;

public interface ItemService {

    ItemResponseDto create(ItemRequestDto dto);

    ItemResponseDto getById(Long id);

    ItemResponseDto update(ItemRequestDto dto);

    void delete(Long id);
}
