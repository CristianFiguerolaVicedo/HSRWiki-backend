package com.crifigvic.hsrwiki.controller;

import com.crifigvic.hsrwiki.dto.CharacterDto;
import com.crifigvic.hsrwiki.service.CharacterService;
import com.crifigvic.hsrwiki.service.GameDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/characters")
public class CharacterController {

    private final GameDataLoader loader;
    private final CharacterService mapper;

    @GetMapping
    public List<CharacterDto> getAll() {
        return loader.getAllCharacters().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public CharacterDto getById(@PathVariable int id) {
        return mapper.toDto(loader.getById(id));
    }
}
