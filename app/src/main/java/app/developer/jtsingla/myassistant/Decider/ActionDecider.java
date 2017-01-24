package app.developer.jtsingla.myassistant.Decider;

import android.content.Context;

import java.util.ArrayList;

import app.developer.jtsingla.myassistant.Actions.Call;
import app.developer.jtsingla.myassistant.Activity.HomeActivity;
import app.developer.jtsingla.myassistant.Utils.Message;

/**
 * Created by jssingla on 1/16/17.
 */

public class ActionDecider {
    private static String[] actionsKeywords = {
      "call", "phone", "ring",  //synonyms to call
      "reminder",
      "message", "text", "whatsapp",
      "camera"
    };

    private static String[] randomMessagesForUnhandledCases = {
            "Sorry, I don't handle that as of now.",
            "Sorry, I didn't understand",
            "I wish I could help you with that",
            "I'm sorry, I can't do that",
            "Uh oh! Could you please rephrase?"
    };

    /* API to call specific class on basis of message,
       doesn;t return anything, any relevant info, i.e success, failure
       will be delivered to user from the action itself. */
    private static void callActionClass (String message, String match, Context context) {
        ArrayList<Message> messages = HomeActivity.messages;
        switch (match) {
            case "call":  //fallthrough
            case "phone": //fallthrough
            case "ring":
                Call.attemptPerformCall(context, message);
                break;
            case "reminder":
                messages.add(new Message(false, "Trying to set the reminder")); // test
                break;
            case "message": //fallthrough
            case "text": //fallthrough
            case "whatsapp":
                messages.add(new Message(false, "Trying to send the message")); // test
                break;
            case "camera":
                messages.add(new Message(false, "Trying to open the camera")); // test
                break;
            default:
                /* it will never come here as we only call this on keyword match */
        }
    }

    public static String generateRandomMessage(String[] stringArray) {
        int min = 0, max = stringArray.length;
        int randomIndex = min + (int)(Math.random() * max);
        return stringArray[randomIndex];
    }

    public static void performAction(Context context, String message) {
        /* ToDo: need to figure out the cases where both keywords might be there,
                  * May need to limit such scenarios or handle appropriately */
        String match = parseMessageForStringArray(message, actionsKeywords);
        if (match == null) {
            /* if none of the patterns matched, generate random message and send to user*/
            ArrayList<Message> messages = HomeActivity.messages;
            messages.add(new Message(false, generateRandomMessage(randomMessagesForUnhandledCases)));
            return;
        }
        callActionClass(message, match, context);
    }

    public static String parseMessageForStringArray(String message, String[] keywords) {
        for (int i = 0; i < keywords.length; i++) {
            if (message.toLowerCase().contains(" " + keywords[i] + " ")) { /* exact match */
                return keywords[i];
            }
            /* search for "keyword " if only its a first word, avoiding matches like "*keyword " */
            if (message.toLowerCase().contains(keywords[i] + " ") &&
                    message.toLowerCase().startsWith(keywords[i])) {
                return keywords[i];
            }

            /* search for " keyword" if only its a last word, avoiding matches like " keyword*" */
            if (message.toLowerCase().contains(" " + keywords[i]) &&
                    message.toLowerCase().endsWith(keywords[i])) {
                return keywords[i];
            }

            /* check if that exact keyword is the only word in message */
            if (message.toLowerCase().equals(keywords[i])) {
                return keywords[i];
            }

            /* allow dots "." or "s" after keyword, as people generally tend to give "keyword..." as message */
            /* TODO */
        }
        return null;
    }
}
