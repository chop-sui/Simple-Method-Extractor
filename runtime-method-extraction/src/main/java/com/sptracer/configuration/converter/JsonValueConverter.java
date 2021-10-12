package com.sptracer.configuration.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sptracer.configuration.converter.AbstractValueConverter;

import java.io.IOException;

public class JsonValueConverter<T> extends AbstractValueConverter<T> {

    private final TypeReference<T> typeReference;
    private final ObjectMapper objectMapper;

    public JsonValueConverter(TypeReference<T> typeReference) {
        this.typeReference = typeReference;
        objectMapper = new ObjectMapper();
    }

    @Override
    public T convert(String s) {
        try {
            return objectMapper.readValue(s, typeReference);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toString(T value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
