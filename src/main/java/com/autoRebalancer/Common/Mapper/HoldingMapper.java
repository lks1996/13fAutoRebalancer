package com.autoRebalancer.Common.Mapper;

import com.autoRebalancer._13f.Dto.Holding;
import com.autoRebalancer._13f.Entity.HoldingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HoldingMapper {

    @Mapping(target = "holdingId", ignore = true)
    @Mapping(target = "filing", ignore = true)
    @Mapping(source = "sshPrnamt", target = "shares")
    HoldingEntity toEntity(Holding holdingDto);

    List<HoldingEntity> toEntityList(List<Holding> holdingDtos);

    @Mapping(source = "shares", target = "sshPrnamt")
    @Mapping(target = "accessionNumber", ignore = true)
    @Mapping(target = "cik", ignore = true)
    Holding toDto(HoldingEntity holdingEntity);

    List<Holding> toDtoList(List<HoldingEntity> holdingEntities);
}
