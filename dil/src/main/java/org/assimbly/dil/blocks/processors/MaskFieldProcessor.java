package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Masks selected JSON fields with a replacement value (or type-appropriate empty/zero value when replacement is null).
 * Supports top-level fields, nested fields by name, dotted field paths (e.g. {@code user.password}), and arrays of objects.
 */
public class MaskFieldProcessor implements Processor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public void process(Exchange exchange) {
        String fieldsProperty = exchange.getProperty("fields", String.class);
        if (fieldsProperty == null || fieldsProperty.isBlank()) {
            return;
        }

        Set<String> targets = Arrays.stream(fieldsProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (targets.isEmpty()) {
            return;
        }

        String replacement = exchange.getProperty("replacement", String.class);
        Object body = exchange.getMessage().getBody();
        if (body == null) {
            return;
        }

        JsonNode root;
        BodyKind kind;
        if (body instanceof JsonNode node) {
            root = node.deepCopy();
            kind = BodyKind.JSON_NODE;
        } else if (body instanceof Map<?, ?> map) {
            root = MAPPER.valueToTree(map);
            kind = BodyKind.MAP;
        } else if (body instanceof byte[] bytes) {
            if (bytes.length == 0) {
                return;
            }
            root = MAPPER.readTree(bytes);
            kind = BodyKind.BYTES;
        } else {
            String text = exchange.getMessage().getBody(String.class);
            if (text == null || text.isBlank()) {
                return;
            }
            root = MAPPER.readTree(text);
            kind = BodyKind.STRING;
        }

        maskNode(root, "", targets, replacement);

        switch (kind) {
            case JSON_NODE -> exchange.getMessage().setBody(root);
            case MAP -> exchange.getMessage().setBody(MAPPER.convertValue(root, MAP_TYPE));
            case BYTES -> exchange.getMessage().setBody(MAPPER.writeValueAsString(root).getBytes(StandardCharsets.UTF_8));
            case STRING -> exchange.getMessage().setBody(MAPPER.writeValueAsString(root));
        }
    }

    private void maskNode(JsonNode node, String currentPath, Set<String> targets, String replacement) {
        if (node instanceof ObjectNode objectNode) {
            List<String> names = List.copyOf(objectNode.propertyNames());
            for (String fieldName : names) {
                String fullPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
                JsonNode child = objectNode.get(fieldName);
                if (targets.contains(fieldName) || targets.contains(fullPath)) {
                    objectNode.set(fieldName, maskValue(child, replacement));
                } else {
                    maskNode(child, fullPath, targets, replacement);
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            for (JsonNode element : arrayNode) {
                maskNode(element, currentPath, targets, replacement);
            }
        }
    }

    private JsonNode maskValue(JsonNode value, String replacement) {
        JsonNodeFactory factory = MAPPER.getNodeFactory();
        if (value == null || value.isNull()) {
            return factory.nullNode();
        }
        if (replacement == null) {
            return maskWithDefault(value, factory);
        }
        return maskWithReplacement(value, replacement, factory);
    }

    private JsonNode maskWithReplacement(JsonNode value, String replacement, JsonNodeFactory factory) {
        if (value.isInt()) {
            try {
                return factory.numberNode(Integer.parseInt(replacement));
            } catch (NumberFormatException _) {
                return factory.stringNode(replacement);
            }
        }
        if (value.isLong()) {
            try {
                return factory.numberNode(Long.parseLong(replacement));
            } catch (NumberFormatException _) {
                return factory.stringNode(replacement);
            }
        }
        if (value.isBigInteger()) {
            try {
                return factory.numberNode(new BigInteger(replacement));
            } catch (NumberFormatException _) {
                return factory.stringNode(replacement);
            }
        }
        if (value.isFloat() || value.isDouble()) {
            try {
                return factory.numberNode(Double.parseDouble(replacement));
            } catch (NumberFormatException _) {
                return factory.stringNode(replacement);
            }
        }
        if (value.isBigDecimal()) {
            try {
                return factory.numberNode(new BigDecimal(replacement));
            } catch (NumberFormatException _) {
                return factory.stringNode(replacement);
            }
        }
        if (value.isBoolean()) {
            if ("true".equalsIgnoreCase(replacement) || "false".equalsIgnoreCase(replacement)) {
                return factory.booleanNode(Boolean.parseBoolean(replacement));
            }
            return factory.stringNode(replacement);
        }
        return factory.stringNode(replacement);
    }

    private JsonNode maskWithDefault(JsonNode value, JsonNodeFactory factory) {
        if (value.isBoolean()) {
            return factory.booleanNode(false);
        }
        if (value.isInt()) {
            return factory.numberNode(0);
        }
        if (value.isLong()) {
            return factory.numberNode(0L);
        }
        if (value.isBigInteger()) {
            return factory.numberNode(BigInteger.ZERO);
        }
        if (value.isBigDecimal()) {
            return factory.numberNode(BigDecimal.ZERO);
        }
        if (value.isFloatingPointNumber()) {
            return factory.numberNode(0.0d);
        }
        if (value.isArray()) {
            return factory.arrayNode();
        }
        if (value.isObject()) {
            return factory.objectNode();
        }
        return factory.stringNode("");
    }

    private enum BodyKind {
        JSON_NODE,
        MAP,
        BYTES,
        STRING
    }
}
