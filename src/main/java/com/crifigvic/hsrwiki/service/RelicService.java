package com.crifigvic.hsrwiki.service;

import com.crifigvic.hsrwiki.dto.RelicDto;
import com.crifigvic.hsrwiki.entity.RelicSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RelicService {

    public RelicDto toDto(RelicSet relicSet) {
        return new RelicDto(
                relicSet.getName(),
                relicSet.getPieces(),
                relicSet.getDesc(),
                relicSet.getModifiers()
        );
    }
}
