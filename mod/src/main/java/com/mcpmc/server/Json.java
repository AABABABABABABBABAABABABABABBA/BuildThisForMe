package com.mcpmc.server;

import java.util.*;

public final class Json {
    private Json() {}

    public static String stringify(Object v) {
        StringBuilder sb = new StringBuilder();
        write(sb, v);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object v) {
        if (v == null) { sb.append("null"); return; }
        if (v instanceof Boolean || v instanceof Number) { sb.append(v); return; }
        if (v instanceof String s) { writeStr(sb, s); return; }
        if (v instanceof Map<?,?> m) {
            sb.append('{');
            boolean first = true;
            for (var e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeStr(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        if (v instanceof Collection<?> c) {
            sb.append('[');
            boolean first = true;
            for (var e : c) {
                if (!first) sb.append(',');
                first = false;
                write(sb, e);
            }
            sb.append(']');
            return;
        }
        if (v.getClass().isArray()) {
            sb.append('[');
            int len = java.lang.reflect.Array.getLength(v);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                write(sb, java.lang.reflect.Array.get(v, i));
            }
            sb.append(']');
            return;
        }
        writeStr(sb, v.toString());
    }

    private static void writeStr(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    public static Object parse(String src) {
        Parser p = new Parser(src);
        Object v = p.readValue();
        p.skipWs();
        if (p.pos < src.length()) throw new IllegalArgumentException("trailing junk at " + p.pos);
        return v;
    }

    private static final class Parser {
        final String s; int pos;
        Parser(String s) { this.s = s; }
        void skipWs() { while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++; }
        char peek() { return s.charAt(pos); }
        Object readValue() {
            skipWs();
            if (pos >= s.length()) throw new IllegalArgumentException("unexpected end");
            char c = peek();
            if (c == '{') return readObj();
            if (c == '[') return readArr();
            if (c == '"') return readStr();
            if (c == '-' || (c >= '0' && c <= '9')) return readNum();
            if (s.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (s.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            if (s.startsWith("null", pos)) { pos += 4; return null; }
            throw new IllegalArgumentException("bad token at " + pos);
        }
        Map<String,Object> readObj() {
            pos++; skipWs();
            Map<String,Object> m = new LinkedHashMap<>();
            if (peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                String k = readStr();
                skipWs();
                if (peek() != ':') throw new IllegalArgumentException("expected : at " + pos);
                pos++;
                m.put(k, readValue());
                skipWs();
                char d = peek();
                if (d == ',') { pos++; continue; }
                if (d == '}') { pos++; return m; }
                throw new IllegalArgumentException("expected , or } at " + pos);
            }
        }
        List<Object> readArr() {
            pos++; skipWs();
            List<Object> l = new ArrayList<>();
            if (peek() == ']') { pos++; return l; }
            while (true) {
                l.add(readValue());
                skipWs();
                char d = peek();
                if (d == ',') { pos++; continue; }
                if (d == ']') { pos++; return l; }
                throw new IllegalArgumentException("expected , or ] at " + pos);
            }
        }
        String readStr() {
            if (peek() != '"') throw new IllegalArgumentException("expected string at " + pos);
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char n = s.charAt(pos++);
                    switch (n) {
                        case '"', '\\', '/' -> sb.append(n);
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> { sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16)); pos += 4; }
                        default -> throw new IllegalArgumentException("bad escape at " + pos);
                    }
                } else sb.append(c);
            }
            throw new IllegalArgumentException("unterminated string");
        }
        Number readNum() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < s.length() && "0123456789.eE+-".indexOf(s.charAt(pos)) >= 0) pos++;
            String t = s.substring(start, pos);
            if (t.contains(".") || t.contains("e") || t.contains("E")) return Double.parseDouble(t);
            try { return Long.parseLong(t); } catch (NumberFormatException e) { return Double.parseDouble(t); }
        }
    }
}
