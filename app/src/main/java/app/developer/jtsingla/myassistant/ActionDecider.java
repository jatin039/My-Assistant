package app.developer.jtsingla.myassistant;

import android.content.Context;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by jssingla on 1/16/17.
 */

public class ActionDecider {
    private static String[] actionsKeywords = {
      "Call", "Reminder", "Message", "Camera"
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
            case "Call":
                messages.add(new Message(false, "Trying to make the call")); // test
                break;
            case "Reminder":
                messages.add(new Message(false, "Trying to set the reminder")); // test
                break;
            case "Message":
                messages.add(new Message(false, "Trying to send the message")); // test
                break;
            case "Camera":
                messages.add(new Message(false, "Trying to open the camera")); // test
                break;
            default:
                /* it will never come here as we only call this on keyword match */
        }
    }

    private static String generateRandomMessage() {
        int min = 0, max = randomMessagesForUnhandledCases.length;
        int randomIndex = min + (int)(Math.random() * (max + 1));
        return randomMessagesForUnhandledCases[randomIndex];
    }

    public static void performAction(Context context, String message) {
        /* ToDo: need to figure out the cases where both keywords might be there,
                  * May need to limit such scenarios or handle appropriately */
        for (int i = 0; i < actionsKeywords.length; i++) {
            if (message.contains(actionsKeywords[i])) {
                callActionClass(message, actionsKeywords[i],context);
                return;
            }
        }
        /* if none of the patterns were matched, generate random message and send */
        ArrayList<Message> messages = HomeActivity.messages;
        messages.add(new Message(false, generateRandomMessage()));
    }
}
