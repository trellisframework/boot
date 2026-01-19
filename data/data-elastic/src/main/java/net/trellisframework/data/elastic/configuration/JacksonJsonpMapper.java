package net.trellisframework.data.elastic.configuration;

import co.elastic.clients.json.JsonpDeserializer;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpMapperBase;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class JacksonJsonpMapper extends JsonpMapperBase {

    private final ObjectMapper objectMapper;
    private final JsonProvider jsonProvider;
    private final Map<String, Object> attributes;

    public JacksonJsonpMapper() {
        this(createDefaultMapper(), new HashMap<>());
    }

    public JacksonJsonpMapper(ObjectMapper objectMapper) {
        this(objectMapper, new HashMap<>());
    }

    private JacksonJsonpMapper(ObjectMapper objectMapper, Map<String, Object> attributes) {
        this.objectMapper = objectMapper;
        this.jsonProvider = JsonProvider.provider();
        this.attributes = attributes;
    }

    private static ObjectMapper createDefaultMapper() {
        return JsonMapper.builder()
                .configure(tools.jackson.databind.SerializationFeature.INDENT_OUTPUT, false)
                .configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    @Override
    public JsonProvider jsonProvider() {
        return jsonProvider;
    }

    @Override
    public <T> T deserialize(JsonParser parser, Type type) {
        try {
            // Convert JsonParser to string
            StringWriter sw = new StringWriter();
            JsonGenerator gen = jsonProvider.createGenerator(sw);
            transferAll(parser, gen);
            gen.close();

            // Parse with Jackson 3
            return objectMapper.readValue(sw.toString(), objectMapper.constructType(type));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize", e);
        }
    }

    @Override
    public <T> void serialize(T value, JsonGenerator generator) {
        if (value instanceof co.elastic.clients.json.JsonpSerializable jsonp) {
            jsonp.serialize(generator, this);
            return;
        }

        try {
            // Serialize with Jackson 3 to string
            StringWriter sw = new StringWriter();
            objectMapper.writeValue(sw, value);

            // Parse and transfer to JsonGenerator
            JsonParser parser = jsonProvider.createParser(new StringReader(sw.toString()));
            transferAll(parser, generator);
            parser.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize", e);
        }
    }

    private void transferAll(JsonParser parser, JsonGenerator generator) {
        JsonParser.Event event = parser.next();
        transferValue(parser, generator, event);
    }

    private void transferValue(JsonParser parser, JsonGenerator generator, JsonParser.Event event) {
        switch (event) {
            case START_OBJECT:
                generator.writeStartObject();
                while (parser.hasNext()) {
                    event = parser.next();
                    if (event == JsonParser.Event.END_OBJECT) {
                        generator.writeEnd();
                        break;
                    }
                    if (event == JsonParser.Event.KEY_NAME) {
                        generator.writeKey(parser.getString());
                        event = parser.next();
                        transferValue(parser, generator, event);
                    }
                }
                break;
            case START_ARRAY:
                generator.writeStartArray();
                while (parser.hasNext()) {
                    event = parser.next();
                    if (event == JsonParser.Event.END_ARRAY) {
                        generator.writeEnd();
                        break;
                    }
                    transferValue(parser, generator, event);
                }
                break;
            case VALUE_STRING:
                generator.write(parser.getString());
                break;
            case VALUE_NUMBER:
                if (parser.isIntegralNumber()) {
                    generator.write(parser.getLong());
                } else {
                    generator.write(parser.getBigDecimal());
                }
                break;
            case VALUE_TRUE:
                generator.write(true);
                break;
            case VALUE_FALSE:
                generator.write(false);
                break;
            case VALUE_NULL:
                generator.writeNull();
                break;
            case KEY_NAME:
                // Should not happen at this level, but handle it
                generator.writeKey(parser.getString());
                break;
            default:
                // Skip unknown events
                break;
        }
    }

    @Override
    protected <T> JsonpDeserializer<T> getDefaultDeserializer(Type type) {
        return new JsonpDeserializer<T>() {
            @Override
            public T deserialize(JsonParser parser, JsonpMapper mapper, jakarta.json.stream.JsonParser.Event event) {
                try {
                    // Use the existing deserialize method
                    return JacksonJsonpMapper.this.deserialize(parser, type);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize type: " + type, e);
                }
            }

            @Override
            public EnumSet<JsonParser.Event> acceptedEvents() {
                return EnumSet.allOf(JsonParser.Event.class);
            }

            @Override
            public EnumSet<JsonParser.Event> nativeEvents() {
                return EnumSet.allOf(JsonParser.Event.class);
            }
        };
    }

    @Override
    public <T> JsonpMapper withAttribute(String name, T value) {
        Map<String, Object> next = new HashMap<>(attributes);
        next.put(name, value);
        return new JacksonJsonpMapper(objectMapper, next);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T attribute(String name) {
        return (T) attributes.get(name);
    }

    @Override
    public boolean ignoreUnknownFields() {
        return !objectMapper.isEnabled(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public ObjectMapper objectMapper() {
        return this.objectMapper;
    }
}
