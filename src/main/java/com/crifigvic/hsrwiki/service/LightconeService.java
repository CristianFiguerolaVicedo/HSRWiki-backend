package com.crifigvic.hsrwiki.service;

import com.crifigvic.hsrwiki.dto.LightconeDto;
import com.crifigvic.hsrwiki.entity.Lightcone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LightconeService {

    public LightconeDto toDto(Lightcone lightcone) {
        return new LightconeDto(
                lightcone.getId(),
                lightcone.getName(),
                lightcone.getRarity(),
                lightcone.getPath(),
                lightcone.getDesc(),
                lightcone.getIcon(),
                lightcone.getImage(),
                lightcone.getMiniIcon(),
                lightcone.getAscensions(),
                lightcone.getAbility()
        );
    }
}
