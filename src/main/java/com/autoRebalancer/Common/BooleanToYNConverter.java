package com.autoRebalancer.Common;

import jakarta.persistence.AttributeConverter;

public class BooleanToYNConverter implements AttributeConverter<Boolean, Character> {

    @Override
    public Character convertToDatabaseColumn(Boolean attribute) {
        // Entity의 boolean 값을 DB의 Character로 변환
        // true -> 'Y', false -> 'N', null -> null
        return (attribute != null && attribute) ? 'Y' : 'N';
    }

    @Override
    public Boolean convertToEntityAttribute(Character dbData) {
        // DB의 Character 값을 Entity의 boolean으로 변환
        // 'Y' -> true, 그 외(N, null 등) -> false
        return 'Y' == dbData;
    }
}
