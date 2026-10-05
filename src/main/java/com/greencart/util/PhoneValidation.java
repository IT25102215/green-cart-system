package com.greencart.util;

/** Central phone-number validation used by all forms that accept a phone number. */
public final class PhoneValidation {
    private PhoneValidation() {}

    /**
     * GreenCart stores Sri Lankan-style local phone numbers as exactly 10 digits.
     * No spaces, + sign, hyphens or letters are accepted.
     */
    public static String requireTenDigits(String phone) {
        String value = phone == null ? "" : phone.trim();
        if (!value.matches("\\d{10}")) {
            throw new IllegalArgumentException("Phone number must contain exactly 10 digits.");
        }
        return value;
    }
}
