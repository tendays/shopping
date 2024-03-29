package org.gamboni.shopping.server.tech;

import java.util.Optional;

public class Enums {
    public static <T extends Enum<T>> Optional<T> valueOf(Class<T> type, String name) {
        try {
            return Optional.of(Enum.valueOf(type, name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
