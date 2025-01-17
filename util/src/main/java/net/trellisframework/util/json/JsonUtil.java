package net.trellisframework.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.trellisframework.core.log.Logger;
import net.trellisframework.core.message.Messages;
import net.trellisframework.http.exception.NotAcceptableException;
import net.trellisframework.http.exception.NotFoundException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


public class JsonUtil {
    static ObjectMapper mapper;

    public static ObjectMapper getMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                .serializationInclusion(NON_NULL)
                .failOnEmptyBeans(false)
                .failOnUnknownProperties(false)
                .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
    }

    private static ObjectMapper getMapper() {
        if (mapper == null)
            mapper = getMapper(Jackson2ObjectMapperBuilder.json());
        return mapper;
    }

    public static String toString(Object value) {
        return toString(getMapper(), value);
    }

    public static String toString(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            Logger.error("JsonProcessingException", e.getMessage());
            return "";
        }
    }

    public static <T> T toObject(String value, Class<T> valueType) {
        return toObject(getMapper(), value, valueType);
    }

    public static <T> T toObject(ObjectMapper mapper, String value, Class<T> valueType) {
        try {
            return mapper.readValue(value, valueType);
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T, C extends Collection<T>> C toObject(Object value, Class<C> collectionClass, Class<T> valueType) {
        return toObject(getMapper(), value, collectionClass, valueType);
    }

    public static <T, C extends Collection<T>> C toObject(ObjectMapper mapper, Object value, Class<C> collectionClass, Class<T> valueType) {
        try {
            return mapper.convertValue(value, mapper.getTypeFactory().constructCollectionType(collectionClass, valueType));
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T, C extends Collection<T>> C toObject(String value, Class<C> collectionClass, Class<T> valueType) {
        return toObject(getMapper(), value, collectionClass, valueType);
    }

    public static <T, C extends Collection<T>> C toObject(ObjectMapper mapper, String value, Class<C> collectionClass, Class<T> valueType) {
        try {
            return mapper.readValue(value, mapper.getTypeFactory().constructCollectionType(collectionClass, valueType));
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }


    public static <T> T toObject(Object value, Class<T> valueType) {
        return toObject(getMapper(), value, valueType);
    }

    public static <T> T toObject(ObjectMapper mapper, Object value, Class<T> valueType) {
        try {
            return mapper.convertValue(value, valueType);
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T> T toObject(Object value, TypeReference<T> valueTypeRef) {
        return toObject(getMapper(), value, valueTypeRef);
    }

    public static <T> T toObject(ObjectMapper mapper, Object value, TypeReference<T> valueTypeRef) {
        try {
            return mapper.convertValue(value, valueTypeRef);
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T> T toObject(String value, TypeReference<T> valueTypeRef) {
        return toObject(getMapper(), value, valueTypeRef);
    }


    public static <T> T toObject(ObjectMapper mapper, String value, TypeReference<T> valueTypeRef) {
        try {
            return mapper.readValue(value, valueTypeRef);
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T> T toObject(String value, JavaType javaType) {
        return toObject(getMapper(), value, javaType);
    }

    public static <T> T toObject(ObjectMapper mapper, String value, JavaType javaType) {
        try {
            return mapper.readValue(value, javaType);
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T, C extends Collection<T>> C toObject(String value, Class<C> collectionClass, JavaType javaType) {
        return toObject(getMapper(), value, collectionClass, javaType);
    }

    public static <T, C extends Collection<T>> C toObject(ObjectMapper mapper, String value, Class<C> collectionClass, JavaType javaType) {
        try {
            return mapper.readValue(value, mapper.getTypeFactory().constructCollectionType(collectionClass, javaType));
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T, C extends Collection<T>> C toObject(Object value, Class<C> collectionClass, JavaType javaType) {
        return toObject(getMapper(), value, collectionClass, javaType);
    }


    public static <T, C extends Collection<T>> C toObject(ObjectMapper mapper, Object value, Class<C> collectionClass, JavaType javaType) {
        try {
            return mapper.convertValue(value, mapper.getTypeFactory().constructCollectionType(collectionClass, javaType));
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T> void writeToFile(ObjectMapper mapper, String path, T value) {
        try {
            File file = new File(path);
            File dir = file.getParentFile();
            if (!dir.exists())
                dir.mkdirs();
            String json = toString(mapper, value);
            Files.write(Paths.get(path), json.getBytes());
        } catch (IOException e) {
            Logger.error("Write file", e.getMessage());
            throw new NotAcceptableException(Messages.ERROR_ON_SAVE_FILE);
        }
    }

    public static <T> T readFromFile(String path, Class<T> valueType) {
        return readFromFile(getMapper(), path, valueType);
    }

    public static <T> T readFromFile(ObjectMapper mapper, String path, Class<T> valueType) {
        File file = new File(path);
        if (!file.exists() || !file.isFile())
            throw new NotFoundException(Messages.FILE_NOT_FOUND);
        try {
            return mapper.readValue(file, valueType);
        } catch (IOException e) {
            Logger.error("Read file", e.getMessage());
            throw new NotAcceptableException(Messages.ERROR_ON_READ_FILE);
        }
    }
}
