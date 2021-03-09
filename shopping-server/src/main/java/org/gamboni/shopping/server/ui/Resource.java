package org.gamboni.shopping.server.ui;

/**
 * @author tendays
 */
public interface Resource {
    String render();
    String getUrl();
    Html asElement();
}
