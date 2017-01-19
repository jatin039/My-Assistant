package app.developer.jtsingla.myassistant.Actions;

import java.util.ArrayList;

import app.developer.jtsingla.myassistant.Decider.ActionDecider;
import app.developer.jtsingla.myassistant.Activity.HomeActivity;
import app.developer.jtsingla.myassistant.Utils.Message;

/**
 * Created by jssingla on 1/16/17.
 */

public class Call {
    /*
    This class will handle all the requests for calling,
    It should understand what user exactly user want to do
     */
    private static String[] callKeywords = {
         "",
         ""
    };

    private static String[] negateCallKeywords = {
        "don't",
        "do not",
        "not",
        "dont"
    };

    private static String[] negateCallResponseMessages = {
        "Ok",
        "Hmm",
        "Not making the call...",
        "Got it."
    };

    private static String[] callResponseMessages = {
         "Ok",
         "Trying to make the call",
         "Will do that just now",
         "At your service, always"
    };

    /* tries to perform the call */
    public static void performCall(String message) {
        if (negateCall(message)) return; /* return if user is asking not to make a call */
        ArrayList<Message> messages = HomeActivity.messages;
        messages.add(new Message(false, ActionDecider.generateRandomMessage(callResponseMessages)));
        /* actually perform a call */
    }

    /* returns true if message says not to make a call */
    private static boolean negateCall(String message) {
        ArrayList<Message> messages = HomeActivity.messages;
        for (int i = 0; i < negateCallKeywords.length; i++) {
            if (message.toLowerCase().contains(negateCallKeywords[i])) {
                messages.add(new Message(false,
                        ActionDecider.generateRandomMessage(negateCallResponseMessages)));
                return true;
            }
        }
        return false;
    }
}
