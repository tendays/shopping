package org.gamboni.shopping.server.tech.http;

/**
 * @author tendays
 */
interface Formatter<T> {
    String format(T value);
}
