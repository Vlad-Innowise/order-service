package by.innowise.internship.orders.service.impl;

import by.innowise.internship.orders.exception.ItemNotFoundException;
import by.innowise.internship.orders.mapper.ItemMapper;
import by.innowise.internship.orders.model.dto.item.ItemRequestDto;
import by.innowise.internship.orders.model.dto.item.ItemResponseDto;
import by.innowise.internship.orders.model.entity.Item;
import by.innowise.internship.orders.repository.ItemRepository;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import by.innowise.internship.orders.util.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    private static final Long MACBOOK_ITEM_ID = 1L;
    private static final Long IPHONE_ITEM_ID = 2L;
    private static final Long MISSING_ITEM_ID = 999L;

    @Mock
    private ItemRepository repository;

    @Mock
    private ItemMapper mapper;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Item macbook;

    private Item iphone;

    @BeforeEach
    void prepareTest() {
        LocalDateTime creationDate = LocalDateTime.now();

        macbook = new Item(MACBOOK_ITEM_ID, "Macbook", BigDecimal.valueOf(2100));
        macbook.setCreatedAt(creationDate);
        macbook.setUpdatedAt(creationDate);
        macbook.setVersion(0L);

        iphone = new Item(IPHONE_ITEM_ID, "Iphone", BigDecimal.valueOf(1100));
        iphone.setCreatedAt(creationDate);
        iphone.setUpdatedAt(creationDate);
        iphone.setVersion(0L);
    }

    @Test
    void createHappyPass() {

        ItemRequestDto itemRequest = new ItemRequestDto(null, macbook.getName(), macbook.getPrice());
        ItemResponseDto expectedResult = TestUtil.mapToItemResponseDto(macbook);

        doReturn(macbook)
                .when(mapper).toEntity(any(ItemRequestDto.class));
        doReturn(macbook)
                .when(repository).saveAndFlush(any(Item.class));
        doReturn(expectedResult)
                .when(mapper).toDto(any(Item.class));

        ItemResponseDto actualResult = itemService.create(itemRequest);

        assertThat(actualResult).isEqualTo(expectedResult);
        verify(mapper).toEntity(any(ItemRequestDto.class));
        verify(repository).saveAndFlush(any(Item.class));
        verify(mapper).toDto(any(Item.class));
    }

    @Test
    void getByIdHappyPass() {
        ItemResponseDto expectedResult = TestUtil.mapToItemResponseDto(macbook);

        doReturn(Optional.of(macbook))
                .when(repository).findById(anyLong());
        doReturn(expectedResult)
                .when(mapper).toDto(any(Item.class));

        ItemResponseDto actualResult = itemService.getById(MACBOOK_ITEM_ID);

        assertThat(actualResult).isEqualTo(expectedResult);
        verify(repository).findById(anyLong());
        verify(mapper).toDto(any(Item.class));
    }

    @Test
    void shouldThrowExceptionWhenItemNotFoundById() {
        doReturn(Optional.empty())
                .when(repository).findById(anyLong());

        assertThrowsExactly(ItemNotFoundException.class, () -> itemService.getById(MISSING_ITEM_ID));
        verify(repository).findById(anyLong());
    }

    @Test
    void getAllByIdsFullOrPartially() {

        Collection<Long> itemsToFind = List.of(MACBOOK_ITEM_ID, IPHONE_ITEM_ID, MISSING_ITEM_ID);
        Set<ItemSnapshot> expectedResult = Set.of(convertItemToItemSnapshot(macbook),
                                                  convertItemToItemSnapshot(iphone));

        doReturn(Set.of(macbook, iphone))
                .when(repository).findDistinctByIdIn(anyCollection());
        mockToItemSnapshotDtoMapper();


        Set<ItemSnapshot> actualResult = itemService.getAllByIds(itemsToFind);

        assertThat(actualResult).containsExactlyInAnyOrderElementsOf(expectedResult);
        verify(repository).findDistinctByIdIn(anyCollection());
        verify(mapper, times(expectedResult.size())).toSnapshot(any(Item.class));
    }

    @Test
    void updateItemHappyPass() {
        BigDecimal updatedItemPrice = BigDecimal.valueOf(1500);
        ItemRequestDto updateDto = new ItemRequestDto(MACBOOK_ITEM_ID, macbook.getName(), updatedItemPrice);

        //updating entity
        macbook.setPrice(updateDto.price());
        long currentVersion = macbook.getVersion();
        LocalDateTime updatedDate = LocalDateTime.now();
        macbook.setVersion(++currentVersion);
        macbook.setUpdatedAt(updatedDate);

        ItemResponseDto expectedResult = TestUtil.mapToItemResponseDto(macbook);

        doReturn(Optional.of(macbook))
                .when(repository).findById(anyLong());
        doReturn(macbook)
                .when(mapper).update(any(Item.class), any(ItemRequestDto.class));
        doReturn(macbook)
                .when(repository).saveAndFlush(any(Item.class));
        doReturn(expectedResult)
                .when(mapper).toDto(any(Item.class));

        ItemResponseDto actualResult = itemService.update(updateDto);

        assertThat(actualResult).isEqualTo(expectedResult);
        verify(repository).findById(anyLong());
        verify(mapper).update(any(Item.class), any(ItemRequestDto.class));
        verify(repository).saveAndFlush(any(Item.class));
        verify(mapper).toDto(any(Item.class));
    }

    @Test
    void shouldThrowItemNotFoundExceptionWhenUpdateDtoContainsMissingItemId() {
        BigDecimal updatedItemPrice = BigDecimal.valueOf(1500);
        ItemRequestDto updateDto = new ItemRequestDto(MISSING_ITEM_ID, macbook.getName(), updatedItemPrice);

        doReturn(Optional.empty())
                .when(repository).findById(anyLong());

        assertThrowsExactly(ItemNotFoundException.class, () -> itemService.update(updateDto));
        verify(repository).findById(anyLong());
    }

    @Test
    void shouldDeleteItemByItemIdWhenExists() {
        doReturn(Optional.of(macbook))
                .when(repository).findById(anyLong());
        doNothing()
                .when(repository).delete(any(Item.class));

        itemService.delete(MACBOOK_ITEM_ID);

        verify(repository).findById(anyLong());
        verify(repository).delete(any(Item.class));
    }

    @Test
    void shouldThrowItemNotFoundExceptionWhenDeleteItemInCaseItemNotExists() {
        doReturn(Optional.empty())
                .when(repository).findById(anyLong());

        assertThrowsExactly(ItemNotFoundException.class, () -> itemService.delete(MISSING_ITEM_ID));
        verify(repository).findById(anyLong());
    }

    private void mockToItemSnapshotDtoMapper() {
        doAnswer(invocation -> {
            Item item = invocation.getArgument(0, Item.class);
            return convertItemToItemSnapshot(item);
        }).when(mapper).toSnapshot(any(Item.class));
    }

    private ItemSnapshot convertItemToItemSnapshot(Item item) {
        return ItemSnapshot.builder()
                           .itemId(item.getId())
                           .itemName(item.getName())
                           .itemPrice(item.getPrice())
                           .build();
    }

}
