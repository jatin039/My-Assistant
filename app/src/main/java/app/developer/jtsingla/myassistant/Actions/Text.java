package app.developer.jtsingla.myassistant.Actions;

import android.content.Context;

import app.developer.jtsingla.myassistant.Utils.CommonAPIs;

import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.isNegate;

/**
 * Created by jssingla on 1/23/17.
 */

public class Text {
    private static String[] textKeywords = {
        "message text to", // sounds stupid, but still...
        "text message to",
        "make a text message to",
        "make a text to",
        "make a message to",
        "make a message text to", //sounds stupid but people may say.
        "make a whatsapp message to",
        "make a whatsapp text to",
        "make a whatsapp to",
        "make text to",
        "make message to",
        "make text message to",
        "make message text to", // sounds stupid but still.
        "make whatsapp message to",
        "make whatsapp text to",
        "make whatsapp to",
        "whatsapp to",
        "message to",
        "text to",
        "message",
        "text",
        "whatsapp"
    };


    public static void attemptSendText(Context context, String message) {
        if (isNegate(message)) {
            return; // if user tells not to make a text. when someone is trying to test the app. :D
        }
    }
}
