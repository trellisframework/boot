package net.trellisframework.workflow.temporal.util;

import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributeUpdate;
import io.temporal.common.converter.GlobalDataConverter;
import io.temporal.common.converter.DataConverterException;
import io.temporal.common.converter.EncodedValues;
import net.trellisframework.util.json.JsonUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TemporalAttributes {

    private static final int MAX_VALUE_LENGTH = 255;

    private TemporalAttributes() {
    }

    public static Map<?, ?> toMap(Object value) {
        if (value instanceof Map<?, ?> map) return map;
        if (value instanceof EncodedValues encodedValues) {
            var payloads = encodedValues.toPayloads();
            if (payloads.isEmpty()) return null;
            var converter = GlobalDataConverter.get();
            for (int index = 0; index < payloads.get().getPayloadsCount(); index++) {
                try {
                    Map<?, ?> decoded = converter.fromPayloads(index, payloads, Map.class, Map.class);
                    if (decoded != null) return decoded;
                } catch (DataConverterException ignored) {
                    // Non-map workflow arguments, such as the workflow class name, are skipped.
                }
            }
            return null;
        }
        if (value == null || value instanceof CharSequence || value instanceof Number || value instanceof Boolean) return null;
        return JsonUtil.toObject(value, Map.class);
    }

    public static String normalize(Object value) {
        if (value == null) return null;
        String normalized = value.toString().trim();
        if (normalized.isEmpty() || normalized.length() > MAX_VALUE_LENGTH) return null;
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static void add(List<SearchAttributeUpdate<?>> updates, SearchAttributeKey<String> key, String value) {
        if (value != null) {
            updates.add(key.valueSet(value));
        }
    }
}
