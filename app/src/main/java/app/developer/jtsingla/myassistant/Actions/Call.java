package app.developer.jtsingla.myassistant.Actions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import app.developer.jtsingla.myassistant.Decider.ActionDecider;
import app.developer.jtsingla.myassistant.Activity.HomeActivity;
import app.developer.jtsingla.myassistant.Utils.Message;

import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.*;

/**
 * Created by jssingla on 1/16/17.
 */

public class Call {
    /*
    This class will handle all the requests for calling,
    It should understand what user exactly user want to do
     */
    /* TODO: add synonymus names like amma, mummy, mother for mom and other various names */
    private static String[] callKeywords = {
            // IMP: later keywords should be subset of former ones
        "ring call to",
        "make call to",
        "make ring to",
        "ring phone to",
        "make phone to",
        "make a call to",
        "make a phone to",
        "make a ring to",
        "make call",
        "make ring",
        "make phone",
        "ring to",
        "phone to",
        "call to",
        "ring",
        "phone",
        "call"
    };

    /* some more trimmable words */
    private static String[] callTrimKeywords = {
        "my",
        "now",
        "mine",
        "girlfriend",
        "girl friend",
        "friend"
    };

    private static String[] negateCallResponseMessages = {
        "Ok",
        "Hmm",
        "Not making the call...",
        "Got it."
    };

    /* Not using this now.
    private static String[] callResponseMessages = {
         "Ok",
         "Trying to make the call",
         "Will do that just now",
         "At your service, always"
    };*/

    /* for handling actions */
    private static final String MYACTION = "call";

    private enum actionTypes {
        /* when user said a name which brought many results (1-MAX_RESULTS)
         * take input from user and set this action type. */
        GetResultFromContactList("GetResultFromContactList"),
        /* when user is yet to enter a name, just entered the keyword. */
        GetContact("GetContact");

        private String actionType;

        private actionTypes(String actionType) {
            actionType = actionType;
        }

        public String getActionType() {
            return actionType;
        }
    }

    private static String makingCall = "Making a call to ";
    private static String whomToCall = "Whom should I call?";
    private static int MAX_RESULTS = 10;

    /* tries to perform the call */
    public static void attemptPerformCall(Context context, String message) {
        if (isNegate(message)) return; /* return if user is asking not to make a call */
        ArrayList<Message> messages = HomeActivity.messages;
        /* check if we have permissions to Contacts */
        String name = extractCallName(message, callKeywords, MYACTION);
        if (name == null) {
            /* only keyword was present in the message, no name */
            /* ask user for further input and mark action as expected */
            messages.add(new Message(false, whomToCall));
            markActionAsExpected(context, MYACTION, actionTypes.GetContact.toString(),
                    null/* user is expected to enter a name, so no probablecontacts are needed.*/);
            return;
        }
        LinkedHashMap<String, String> probableContacts =
                getProbableContacts((Activity)context, name);

        interfacePerformCall(probableContacts, name, context);
    }

