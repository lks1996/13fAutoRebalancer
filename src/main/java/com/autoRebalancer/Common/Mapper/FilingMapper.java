package com.autoRebalancer.Common.Mapper;

import com.autoRebalancer._13f.Dto.Filing;
import com.autoRebalancer._13f.Entity.FilingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface FilingMapper {

    @Mapping(target = "holdings", ignore = true)
    FilingEntity toEntity(Filing filingDto);

    @Mapping(target = "holdings", ignore = true)
    List<FilingEntity> toEntityList(List<Filing> filingDtos);

    @Mapping(target = "periodOfReport", source = "periodOfReport", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "filedAsOfDate", source = "filedAsOfDate", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "dateAsOfChange", source = "dateAsOfChange", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "effectivenessDate", source = "effectivenessDate", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "confDateDeniedExpired", source = "confDateDeniedExpired", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "amendmentDateReported", source = "amendmentDateReported", dateFormat = "yyyy-MM-dd")
    Filing toDto(FilingEntity filingEntity);

    default String map(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE); // "yyyy-MM-dd"
    }

    default LocalDate map(String dateString) {
        if (dateString == null || dateString.isEmpty() || "0001-01-01".equals(dateString)) {
            return null;
        }
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            System.err.println("Failed to parse date string: " + dateString);
            return null;
        }
    }
}
