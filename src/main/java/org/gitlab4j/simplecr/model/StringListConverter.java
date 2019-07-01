package org.gitlab4j.simplecr.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> list) {

        if (list == null || list.isEmpty()) {
            return null;
        }

        return String.join(",", list);
    }

    @Override
    public List<String> convertToEntityAttribute(String joined) {

        if (joined == null || joined.trim().isEmpty()) {
            return null;
        }

        return new ArrayList<>(Arrays.asList(joined.split(",")));
    }
}
