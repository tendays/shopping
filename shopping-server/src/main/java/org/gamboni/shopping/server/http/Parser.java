package org.gamboni.shopping.server.http;

/**
 * @author tendays
 */
interface Parser<T> {
    T parse(String value) throws Exception;
}
