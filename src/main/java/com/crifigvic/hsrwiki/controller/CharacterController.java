package com.crifigvic.hsrwiki.controller;

import com.crifigvic.hsrwiki.dto.CharacterDto;
import com.crifigvic.hsrwiki.entity.Character;
import com.crifigvic.hsrwiki.service.CharacterBuildService;
import com.crifigvic.hsrwiki.service.CharacterService;
import com.crifigvic.hsrwiki.service.GameDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/characters")
public class CharacterController {

    private final GameDataLoader loader;
    private final CharacterService mapper;
    private final CharacterBuildService characterBuildService;

    @GetMapping
    public List<CharacterDto> getAll() {
        return loader.getAllCharacters().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public CharacterDto getById(@PathVariable int id) {
        return mapper.toDto(loader.getById(id));
    }

    @GetMapping("/builds")
    public ResponseEntity<Map<String, Object>> getBuildInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalBuilds", characterBuildService.getBuildCount());
        response.put("availableCharacters", characterBuildService.getCharactersWithBuilds());
        return ResponseEntity.ok(response);
    }
}
