package app.developer.jtsingla.myassistant.DTOs;

import android.util.Pair;

/**
 * Created by jssingla on 1/30/17.
 */

public class TextDTO {
    private String text;
    private Pair<String, String> contact;

    private String DummyText = "dummyText";

    public TextDTO() {
        this.text = DummyText;
        this.contact = null;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Pair<String, String> getContact() {
        return contact;
    }

    public void setContact(Pair<String, String> contact) {
        this.contact = contact;
    }

    public boolean isDummyText() {
        return this.text.equals(DummyText);
    }

    public boolean isDummyContact() {
        return this.contact.equals(null);
    }
}
