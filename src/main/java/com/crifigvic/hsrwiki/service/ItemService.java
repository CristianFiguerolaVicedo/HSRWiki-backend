package com.crifigvic.hsrwiki.service;

import com.crifigvic.hsrwiki.dto.ItemDto;
import com.crifigvic.hsrwiki.entity.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemService {

    public ItemDto toDto(Item item) {
        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getType(),
                item.getSub_type(),
                item.getRarity(),
                item.getIcon(),
                item.getCome_from()
        );
    }
}
