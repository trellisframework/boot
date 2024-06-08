package net.trellisframework.http.helper;

import com.fasterxml.jackson.databind.*;
import net.trellisframework.core.log.Logger;
import okhttp3.ResponseBody;
import retrofit2.Converter;

import java.io.IOException;

public final class ResponseConverters {
    private ResponseConverters() {
    }

    static final class StringResponseBodyConverter implements Converter<ResponseBody, String> {
        static final StringResponseBodyConverter INSTANCE = new StringResponseBodyConverter();

        @Override
        public String convert(ResponseBody value) throws IOException {
            return value.string();
        }
    }

    static final class BooleanResponseBodyConverter implements Converter<ResponseBody, Boolean> {
        static final BooleanResponseBodyConverter INSTANCE = new BooleanResponseBodyConverter();

        @Override
        public Boolean convert(ResponseBody value) throws IOException {
            return Boolean.valueOf(value.string());
        }
    }

    static final class ByteResponseBodyConverter implements Converter<ResponseBody, Byte> {
        static final ByteResponseBodyConverter INSTANCE = new ByteResponseBodyConverter();

        @Override
        public Byte convert(ResponseBody value) throws IOException {
            return Byte.valueOf(value.string());
        }
    }

    static final class CharacterResponseBodyConverter implements Converter<ResponseBody, Character> {
        static final CharacterResponseBodyConverter INSTANCE = new CharacterResponseBodyConverter();

        @Override
        public Character convert(ResponseBody value) throws IOException {
            String body = value.string();
            if (body.length() != 1) {
                throw new IOException(
                        "Expected body of length 1 for Character conversion but was " + body.length());
            }
            return body.charAt(0);
        }
    }

    static final class DoubleResponseBodyConverter implements Converter<ResponseBody, Double> {
        static final DoubleResponseBodyConverter INSTANCE = new DoubleResponseBodyConverter();

        @Override
        public Double convert(ResponseBody value) throws IOException {
            return Double.valueOf(value.string());
        }
    }

    static final class FloatResponseBodyConverter implements Converter<ResponseBody, Float> {
        static final FloatResponseBodyConverter INSTANCE = new FloatResponseBodyConverter();

        @Override
        public Float convert(ResponseBody value) throws IOException {
            return Float.valueOf(value.string());
        }
    }

    static final class IntegerResponseBodyConverter implements Converter<ResponseBody, Integer> {
        static final IntegerResponseBodyConverter INSTANCE = new IntegerResponseBodyConverter();

        @Override
        public Integer convert(ResponseBody value) throws IOException {
            return Integer.valueOf(value.string());
        }
    }

    static final class LongResponseBodyConverter implements Converter<ResponseBody, Long> {
        static final LongResponseBodyConverter INSTANCE = new LongResponseBodyConverter();

        @Override
        public Long convert(ResponseBody value) throws IOException {
            return Long.valueOf(value.string());
        }
    }

    static final class ShortResponseBodyConverter implements Converter<ResponseBody, Short> {
        static final ShortResponseBodyConverter INSTANCE = new ShortResponseBodyConverter();

        @Override
        public Short convert(ResponseBody value) throws IOException {
            return Short.valueOf(value.string());
        }
    }

    static final class VoidResponseBodyConverter implements Converter<ResponseBody, Void> {
        static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();

        @Override
        public Void convert(ResponseBody value) {
            return null;
        }
    }

    static final class JacksonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
        private final ObjectReader adapter;

        JacksonResponseBodyConverter(ObjectReader adapter) {
            this.adapter = adapter;
        }

        @Override
        public T convert(ResponseBody value) {
            try (value) {
                String stringValue = value.string();
                return adapter.readValue(stringValue);
            } catch (IOException e) {
                Logger.error("JsonParseException", e.getMessage());
                return null;
            }
        }
    }
}