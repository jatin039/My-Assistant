package app.developer.jtsingla.myassistant.Utils;

/**
 * Created by jssingla on 1/15/17.
 */

public class Message {
    private boolean isRight;
    private String messageText;

    public Message(boolean isRight, String messageText) {
        this.isRight = isRight;
        this.messageText = messageText;
    }

    public String getMessageText() {
        return this.messageText;
    }

    public boolean isRight() {
        return this.isRight;
    }
}
