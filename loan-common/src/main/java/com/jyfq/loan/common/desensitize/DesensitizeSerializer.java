package com.jyfq.loan.common.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 脱敏序列化器（Jackson 序列化时自动执行打码逻辑）
 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType type;

    public DesensitizeSerializer() {
    }

    public DesensitizeSerializer(DesensitizeType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (StringUtils.isBlank(value)) {
            gen.writeString(value);
            return;
        }
        gen.writeString(desensitize(value, type));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }
        Desensitize annotation = property.getAnnotation(Desensitize.class);
        if (annotation != null) {
            return new DesensitizeSerializer(annotation.type());
        }
        return prov.findValueSerializer(property.getType(), property);
    }

    private static String desensitize(String value, DesensitizeType type) {
        if (type == null) {
            return value;
        }
        return switch (type) {
            case PHONE -> {
                if (value.length() >= 11) {
                    yield value.substring(0, 3) + "****" + value.substring(7);
                }
                yield value;
            }
            case ID_CARD -> {
                if (value.length() >= 18) {
                    yield value.substring(0, 3) + "***********" + value.substring(14);
                }
                yield value;
            }
            case NAME -> {
                if (value.length() == 2) {
                    yield value.charAt(0) + "*";
                } else if (value.length() > 2) {
                    yield value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
                }
                yield value;
            }
            case BANK_CARD -> {
                if (value.length() >= 8) {
                    yield value.substring(0, 4) + " **** **** " + value.substring(value.length() - 4);
                }
                yield value;
            }
            case EMAIL -> {
                int atIdx = value.indexOf('@');
                if (atIdx > 1) {
                    yield value.charAt(0) + "***" + value.substring(atIdx);
                }
                yield value;
            }
            case ADDRESS -> {
                if (value.length() > 6) {
                    yield value.substring(0, 6) + "****";
                }
                yield value;
            }
        };
    }
}
