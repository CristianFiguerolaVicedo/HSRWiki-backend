package com.crifigvic.hsrwiki.service;

import com.crifigvic.hsrwiki.dto.CharacterDto;
import com.crifigvic.hsrwiki.entity.Character;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CharacterService {

    public CharacterDto toDto(Character character) {
        return new CharacterDto(
                character.getId(),
                character.getName(),
                character.getRarity(),
                character.getElement().name(),
                character.getPath().name(),
                character.getIcon(),
                character.getAscension(),
                character.getEidolons(),
                character.getSkills(),
                character.getTraces()
        );
    }
}
