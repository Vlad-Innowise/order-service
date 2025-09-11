package by.innowise.internship.orders.service.impl;

import by.innowise.internship.orders.exception.ItemNotFoundException;
import by.innowise.internship.orders.mapper.ItemMapper;
import by.innowise.internship.orders.model.dto.item.ItemRequestDto;
import by.innowise.internship.orders.model.dto.item.ItemResponseDto;
import by.innowise.internship.orders.model.entity.Item;
import by.innowise.internship.orders.repository.ItemRepository;
import by.innowise.internship.orders.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository repository;
    private final ItemMapper mapper;

    @Transactional
    @Override
    public ItemResponseDto create(ItemRequestDto dto) {
        Item toSave = mapper.toEntity(dto);
        log.info("Mapped to item entity: {}", dto);
        repository.saveAndFlush(toSave);
        return mapper.toDto(toSave);
    }

    @Transactional(readOnly = true)
    @Override
    public ItemResponseDto getById(Long id) {
        return mapper.toDto(getItemById(id));
    }

    @Transactional
    @Override
    public ItemResponseDto update(ItemRequestDto dto) {
        Item toUpdate = getItemById(dto.id());
        Item updated = mapper.update(toUpdate, dto);
        log.info("Updated entity: {}", updated);
        repository.saveAndFlush(updated);
        return mapper.toDto(updated);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Item found = getItemById(id);
        log.info("Invoking item repository to delete the item: [{}]", found);
        repository.delete(found);
    }

    private Item getItemById(Long id) {
        log.info("Invoking item repository to find by id: [{}]", id);
        Item found = repository.findById(id)
                               .orElseThrow(() -> new ItemNotFoundException(
                                       "Not found the item with id: [%s]".formatted(id), HttpStatus.NOT_FOUND));
        log.info("Retrieved the item from DB: {}", found);
        return found;
    }

}
