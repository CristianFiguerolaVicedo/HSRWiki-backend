package com.crifigvic.hsrwiki.controller;

import com.crifigvic.hsrwiki.dto.RelicDto;
import com.crifigvic.hsrwiki.entity.RelicSet;
import com.crifigvic.hsrwiki.service.GameDataLoader;
import com.crifigvic.hsrwiki.service.RelicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/relics")
@RequiredArgsConstructor
public class RelicController {

    private final GameDataLoader gameDataLoader;
    private final RelicService mapper;

    @GetMapping
    public List<RelicDto> getAllRelicSets() {
        return gameDataLoader.getAllRelicSets().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{name}")
    public ResponseEntity<RelicDto> getRelicSetByName(@PathVariable String name) {
        RelicSet relicSet = gameDataLoader.getRelicSetByName(name);
        if (relicSet == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.toDto(relicSet));
    }
}
