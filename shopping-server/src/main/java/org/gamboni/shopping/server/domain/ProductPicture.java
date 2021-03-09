package org.gamboni.shopping.server.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author tendays
 */
@Entity
public class ProductPicture {
    private String text;
    private String file;

    public ProductPicture() {
    }

    @Id
    @Column(length = 191)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
