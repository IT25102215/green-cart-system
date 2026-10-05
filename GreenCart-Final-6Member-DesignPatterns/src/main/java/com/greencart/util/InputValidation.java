package com.greencart.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class InputValidation {

    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private InputValidation() {}

    public static String requireEmail(String value) {
        String email = requireText(value, "Email", 5, 254).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        return email;
    }

    public static String requireStrongPassword(String value) {
        if (value == null || !STRONG_PASSWORD.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and include uppercase, lowercase, number and special character"
            );
        }
        return value;
    }

    public static String requireText(String value, String field, int min, int max) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        String clean = value.trim();
        if (clean.length() < min) {
            throw new IllegalArgumentException(field + " must contain at least " + min + " character(s)");
        }
        if (clean.length() > max) {
            throw new IllegalArgumentException(field + " cannot exceed " + max + " characters");
        }
        return clean;
    }
}
