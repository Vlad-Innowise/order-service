package by.innowise.internship.orders.controller;

import by.innowise.internship.orders.model.dto.item.ItemRequestDto;
import by.innowise.internship.orders.model.dto.item.ItemResponseDto;
import by.innowise.internship.orders.service.ItemService;
import by.innowise.internship.orders.validation.OnCreate;
import by.innowise.internship.orders.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
@Slf4j
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponseDto> createItem(@RequestBody @Validated({OnCreate.class}) ItemRequestDto dto) {
        log.info("Requested to create an item: {}", dto);
        ItemResponseDto created = itemService.create(dto);
        log.info("Created an item: {}. Sending response to a client", created);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDto> getItem(@PathVariable Long id) {
        log.info("Requested to get an item by id: {}", id);
        ItemResponseDto found = itemService.getById(id);
        log.info("Sending found item to a client: {}", found);
        return ResponseEntity.ok(found);
    }

    @PutMapping
    public ResponseEntity<ItemResponseDto> updateItem(@RequestBody @Validated(OnUpdate.class) ItemRequestDto dto) {
        log.info("Requested to update the item: {}", dto);
        ItemResponseDto updated = itemService.update(dto);
        log.info("Sending updated item to a client: {}", updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        log.info("Requested to delete the item by id: {}", id);
        itemService.delete(id);
        log.info("Item {} deleted successfully", id);
        return ResponseEntity.ok()
                             .build();
    }

}
