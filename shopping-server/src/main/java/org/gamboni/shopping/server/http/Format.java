package org.gamboni.shopping.server.http;

/**
 * @author tendays
 */
public interface Format<T> extends Formatter<T>, Parser<T> {

    public static Format<Long> LONG = Format.of(Object::toString, Long::parseLong);
    public static Format<String> STRING = Format.of(s -> s, s -> s);

    public static <E extends Enum<E>> Format<E> ofEnum(Class<E> enumClass) {
        return Format.of(Enum::name, string -> Enum.valueOf(enumClass, string));
    }

    static <T> Format<T> of(Formatter<T> formatter, Parser<T> parser) {
        return new Format<T>() {
            @Override
            public T parse(String value) throws Exception {
                return parser.parse(value);
            }

            @Override
            public String format(T value) {
                return formatter.format(value);
            }
        };
    }
}
