package org.gamboni.shopping.server.http;

/**
 * @author tendays
 */
interface Formatter<T> {
    String format(T value);
}
