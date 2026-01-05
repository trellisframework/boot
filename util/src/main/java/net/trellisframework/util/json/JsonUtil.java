package net.trellisframework.util.json;


import net.trellisframework.core.log.Logger;
import net.trellisframework.core.message.Messages;
import net.trellisframework.http.exception.BadRequestException;
import net.trellisframework.http.exception.NotAcceptableException;
import net.trellisframework.http.exception.NotFoundException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;


public class JsonUtil {
    static ObjectMapper mapper;

    public static ObjectMapper getMapper(JsonMapper.Builder builder) {
        return builder
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
    }

    private static ObjectMapper getMapper() {
        if (mapper == null)
            mapper = getMapper(JsonMapper.builder());
        return mapper;
    }

    public static String toString(Object value) {
        return toString(getMapper(), value);
    }

    public static String toString(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return "";
        }
    }

    public static <T> T toObject(String value, Class<T> valueType) {
        return toObject(getMapper(), value, valueType);
    }

    public static <T> T toObject(ObjectMapper mapper, String value, Class<T> valueType) {
        try {
            return mapper.readValue(value, valueType);
        } catch (Exception e) {
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
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }


    public static <T> T toObject(Object value, Class<T> valueType) {
        return toObject(getMapper(), value, valueType);
    }

    public static <I, O> O toObject(I source, O destination) {
        return toObject(getMapper(), source, destination);
    }

    public static <I, O> O toObject(ObjectMapper mapper, I source, O destination) {
        try {
            return mapper.readerForUpdating(destination).readValue(mapper.writeValueAsString(source));
        } catch (Exception e) {
            throw new BadRequestException(net.trellisframework.util.constant.Messages.CAN_NOT_DESERIALIZE_OBJECT);
        }
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            Logger.error("Read file", e.getMessage());
            throw new NotAcceptableException(Messages.ERROR_ON_READ_FILE);
        }
    }
}
