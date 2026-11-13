package com.payroll.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Lightweight, zero-dependency JSON parser and serializer.
 * Supports Maps, Lists, Collections, Numbers, Dates, Enums, and JavaBean models.
 */
public class JsonUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // =========================================================================
    // SERIALIZER (Java Object -> JSON String)
    // =========================================================================

    public static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        serialize(obj, sb);
        return sb.toString();
    }

    private static void serialize(Object obj, StringBuilder sb) {
        if (obj == null) {
            sb.append("null");
            return;
        }

        if (obj instanceof String) {
            sb.append('"').append(escapeString((String) obj)).append('"');
            return;
        }

        if (obj instanceof Character) {
            sb.append('"').append(escapeString(String.valueOf(obj))).append('"');
            return;
        }

        if (obj instanceof Number) {
            if (obj instanceof BigDecimal) {
                sb.append(((BigDecimal) obj).toPlainString());
            } else {
                sb.append(obj.toString());
            }
            return;
        }

        if (obj instanceof Boolean) {
            sb.append(obj.toString());
            return;
        }

        if (obj instanceof Enum<?>) {
            sb.append('"').append(((Enum<?>) obj).name()).append('"');
            return;
        }

        if (obj instanceof LocalDate) {
            sb.append('"').append(((LocalDate) obj).format(DATE_FORMATTER)).append('"');
            return;
        }

        if (obj instanceof LocalTime) {
            sb.append('"').append(((LocalTime) obj).format(TIME_FORMATTER)).append('"');
            return;
        }

        if (obj instanceof LocalDateTime) {
            sb.append('"').append(((LocalDateTime) obj).format(DATE_TIME_FORMATTER)).append('"');
            return;
        }

        if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escapeString(String.valueOf(entry.getKey()))).append("\":");
                serialize(entry.getValue(), sb);
            }
            sb.append('}');
            return;
        }

        if (obj instanceof Collection<?>) {
            Collection<?> col = (Collection<?>) obj;
            sb.append('[');
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(',');
                first = false;
                serialize(item, sb);
            }
            sb.append(']');
            return;
        }

        if (obj.getClass().isArray()) {
            sb.append('[');
            int len = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                serialize(java.lang.reflect.Array.get(obj, i), sb);
            }
            sb.append(']');
            return;
        }

        // POJO JavaBean reflection
        serializeBean(obj, sb);
    }

    private static void serializeBean(Object bean, StringBuilder sb) {
        sb.append('{');
        boolean first = true;
        Method[] methods = bean.getClass().getMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));

        for (Method method : methods) {
            if (method.getParameterCount() != 0 || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String name = method.getName();
            String propName = null;
            if (name.startsWith("get") && name.length() > 3 && !name.equals("getClass")) {
                propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            } else if (name.startsWith("is") && name.length() > 2) {
                propName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }

            if (propName != null) {
                try {
                    Object val = method.invoke(bean);
                    if (!first) sb.append(',');
                    first = false;
                    sb.append('"').append(propName).append("\":");
                    serialize(val, sb);
                } catch (Exception ignored) {}
            }
        }
        sb.append('}');
    }

    public static String escapeString(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(hex.substring(hex.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // PARSER (JSON String -> Map / List / Object)
    // =========================================================================

    public static Object parse(String json) {
        if (json == null) return null;
        json = json.trim();
        if (json.isEmpty()) return null;
        return new JsonParser(json).parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object res = parse(json);
        if (res instanceof Map) {
            return (Map<String, Object>) res;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        Object res = parse(json);
        if (res instanceof List) {
            return (List<Object>) res;
        }
        return new ArrayList<>();
    }

    // Helper map accessors
    public static String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val).trim() : defaultVal;
    }

    public static int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString().trim()); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    public static BigDecimal getBigDecimal(Map<String, Object> map, String key, BigDecimal defaultVal) {
        Object val = map.get(key);
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        if (val != null) {
            try { return new BigDecimal(val.toString().trim()); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultVal) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) {
            return Boolean.parseBoolean(val.toString().trim());
        }
        return defaultVal;
    }

    public static LocalDate getLocalDate(Map<String, Object> map, String key) {
        String s = getString(map, key, null);
        if (s == null || s.isEmpty()) return null;
        try {
            return LocalDate.parse(s, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalTime getLocalTime(Map<String, Object> map, String key) {
        String s = getString(map, key, null);
        if (s == null || s.isEmpty()) return null;
        try {
            if (s.length() == 5) s += ":00"; // handle HH:mm
            return LocalTime.parse(s, TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private static class JsonParser {
        private final String src;
        private int pos = 0;

        public JsonParser(String src) {
            this.src = src;
        }

        public Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            throw new IllegalArgumentException("Unexpected char at position " + pos + ": " + c);
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            consume('{');
            skipWhitespace();
            if (peek() == '}') {
                consume('}');
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                consume(':');
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                char c = peek();
                if (c == '}') {
                    consume('}');
                    break;
                }
                consume(',');
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            consume('[');
            skipWhitespace();
            if (peek() == ']') {
                consume(']');
                return list;
            }
            while (true) {
                Object val = parseValue();
                list.add(val);
                skipWhitespace();
                char c = peek();
                if (c == ']') {
                    consume(']');
                    break;
                }
                consume(',');
            }
            return list;
        }

        private String parseString() {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= src.length()) break;
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 <= src.length()) {
                                String hex = src.substring(pos, pos + 4);
                                pos += 4;
                                sb.append((char) Integer.parseInt(hex, 16));
                            }
                            break;
                        default: sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            boolean isDecimal = false;
            if (pos < src.length() && src.charAt(pos) == '.') {
                isDecimal = true;
                pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                isDecimal = true;
                pos++;
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            String numStr = src.substring(start, pos);
            if (isDecimal) {
                return Double.parseDouble(numStr);
            }
            long l = Long.parseLong(numStr);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            }
            return l;
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Expected boolean at " + pos);
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Expected null at " + pos);
        }

        private void skipWhitespace() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private char peek() {
            if (pos >= src.length()) return '\0';
            return src.charAt(pos);
        }

        private void consume(char expected) {
            if (pos >= src.length() || src.charAt(pos) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + pos + " but got '" + peek() + "'");
            }
            pos++;
        }
    }
}
