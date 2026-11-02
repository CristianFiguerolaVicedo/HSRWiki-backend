package com.crifigvic.hsrwiki.controller;

import com.crifigvic.hsrwiki.dto.LightconeDto;
import com.crifigvic.hsrwiki.entity.Lightcone;
import com.crifigvic.hsrwiki.service.GameDataLoader;
import com.crifigvic.hsrwiki.service.LightconeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lightcones")
public class LightconeController {

    private final GameDataLoader gameDataLoader;
    private final LightconeService mapper;

    @GetMapping
    public List<LightconeDto> getAllLightcones() {
        return gameDataLoader.getAllLightcones().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LightconeDto> getLightConeById(@PathVariable int id) {
        return toResponse(gameDataLoader.getLightconeById(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<LightconeDto> getLightconeByName(@PathVariable String name) {
        return toResponse(gameDataLoader.getLightconeByName(name));
    }

    private ResponseEntity<LightconeDto> toResponse(Lightcone lightcone) {
        return lightcone == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(mapper.toDto(lightcone));
    }
}
