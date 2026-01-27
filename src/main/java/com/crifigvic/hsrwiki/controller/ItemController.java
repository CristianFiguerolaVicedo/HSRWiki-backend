package com.crifigvic.hsrwiki.controller;

import com.crifigvic.hsrwiki.dto.ItemDto;
import com.crifigvic.hsrwiki.entity.Item;
import com.crifigvic.hsrwiki.service.GameDataLoader;
import com.crifigvic.hsrwiki.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
public class ItemController {

    private final GameDataLoader loader;
    private final ItemService mapper;

    @GetMapping
    public List<ItemDto> getAllItems() {
        return loader.getAllItems().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public ItemDto getItemId(@PathVariable String id) {
        return mapper.toDto(loader.getItemById(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ItemDto> getItemByName(@PathVariable String name) {
        Item item = loader.getItemByName(name);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.toDto(item));
    }
}
