package org.gamboni.shopping.server.ui;

/**
 * @author tendays
 */
public class Tag implements Html {
    private final String nameAndAttributes;

    public Tag(String nameAndAttributes) {
        this.nameAndAttributes = nameAndAttributes;
    }

    public String toString() {
        return "<"+ nameAndAttributes +">";
    }
}
