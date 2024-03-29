package org.gamboni.shopping.server.tech.http;

/**
 * @author tendays
 */
interface Parser<T> {
    T parse(String value) throws Exception;
}
