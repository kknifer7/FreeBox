package io.knifer.freebox.util.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.knifer.freebox.constant.ValueEnum;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ValueEnumTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();

        if (!rawType.isEnum() || !ValueEnum.class.isAssignableFrom(rawType)) {

            return null;
        }

        Map<Object, T> valueToEnum = Arrays.stream(rawType.getEnumConstants())
                .collect(Collectors.toMap(
                        enumConst -> ((ValueEnum) enumConst).getValue(),
                        enumConst -> enumConst
                ));
        Map<T, Object> enumToValue = Arrays.stream(rawType.getEnumConstants())
                .collect(Collectors.toMap(
                        enumConst -> enumConst,
                        enumConst -> ((ValueEnum) enumConst).getValue()
                ));

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();

                    return;
                }

                Object val = enumToValue.get(value);
                if (val == null) {
                    out.nullValue();

                    return;
                }

                if (val instanceof Number) {
                    out.value((Number) val);
                } else if (val instanceof String) {
                    out.value((String) val);
                } else if (val instanceof Boolean) {
                    out.value((Boolean) val);
                } else {
                    out.value(val.toString());
                }
            }

            @Override
            public T read(JsonReader in) throws IOException {
                Object value;
                T result;

                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();

                    return null;
                }
                value = readValue(in);
                if (value == null) {

                    return null;
                }
                result = valueToEnum.get(value);
                if (result == null) {
                    log.warn("Unknown value '{}' for enum {}", value, rawType.getName());
                }

                return result;
            }

            @Nullable
            private Object readValue(JsonReader in) throws IOException {
                JsonToken token = in.peek();
                switch (token) {
                    case NUMBER:
                        String numberStr = in.nextString();
                        try {
                            return Integer.parseInt(numberStr);
                        } catch (NumberFormatException e1) {
                            try {
                                return Long.parseLong(numberStr);
                            } catch (NumberFormatException e2) {
                                try {
                                    return Double.parseDouble(numberStr);
                                } catch (NumberFormatException e3) {
                                    return numberStr; // 返回原始字符串
                                }
                            }
                        }
                    case STRING:
                        return in.nextString();
                    case BOOLEAN:
                        return in.nextBoolean();
                    default:
                        log.warn("Invalid token {} for enum {}", token, rawType.getName());
                        return null;
                }
            }
        };
    }
}