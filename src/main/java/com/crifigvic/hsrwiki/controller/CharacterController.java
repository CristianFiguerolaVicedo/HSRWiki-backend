package com.crifigvic.hsrwiki.controller;

import com.crifigvic.hsrwiki.dto.CharacterDto;
import com.crifigvic.hsrwiki.entity.Character;
import com.crifigvic.hsrwiki.service.CharacterBuildService;
import com.crifigvic.hsrwiki.service.CharacterService;
import com.crifigvic.hsrwiki.service.GameDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/characters")
public class CharacterController {

    private final GameDataLoader loader;
    private final CharacterService mapper;
    private final CharacterBuildService characterBuildService;

    @GetMapping
    public List<CharacterDto> getAll() {
        return loader.getAllCharacters().stream()
                .sorted(Comparator.comparing(Character::getName))
                .map(mapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharacterDto> getById(@PathVariable int id) {
        Character character = loader.getById(id);
        return character == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(mapper.toDto(character));
    }

    @GetMapping("/builds")
    public ResponseEntity<Map<String, Object>> getBuildInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalBuilds", characterBuildService.getBuildCount());
        response.put("availableCharacters", characterBuildService.getCharactersWithBuilds());
        return ResponseEntity.ok(response);
    }
}
