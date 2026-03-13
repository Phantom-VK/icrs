package com.college.icrs.logging;

public final class IcrsLog {

    private static final int MAX_VALUE_LENGTH = 160;

    private IcrsLog() {
    }

    public static String event(String event, Object... keyValues) {
        StringBuilder builder = new StringBuilder("event=").append(sanitize(event));

        if (keyValues == null || keyValues.length == 0) {
            return builder.toString();
        }

        for (int i = 0; i < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = i + 1 < keyValues.length ? keyValues[i + 1] : "<missing>";
            builder.append(' ')
                    .append(sanitize(key))
                    .append('=')
                    .append(sanitize(value));
        }

        return builder.toString();
    }

    private static String sanitize(Object value) {
        if (value == null) {
            return "null";
        }

        String text = String.valueOf(value)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim()
                .replaceAll("\\s+", " ");

        if (text.isEmpty()) {
            return "\"\"";
        }

        if (text.length() > MAX_VALUE_LENGTH) {
            return text.substring(0, MAX_VALUE_LENGTH - 3) + "...";
        }

        return text;
    }
}
