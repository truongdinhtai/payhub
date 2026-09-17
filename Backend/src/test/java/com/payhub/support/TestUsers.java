package com.payhub.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.UUID;

import com.payhub.user.domain.Role;
import com.payhub.user.domain.User;

/**
 * Test-only factory for {@link User}. Production code intentionally hides the
 * no-arg constructor and the id setter (ids are database-generated), so tests
 * that need a fully-formed User with a known id build it here via reflection.
 */
public final class TestUsers {

    private TestUsers() {
    }

    public static User user(UUID id, String email, String name, Role role) {
        User user = newInstance();
        setField(user, "id", id);
        user.setGoogleSub("sub-" + id);
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        return user;
    }

    private static User newInstance() {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate User", e);
        }
    }

    private static void setField(Object target, String field, Object value) {
        try {
            Field f = User.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
