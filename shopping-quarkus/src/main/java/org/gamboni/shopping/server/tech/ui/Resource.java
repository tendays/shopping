package org.gamboni.shopping.server.tech.ui;

/**
 * @author tendays
 */
public interface Resource {
    String render();
    String getUrl();
    Html asElement();
}
