package net.trellisframework.mcp.scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import net.trellisframework.mcp.annotation.McpResource;
import net.trellisframework.mcp.annotation.McpResourceEndpoint;
import net.trellisframework.mcp.annotation.McpTool;
import net.trellisframework.mcp.annotation.McpToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ReflectionUtils;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Configuration
public class McpComponentScanner {

    private static final Logger log = LoggerFactory.getLogger(McpComponentScanner.class);

    @Bean
    public List<McpServerFeatures.SyncToolSpecification> mcpToolSpecifications(ApplicationContext context) {
        List<OrderedItem<McpServerFeatures.SyncToolSpecification>> items = new ArrayList<>();
        for (String beanName : context.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = context.getBean(beanName);
            } catch (Exception e) {
                continue;
            }
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getDeclaredMethods()) {
                McpTool annotation = method.getAnnotation(McpTool.class);
                if (annotation != null) {
                    items.add(new OrderedItem<>(annotation.index(), createToolSpec(bean, method, annotation)));
                    log.info("Registered MCP tool: {}", annotation.name());
                }
            }
        }
        items.sort(Comparator.comparingInt(OrderedItem::order));
        return items.stream().map(OrderedItem::item).toList();
    }

    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> mcpResourceSpecifications(ApplicationContext context) {
        List<McpServerFeatures.SyncResourceSpecification> items = new ArrayList<>();

        // 1. Scan component beans for @McpResource methods
        for (String beanName : context.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = context.getBean(beanName);
            } catch (Exception e) {
                continue;
            }
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getDeclaredMethods()) {
                McpResource annotation = method.getAnnotation(McpResource.class);
                if (annotation != null) {
                    items.add(createResourceSpec(bean, method, annotation));
                    log.info("Registered MCP resource: {} ({})", annotation.name(), annotation.uri());
                }
            }
        }

        // 2. Scan @McpResourceEndpoint interfaces
        scanResourceEndpointInterfaces(context, items);

        return items;
    }

    private void scanResourceEndpointInterfaces(ApplicationContext context, List<McpServerFeatures.SyncResourceSpecification> items) {
        List<String> basePackages;
        try {
            basePackages = AutoConfigurationPackages.get(context);
        } catch (IllegalStateException e) {
            log.debug("AutoConfigurationPackages not available, skipping @McpResourceEndpoint scanning");
            return;
        }

        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface();
            }
        };
        provider.addIncludeFilter(new AnnotationTypeFilter(McpResourceEndpoint.class));

        for (String basePackage : basePackages) {
            for (BeanDefinition bd : provider.findCandidateComponents(basePackage)) {
                try {
                    Class<?> iface = Class.forName(bd.getBeanClassName());
                    for (Method method : iface.getDeclaredMethods()) {
                        McpResource annotation = method.getAnnotation(McpResource.class);
                        if (annotation != null) {
                            items.add(createResourceSpecFromAnnotation(annotation));
                            log.info("Registered MCP resource: {} ({})", annotation.name(), annotation.uri());
                        }
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Could not load @McpResourceEndpoint interface: {}", bd.getBeanClassName(), e);
                }
            }
        }
    }

    private McpServerFeatures.SyncResourceSpecification createResourceSpecFromAnnotation(McpResource annotation) {
        McpSchema.Resource resource = new McpSchema.Resource(
                annotation.uri(),
                annotation.name(),
                annotation.description(),
                annotation.mimeType(),
                null
        );

        boolean hasValue = !annotation.value().isEmpty();
        boolean hasFile = !annotation.file().isEmpty();

        return new McpServerFeatures.SyncResourceSpecification(
                resource,
                (McpSyncServerExchange exchange, McpSchema.ReadResourceRequest request) -> {
                    try {
                        log.info("resources/read requested URI='{}' → handler URI='{}'", request.uri(), annotation.uri());
                        String text;
                        if (hasValue) {
                            text = annotation.value();
                        } else if (hasFile) {
                            text = readClasspathFile(annotation.file(), annotation.mimeType());
                        } else {
                            throw new RuntimeException("@McpResourceEndpoint method must have 'file' or 'value' set: " + annotation.uri());
                        }
                        McpSchema.ResourceContents contents = new McpSchema.TextResourceContents(
                                annotation.uri(),
                                annotation.mimeType(),
                                text
                        );
                        return new McpSchema.ReadResourceResult(List.of(contents));
                    } catch (Exception e) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        throw new RuntimeException("Failed to read resource " + annotation.uri() + ": " + cause.getMessage(), cause);
                    }
                }
        );
    }

    private McpServerFeatures.SyncToolSpecification createToolSpec(Object bean, Method method, McpTool annotation) {
        boolean hasSchemaFile = !annotation.inputSchemaFile().isEmpty();
        McpSchema.JsonSchema inputSchema = hasSchemaFile
                ? buildInputSchemaFromFile(annotation.inputSchemaFile())
                : buildInputSchema(method);

        McpSchema.ToolAnnotations toolAnnotations = new McpSchema.ToolAnnotations(
                null,
                annotation.readOnly() ? true : null,
                annotation.destructive() ? true : null,
                annotation.idempotent() ? true : null,
                null,
                null
        );

        String description = !annotation.descriptionFile().isEmpty()
                ? readClasspathFile(annotation.descriptionFile(), "text/plain")
                : annotation.description();

        try {
            log.info("Tool '{}' inputSchema: {}", annotation.name(), new ObjectMapper().writeValueAsString(inputSchema));
        } catch (Exception ignored) {}

        McpSchema.Tool tool = new McpSchema.Tool(
                annotation.name(),
                null,
                description,
                inputSchema,
                null,
                toolAnnotations,
                null
        );

        return new McpServerFeatures.SyncToolSpecification(
                tool,
                (McpSyncServerExchange exchange, Map<String, Object> arguments) -> {
                    try {
                        Object[] args;
                        if (hasSchemaFile && method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                            // Schema file defines the structure; serialize entire arguments map to JSON string
                            String jsonArgs = new ObjectMapper().writeValueAsString(arguments);
                            args = new Object[]{jsonArgs};
                        } else {
                            args = resolveArguments(method, arguments);
                        }
                        ReflectionUtils.makeAccessible(method);
                        Object result = method.invoke(bean, args);
                        String text = result != null ? result.toString() : "";
                        return new McpSchema.CallToolResult(text, false);
                    } catch (Exception e) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        return new McpSchema.CallToolResult(cause.getMessage(), true);
                    }
                }
        );
    }

    private McpServerFeatures.SyncResourceSpecification createResourceSpec(Object bean, Method method, McpResource annotation) {
        McpSchema.Resource resource = new McpSchema.Resource(
                annotation.uri(),
                annotation.name(),
                annotation.description(),
                annotation.mimeType(),
                null
        );

        boolean hasValue = !annotation.value().isEmpty();
        boolean hasFile = !annotation.file().isEmpty();

        return new McpServerFeatures.SyncResourceSpecification(
                resource,
                (McpSyncServerExchange exchange, McpSchema.ReadResourceRequest request) -> {
                    try {
                        log.info("resources/read requested URI='{}' → handler URI='{}'", request.uri(), annotation.uri());
                        String text;
                        if (hasValue) {
                            text = annotation.value();
                        } else if (hasFile) {
                            text = readClasspathFile(annotation.file(), annotation.mimeType());
                        } else {
                            ReflectionUtils.makeAccessible(method);
                            Object result = method.invoke(bean);
                            text = result != null ? result.toString() : "";
                        }
                        McpSchema.ResourceContents contents = new McpSchema.TextResourceContents(
                                annotation.uri(),
                                annotation.mimeType(),
                                text
                        );
                        return new McpSchema.ReadResourceResult(List.of(contents));
                    } catch (Exception e) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        throw new RuntimeException("Failed to read resource " + annotation.uri() + ": " + cause.getMessage(), cause);
                    }
                }
        );
    }

    private String readClasspathFile(String fileName, String mimeType) {
        try (InputStream is = new ClassPathResource(fileName).getInputStream()) {
            String raw = new String(is.readAllBytes());
            if (mimeType.contains("json")) {
                // Fix trailing commas before ] or } (common in hand-edited JSON files)
                String sanitized = raw.replaceAll(",\\s*]", "]").replaceAll(",\\s*}", "}");
                ObjectMapper mapper = new ObjectMapper();
                Object parsed = mapper.readValue(sanitized, Object.class);
                return mapper.writeValueAsString(parsed);
            }
            return raw;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read classpath file: " + fileName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private McpSchema.JsonSchema buildInputSchemaFromFile(String fileName) {
        try (InputStream is = new ClassPathResource(fileName).getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> schema = mapper.readValue(is, new TypeReference<>() {});
            String type = (String) schema.getOrDefault("type", "object");
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            List<String> required = (List<String>) schema.get("required");
            Boolean additionalProperties = (Boolean) schema.get("additionalProperties");
            Map<String, Object> defs = (Map<String, Object>) schema.get("$defs");
            if (defs == null) defs = (Map<String, Object>) schema.get("defs");
            Map<String, Object> definitions = (Map<String, Object>) schema.get("definitions");
            return new McpSchema.JsonSchema(type, properties, required, additionalProperties, defs, definitions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input schema file: " + fileName, e);
        }
    }

    private McpSchema.JsonSchema buildInputSchema(Method method) {
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (Parameter param : parameters) {
            McpToolParam toolParam = param.getAnnotation(McpToolParam.class);
            String paramName = param.getName();
            Map<String, Object> prop = new HashMap<>();
            prop.put("type", mapJavaTypeToJsonType(param.getType()));
            if (toolParam != null) {
                prop.put("description", toolParam.description());
                if (toolParam.required()) {
                    required.add(paramName);
                }
            }
            properties.put(paramName, prop);
        }
        return new McpSchema.JsonSchema("object", properties, required, false, null, null);
    }

    private Object[] resolveArguments(Method method, Map<String, Object> arguments) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            Object value = arguments.get(paramName);
            args[i] = convertValue(value, parameters[i].getType());
        }
        return args;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;
        String str = value.toString();
        if (targetType == String.class) return str;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(str);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(str);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(str);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(str);
        return str;
    }

    private String mapJavaTypeToJsonType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class) return "integer";
        if (type == long.class || type == Long.class) return "integer";
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        return "string";
    }

    private record OrderedItem<T>(int order, T item) {}
}
