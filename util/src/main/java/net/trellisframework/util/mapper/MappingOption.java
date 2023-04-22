package net.trellisframework.util.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dozer.loader.api.TypeMappingOption;
import org.dozer.loader.api.TypeMappingOptions;

@Getter
@AllArgsConstructor
public enum MappingOption {
    MAP_NULL(TypeMappingOptions.mapNull(true)),
    NOT_MAP_NULL(TypeMappingOptions.mapNull(false)),
    MAP_EMPTY_STRING(TypeMappingOptions.mapEmptyString(true)),
    NOT_MAP_EMPTY_STRING(TypeMappingOptions.mapEmptyString(false)),
    TRIM_STRINGS(TypeMappingOptions.trimStrings(true)),
    NOT_TRIM_STRINGS(TypeMappingOptions.trimStrings(false)),
    STOP_ON_ERRORS(TypeMappingOptions.stopOnErrors(true)),
    NOT_STOP_ON_ERRORS(TypeMappingOptions.stopOnErrors(false)),;


    final TypeMappingOption mapping;

}
