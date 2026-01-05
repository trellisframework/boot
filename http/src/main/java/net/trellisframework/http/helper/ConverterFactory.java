package net.trellisframework.http.helper;


import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public final class ConverterFactory extends Converter.Factory {

    private final ObjectMapper mapper;

    public static ConverterFactory create() {
        return new ConverterFactory();
    }

    private ConverterFactory() {
        this.mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        if (type == String.class || type == boolean.class || type == Boolean.class || type == byte.class
                || type == Byte.class || type == char.class || type == Character.class || type == double.class
                || type == Double.class || type == float.class || type == Float.class || type == int.class
                || type == Integer.class || type == long.class || type == Long.class || type == short.class || type == Short.class) {
            return RequestBodyConverter.PLAIN_TEXT_INSTANCE;
        }
        return RequestBodyConverter.JSON_INSTANCE;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        if (type == String.class) {
            return ResponseConverters.StringResponseBodyConverter.INSTANCE;
        }
        else if (type == Boolean.class || type == boolean.class) {
            return ResponseConverters.BooleanResponseBodyConverter.INSTANCE;
        }
        else if (type == Byte.class || type == byte.class) {
            return ResponseConverters.ByteResponseBodyConverter.INSTANCE;
        }
        else if (type == Character.class || type == char.class) {
            return ResponseConverters.CharacterResponseBodyConverter.INSTANCE;
        }
        else if (type == Double.class || type == double.class) {
            return ResponseConverters.DoubleResponseBodyConverter.INSTANCE;
        }
        else if (type == Float.class || type == float.class) {
            return ResponseConverters.FloatResponseBodyConverter.INSTANCE;
        }
        else if (type == Integer.class || type == int.class) {
            return ResponseConverters.IntegerResponseBodyConverter.INSTANCE;
        }
        else if (type == Long.class || type == long.class) {
            return ResponseConverters.LongResponseBodyConverter.INSTANCE;
        }
        else if (type == Short.class || type == short.class) {
            return ResponseConverters.ShortResponseBodyConverter.INSTANCE;
        }
        else if (type == Void.class || type == void.class) {
            return ResponseConverters.VoidResponseBodyConverter.INSTANCE;
        }
        return new ResponseConverters.JacksonResponseBodyConverter<>(mapper.readerFor(mapper.getTypeFactory().constructType(type)));
    }

}