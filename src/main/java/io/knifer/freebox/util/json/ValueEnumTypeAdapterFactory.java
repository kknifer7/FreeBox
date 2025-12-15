package io.knifer.freebox.util.json;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.knifer.freebox.constant.ValueEnum;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                Object value = readValue(in);
                T result = valueToEnum.get(value);

                if (result == null) {
                    throw new JsonSyntaxException(
                            String.format("Unknown value '%s' for enum %s", value, rawType.getName())
                    );
                }
                return result;
            }

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
                        throw new JsonSyntaxException("Unexpected token: " + token);
                }
            }
        };
    }
}