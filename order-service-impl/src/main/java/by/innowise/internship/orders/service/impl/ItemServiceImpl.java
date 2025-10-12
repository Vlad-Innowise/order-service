package by.innowise.internship.orders.service.impl;

import by.innowise.internship.orders.exception.ItemNotFoundException;
import by.innowise.internship.orders.mapper.ItemMapper;
import by.innowise.internship.orders.model.dto.item.ItemRequestDto;
import by.innowise.internship.orders.model.dto.item.ItemResponseDto;
import by.innowise.internship.orders.model.entity.Item;
import by.innowise.internship.orders.repository.ItemRepository;
import by.innowise.internship.orders.service.ItemService;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


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

    @Transactional(readOnly = true)
    @Override
    public List<ItemResponseDto> getAll() {
        log.info("Requested all items from DB");
        List<Item> foundItems = repository.findAll();
        log.info("Items found: {}", foundItems.size());
        return foundItems.stream()
                         .map(mapper::toDto)
                         .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public Set<ItemSnapshot> getAllByIds(Collection<Long> idsToFind) {
        log.info("Going to DB to find items with ids: {}", idsToFind);
        Set<Item> foundItems = repository.findDistinctByIdIn(idsToFind);
        log.info("Found Items: {}", foundItems);
        return foundItems.stream()
                         .map(mapper::toSnapshot)
                         .collect(Collectors.toSet());
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
