package com.focisolutions.takehomeexercise.config;

import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
final class CaseInsensitiveEnumConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(final Class<T> targetType) {
        return source -> (T) Enum.valueOf((Class<? extends Enum>) targetType, source.trim().toUpperCase(Locale.ROOT));
    }
}
