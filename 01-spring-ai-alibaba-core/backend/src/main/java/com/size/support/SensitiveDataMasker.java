package com.size.support;

import java.util.regex.Pattern;

/**
 * 写日志前的基础脱敏；生产项目还应在结构化日志字段层进行白名单控制。
 */
public final class SensitiveDataMasker {

    private static final Pattern BEARER = Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+");
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\\\"(?:password|api[-_]?key|token)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)(1\\d{2})\\d{4}(\\d{4})(?!\\d)");

    private SensitiveDataMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String masked = BEARER.matcher(text).replaceAll("$1***");
        masked = JSON_SECRET.matcher(masked).replaceAll("$1***$2");
        return MOBILE.matcher(masked).replaceAll("$1****$2");
    }
}
