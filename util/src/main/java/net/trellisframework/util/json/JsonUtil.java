package net.trellisframework.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import net.trellisframework.core.log.Logger;
import net.trellisframework.core.message.Messages;
import net.trellisframework.http.exception.NotAcceptableException;
import net.trellisframework.http.exception.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;


public class JsonUtil {

    static ObjectMapper mapper = new ObjectMapper();

    public static String toString(Object value) {
        try {
            return getMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            Logger.error("JsonProcessingException", e.getMessage());
            return "";
        }
    }

    private static ObjectMapper getMapper() {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        return mapper;
    }

    public static <T> T toObject(String value, Class<T> valueType) {
        try {
            return getMapper().readValue(value, valueType);
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T, C extends Collection<T>> C toObject(Object value, Class<C> collectionClass, Class<T> valueType) {
        try {
            return getMapper().convertValue(value, getMapper().getTypeFactory().constructCollectionType(collectionClass, valueType));
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }


    public static <T, C extends Collection<T>> C toObject(String value, Class<C> collectionClass, Class<T> valueType) {
        try {
            return getMapper().readValue(value, getMapper().getTypeFactory().constructCollectionType(collectionClass, valueType));
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T> T toObject(Object value, Class<T> valueType) {
        try {
            return getMapper().convertValue(value, valueType);
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T> T toObject(Object value, TypeReference<T> valueTypeRef) {
        try {
            return getMapper().convertValue(value, valueTypeRef);
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T> T toObject(String value, TypeReference<T> valueTypeRef) {
        try {
            return getMapper().readValue(value, valueTypeRef);
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T> T toObject(String value, JavaType javaType) {
        try {
            return getMapper().readValue(value, javaType);
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T, C extends Collection<T>> C toObject(String value, Class<C> collectionClass, JavaType javaType) {
        try {
            return getMapper().readValue(value, getMapper().getTypeFactory().constructCollectionType(collectionClass, javaType));
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T, C extends Collection<T>> C toObject(Object value, Class<C> collectionClass, JavaType javaType) {
        try {
            return getMapper().convertValue(value, getMapper().getTypeFactory().constructCollectionType(collectionClass, javaType));
        } catch (Exception e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

    public static <T> void writeToFile(String path, T value) throws NotAcceptableException {
        try {
            File file = new File(path);
            File dir = file.getParentFile();
            if (!dir.exists())
                dir.mkdirs();
            String json = toString(value);
            Files.write(Paths.get(path), json.getBytes());
        } catch (IOException e) {
            Logger.error("Write file", e.getMessage());
            throw new NotAcceptableException(Messages.ERROR_ON_SAVE_FILE);
        }
    }

    public static <T> T readFromFile(String path, Class<T> valueType) throws NotFoundException, NotAcceptableException {
        File file = new File(path);
        if (!file.exists() || !file.isFile())
            throw new NotFoundException(Messages.FILE_NOT_FOUND);
        try {
            return getMapper().readValue(file, valueType);
        } catch (IOException e) {
            Logger.error("Read file", e.getMessage());
            throw new NotAcceptableException(Messages.ERROR_ON_READ_FILE);
        }
    }
}
