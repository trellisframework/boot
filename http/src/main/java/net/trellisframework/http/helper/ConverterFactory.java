package net.trellisframework.http.helper;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Converter;
import retrofit2.Retrofit;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.trellisframework.http.helper.ResponseConverters.*;

public final class ConverterFactory extends Converter.Factory {

    private static final Set<Type> PLAIN_REQUEST_TYPES = Set.of(
            String.class, boolean.class, Boolean.class, byte.class, Byte.class,
            char.class, Character.class, double.class, Double.class, float.class, Float.class,
            int.class, Integer.class, long.class, Long.class, short.class, Short.class);

    private static final Map<Type, Converter<ResponseBody, ?>> PRIMITIVE_RESPONSE = Map.ofEntries(
            Map.entry(String.class, StringResponseBodyConverter.INSTANCE),
            Map.entry(Boolean.class, BooleanResponseBodyConverter.INSTANCE),
            Map.entry(boolean.class, BooleanResponseBodyConverter.INSTANCE),
            Map.entry(Byte.class, ByteResponseBodyConverter.INSTANCE),
            Map.entry(byte.class, ByteResponseBodyConverter.INSTANCE),
            Map.entry(Character.class, CharacterResponseBodyConverter.INSTANCE),
            Map.entry(char.class, CharacterResponseBodyConverter.INSTANCE),
            Map.entry(Double.class, DoubleResponseBodyConverter.INSTANCE),
            Map.entry(double.class, DoubleResponseBodyConverter.INSTANCE),
            Map.entry(Float.class, FloatResponseBodyConverter.INSTANCE),
            Map.entry(float.class, FloatResponseBodyConverter.INSTANCE),
            Map.entry(Integer.class, IntegerResponseBodyConverter.INSTANCE),
            Map.entry(int.class, IntegerResponseBodyConverter.INSTANCE),
            Map.entry(Long.class, LongResponseBodyConverter.INSTANCE),
            Map.entry(long.class, LongResponseBodyConverter.INSTANCE),
            Map.entry(Short.class, ShortResponseBodyConverter.INSTANCE),
            Map.entry(short.class, ShortResponseBodyConverter.INSTANCE),
            Map.entry(Void.class, VoidResponseBodyConverter.INSTANCE),
            Map.entry(void.class, VoidResponseBodyConverter.INSTANCE));

    private final ObjectMapper mapper;
    private final ConcurrentMap<Type, Converter<ResponseBody, ?>> responseCache = new ConcurrentHashMap<>();

    public static ConverterFactory create() {
        return create(defaultMapper());
    }

    public static ConverterFactory create(ObjectMapper mapper) {
        if (mapper == null) throw new NullPointerException("mapper is null");
        return new ConverterFactory(mapper);
    }

    private ConverterFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(@NotNull Type type, @NotNull Annotation[] parameterAnnotations, @NotNull Annotation[] methodAnnotations, @NotNull Retrofit retrofit) {
        return PLAIN_REQUEST_TYPES.contains(type) ? RequestBodyConverter.PLAIN_TEXT_INSTANCE : RequestBodyConverter.JSON_INSTANCE;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(@NotNull Type type, @NotNull Annotation[] annotations, @NotNull Retrofit retrofit) {
        Converter<ResponseBody, ?> primitive = PRIMITIVE_RESPONSE.get(type);
        return primitive != null ? primitive : responseCache.computeIfAbsent(type, this::buildJacksonConverter);
    }

    private Converter<ResponseBody, ?> buildJacksonConverter(Type type) {
        return new JacksonResponseBodyConverter<>(mapper.readerFor(mapper.getTypeFactory().constructType(type)));
    }

    private static ObjectMapper defaultMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
    }
}