    /* this api is interface between attempt call and actually performing a call */
    private static void interfacePerformCall(LinkedHashMap<String, String> probableContacts,
                                                    String contactName, Context context) {
        ArrayList<Message> messages = HomeActivity.messages;
        if (probableContacts == null) {
            /* no request read permission, user will have to make new request. */
            return;
        }

        /* TODO : handle cases:
         * Case 1: when only 1 result
         * Case 2: when no result,
         *          ask the user to select a contact, and map that string to selected contact
         *          for next time onwards
         * Case 3: multiple results */
        switch (probableContacts.size()) {
            case 0: /* no results found */
                messages.add(new Message(false, noContactsFound + ": " + contactName));
                markActionAsDone(context, MYACTION);
                break;
            case 1: /* only one result found */
                Map.Entry<String, String> entry = probableContacts.entrySet().iterator().next();
                Pair<String, String> contact = new Pair<>(entry.getKey(), entry.getValue());
                performCall(context, contact);
                break;
            default: /* multiple results */
                /* if there are a lot of results more than MAX_RESULTS,
                 * we ask user to be more specific.
                 * TODO?? We may add action expected for Call but for now not adding that. */
                if (probableContacts.size() > MAX_RESULTS) {
                    messages.add(new Message(false, lotOfResults + ": " + contactName));
                    return;
                }
                Iterator it = probableContacts.entrySet().iterator();
                messages.add(new Message(false, "I have found " + probableContacts.size() +
                        " results. Please choose a result no. to select a contact."));
                int i = 1;
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    messages.add(new Message(false, i + "- " + pair.getKey() + ": " +
                            pair.getValue()));
                    i++;
                }
                /* now we need input from user to give the contact number,
                 * putting call as an expecting action */

                markActionAsExpected(context, MYACTION,
                        actionTypes.GetResultFromContactList.toString(), probableContacts);
        }
    }

    /* action handler for specific action
    * TODO: features to be added,
    * 1. When only Call was mentioned and not the name, (needs to be marked as expecting input also)
    */
    public static void actionHandler(Context context, String message) {
        /* get the subAction for Call */
        String subAction = HomeActivity.readSubActionFromSharedPreferences(context);

        /* **ALARM** if subAction was not inserted */
        if (subAction == null) {
            Log.e("callActionHandler", "No subAction found. should not happen. call decideAction.");
            ActionDecider.performAction(context, message);
            return;
        }

        if (subAction.equals(actionTypes.GetContact.toString())) {
            handleGetContactAction(context, message);
        } else if (subAction.equals(actionTypes.GetResultFromContactList.toString())) {
            handleGetResultFromContactListAction(context, message);
        } else {
            /* place holder for adding other cases later on */
            /* TODO: should we mark the actions as done */
            /* marking this as done for now*/
            markActionAsDone(context, MYACTION);
        }
    }

    private static void handleGetContactAction(Context context, String message) {
        // action object is not required for this.
        ArrayList<Message> messages = HomeActivity.messages;
        try {
            String name = extractCallName(message, callKeywords, MYACTION);
            LinkedHashMap<String, String> probableContacts = getProbableContacts((Activity) context,
                    name);
            interfacePerformCall(probableContacts, name, context);
        } catch (NullPointerException npe) {
            messages.add(new Message(false, ActionDecider.generateRandomMessage(invalidNames)));
            Log.e("callHandleGetContact", npe.getMessage());
            /* do not mark action as done, as user will give valid name next time.*/
            //markActionAsDone(context, MYACTION);
            return;
        }
    }

    /* This API will expect the incoming message to be of the form
        ***** "Call keyword + name" | "name + Call keyword" ******
        will return the name.
     */
    private static String extractCallName(String message, String[] keywords,
                                     String action) throws ArrayIndexOutOfBoundsException {
        try {
            /* logic to retrieve name from the message */
            /* to extract the message, we will replace the message to standard format
            *  i.e "Call Someone" from "Make a call to Someone"
            *
            *  we will replace common known strings to this format and then proceed*/
            message = replaceStringWithKeyword(keywords, action, message);

            /* trim the message of all trimmable words */
            message = trimMessageForContact(message);

            /* TODO: as of now expecting the name to be last in the sentence, not handling name first */
            String[] splitMessage = message.split(action);
            return splitMessage[splitMessage.length-1].trim();
        } catch (ArrayIndexOutOfBoundsException oobe) {
            Log.e("extractName", "Only keyword was present in message. " +
                    "Will ask user for input. will mark action as expected. " + oobe.getMessage());
            return null;
        }
    }

    private static void handleGetResultFromContactListAction(Context context, String message) {
        String jsonAction = HomeActivity.readJsonActionObjectFromSharedPreferences(context);
        ArrayList<Message> messages = HomeActivity.messages;
        if (jsonAction == null) {
            /* if object required is not there */
            // TODO: is this possible? need use cases for this.
        }
        LinkedHashMap<String, String> probableContacts;
        Gson gson = new Gson();
        Type type = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
        probableContacts = gson.fromJson(jsonAction, type);
        /* for now assuming message to be only a number, TODO: think of other use cases for call
         * maybe user entered name of contact, do a exact match search then **MAYBE** */
        try {
            int idx = convertStringResponseToInt(message);
            if (idx == 0) idx = Integer.parseInt(message);
            if (idx > probableContacts.size() || idx < 1) {
                messages.add(new Message(false, "Please select a number from 1 - " + probableContacts.size()));
                /* still expect a action from user as he entered invalid number,
                   may want to enter again */
                return;
            }
            Iterator it = probableContacts.entrySet().iterator();
            int i = 1;
            while (it.hasNext()) {
                if (i == idx) {
                    Map.Entry<String, String> pair = (Map.Entry)it.next();
                    Pair<String, String> contact = new Pair<>(pair.getKey(), pair.getValue());
                    performCall(context, contact);
                    break;
                }
                it.next();
                i++;
            }
        } catch (NumberFormatException nfe) {
            Log.e("Call", nfe.getMessage());
            markActionAsDone(context, MYACTION);
            ActionDecider.performAction(context, message);
        }
    }

    /* returns true if make call permission is granted */
    private static boolean isMakeCallPermission(Activity activity) {
        return isPermissionGranted(activity, Manifest.permission.CALL_PHONE);
    }

    private static void performCall(Context context, Pair<String, String> contact) {
        // make a call to contact.
        ArrayList<Message> messages = HomeActivity.messages;
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + contact.second));
        if (isMakeCallPermission((Activity)context)) {
            messages.add(new Message(false, makingCall + contact.first));
            context.startActivity(intent);
            markActionAsDone(context, MYACTION);
        } else {
            messages.add(new Message(false, "Please give permission to call."));
            /* request permission again */ // TODO: add permission expected response
            HomeActivity.Permissions.checkMakeCallPermission((Activity)context);
        }
    }
}
