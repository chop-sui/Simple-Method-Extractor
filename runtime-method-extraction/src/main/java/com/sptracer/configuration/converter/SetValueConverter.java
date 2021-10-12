package com.sptracer.configuration.converter;


import com.sptracer.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

public class SetValueConverter<T> extends AbstractCollectionValueConverter<Collection<T>, T> {

    public static final SetValueConverter<String> STRINGS_VALUE_CONVERTER =
            new SetValueConverter<String>(StringValueConverter.INSTANCE);

    public static final SetValueConverter<String> LOWER_STRINGS_VALUE_CONVERTER =
            new SetValueConverter<String>(StringValueConverter.LOWER_CASE);

    public static final ValueConverter<Collection<Integer>> INTEGERS =
            new SetValueConverter<Integer>(IntegerValueConverter.INSTANCE);

    public SetValueConverter(ValueConverter<T> valueConverter) {
        super(valueConverter);
    }

    public SetValueConverter(ValueConverter<T> valueConverter, String delimiter) {
        super(valueConverter, delimiter);
    }

    @Override
    public Collection<T> convert(String s) {
        if (s != null && s.length() > 0) {
            final LinkedHashSet<T> result = new LinkedHashSet<T>();
            for (String split : s.split(delimiter)) {
                result.add(valueConverter.convert(split.trim()));
            }
            return Collections.unmodifiableSet(result);
        }
        return emptySet();
    }

    @Override
    public String toString(Collection<T> value) {
        return StringUtils.asCsv(value);
    }

    public static <T> Set<T> immutableSet(T... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<T>(Arrays.asList(values)));
    }
}
