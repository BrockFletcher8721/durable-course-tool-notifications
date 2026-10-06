package dev.learning.notify;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(quote(String.valueOf(entry.getKey()))).append(':').append(write(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> values) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : values) {
                if (!first) out.append(',');
                first = false;
                out.append(write(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.space();
        if (!parser.done()) throw parser.bad("Trailing content");
        return value;
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('\"').toString();
    }

    private static final class Parser {
        private final String source;
        private int at;

        private Parser(String source) { this.source = source; }
        private boolean done() { return at == source.length(); }
        private void space() { while (!done() && Character.isWhitespace(source.charAt(at))) at++; }
        private IllegalArgumentException bad(String message) { return new IllegalArgumentException(message + " at " + at); }

        private Object value() {
            space();
            if (done()) throw bad("Expected value");
            return switch (source.charAt(at)) {
                case '{' -> object();
                case '[' -> array();
                case '\"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            at++;
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (!done() && source.charAt(at) == '}') { at++; return result; }
            while (true) {
                space();
                if (done() || source.charAt(at) != '\"') throw bad("Expected object key");
                String key = string();
                space();
                if (done() || source.charAt(at++) != ':') throw bad("Expected colon");
                result.put(key, value());
                space();
                if (!done() && source.charAt(at) == '}') { at++; return result; }
                if (done() || source.charAt(at++) != ',') throw bad("Expected comma");
            }
        }

        private List<Object> array() {
            at++;
            List<Object> result = new ArrayList<>();
            space();
            if (!done() && source.charAt(at) == ']') { at++; return result; }
            while (true) {
                result.add(value());
                space();
                if (!done() && source.charAt(at) == ']') { at++; return result; }
                if (done() || source.charAt(at++) != ',') throw bad("Expected comma");
            }
        }

        private String string() {
            at++;
            StringBuilder out = new StringBuilder();
            while (!done()) {
                char c = source.charAt(at++);
                if (c == '\"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                if (done()) throw bad("Incomplete escape");
                char escaped = source.charAt(at++);
                switch (escaped) {
                    case '\"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (at + 4 > source.length()) throw bad("Incomplete unicode escape");
                        out.append((char) Integer.parseInt(source.substring(at, at + 4), 16));
                        at += 4;
                    }
                    default -> throw bad("Unknown escape");
                }
            }
            throw bad("Unclosed string");
        }

        private Object number() {
            int start = at;
            while (!done() && "-+0123456789.eE".indexOf(source.charAt(at)) >= 0) at++;
            if (start == at) throw bad("Expected number");
            String token = source.substring(start, at);
            return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.parseDouble(token) : Long.parseLong(token);
        }

        private Object literal(String token, Object value) {
            if (!source.startsWith(token, at)) throw bad("Expected " + token);
            at += token.length();
            return value;
        }
    }
}
