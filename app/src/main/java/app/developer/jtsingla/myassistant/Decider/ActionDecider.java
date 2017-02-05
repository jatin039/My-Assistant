package app.developer.jtsingla.myassistant.Decider;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

import app.developer.jtsingla.myassistant.Actions.Call;
import app.developer.jtsingla.myassistant.Actions.Text;
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
                Text.attemptSendText(context, message);
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
        String match = parseMessageForStringArray(message.toLowerCase(), actionsKeywords);
        if (match == null) {
            /* if none of the patterns matched, generate random message and send to user*/
            ArrayList<Message> messages = HomeActivity.messages;
            messages.add(new Message(false, generateRandomMessage(randomMessagesForUnhandledCases)));
            return;
        }
        callActionClass(message, match, context);
    }

    public static String parseMessageForStringArray(String message, String[] keywords) {

        String match = null;
        Integer minIndexMatch = Integer.MAX_VALUE;

        /* get the earliest occuring keyword in message, need this to get
           the appropriate action in case of multiple keywords
         */
        for (String keyword : keywords) {
            Integer indexMatch = getKeywordIndex(message, keyword);
            if (indexMatch < minIndexMatch) {
                minIndexMatch = indexMatch;
                match = keyword;
            }
        }
        return match;
    }

    private static Integer getKeywordIndex(String message, String keyword) {
        if (message.contains(" " + keyword + " ")) { /* exact match */
            return message.indexOf(" " + keyword + " ");
        }
            /* search for "keyword " if only its a first word, avoiding matches like "*keyword " */
        if (message.contains(keyword + " ") &&
                message.startsWith(keyword)) {
            return message.indexOf(keyword + " ");
        }

            /* search for " keyword" if only its a last word, avoiding matches like " keyword*" */
        if (message.contains(" " + keyword) &&
                message.endsWith(keyword)) {
            return message.indexOf(" " + keyword);
        }

            /* check if that exact keyword is the only word in message */
        if (message.equals(keyword)) {
            return message.indexOf(keyword); // i.e 0 in this case always
        }

            /* allow dots "." or "s" after keyword, as people generally tend to give "keyword..." as message */
            /* TODO */
        /* return MAX value if value we are looking for is not in message */
        return Integer.MAX_VALUE;
    }
}
