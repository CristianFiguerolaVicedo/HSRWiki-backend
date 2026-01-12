package com.crifigvic.hsrwiki.entity;

import com.crifigvic.hsrwiki.util.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Lightcone {
     private Integer id;
     private String name;
     private Integer rarity;
     private Path path;
     private String desc;
     private String icon;
     private String image;
     private String miniIcon;

     private List<LightconeAscension> ascensions;
     private LightconeAbility ability;
}
