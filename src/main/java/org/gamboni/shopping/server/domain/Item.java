package org.gamboni.shopping.server.domain;

import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** An item in a shopping list.
 *
 * @author tendays
 */
@Entity
public class Item {
    private String text;
    private long sequence;
    private ProductPicture image;
    private State state = State.UNUSED;

    public Item() {} // for Hibernate
    public Item(String text) {this.text = text;}

    public Item(ProductPicture image) {
        this(image.getText());
        this.image = image;
    }

    @Id
    @Column(length = 191)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Enumerated(EnumType.STRING)
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /** Optional image. */
    @ManyToOne
    public ProductPicture getImage() {
        return image;
    }

    public void setImage(ProductPicture image) {
        this.image = image;
    }

    public Optional<ProductPicture> image() {
        return Optional.ofNullable(image);
    }

    public String toString() {
        return state +" "+ text;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }
}
