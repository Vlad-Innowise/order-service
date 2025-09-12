package by.innowise.internship.orders.mapper;

import by.innowise.internship.orders.model.dto.item.ItemRequestDto;
import by.innowise.internship.orders.model.dto.item.ItemResponseDto;
import by.innowise.internship.orders.model.entity.Item;
import by.innowise.internship.orders.service.dto.ItemSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = BaseMapper.class)
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Item toEntity(ItemRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Item update(@MappingTarget Item e, ItemRequestDto d);

    ItemResponseDto toDto(Item e);

    default ItemSnapshot toSnapshot(Item e) {
        return ItemSnapshot.builder()
                           .itemId(e.getId())
                           .itemName(e.getName())
                           .itemPrice(e.getPrice())
                           .build();
    }
}
