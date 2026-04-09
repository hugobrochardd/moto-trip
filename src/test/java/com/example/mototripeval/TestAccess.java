package com.example.mototripeval;

import org.springframework.test.util.ReflectionTestUtils;

public final class TestAccess {

    private TestAccess() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T field(Object target, String name) {
        return (T) ReflectionTestUtils.getField(target, name);
    }

    public static Integer intField(Object target, String name) {
        return field(target, name);
    }

    public static Boolean boolField(Object target, String name) {
        return field(target, name);
    }

    public static Long longField(Object target, String name) {
        return field(target, name);
    }

    public static String stringField(Object target, String name) {
        return field(target, name);
    }
}
