package app.developer.jtsingla.myassistant;

/**
 * Created by jssingla on 1/15/17.
 */

public class Message {
    private boolean isRight;
    private String messageText;

    Message(boolean isRight, String messageText) {
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
