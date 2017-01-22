package app.developer.jtsingla.myassistant.Actions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private static String permissionForContactRead = "Please provide me the permission to " +
            "read your contacts, so that I can proceed.";

    private static String noContactsFound = "Didn't find any contact with name";
    private static String makingCall = "Making a call to ";
    private static String lotOfResults = "Please be more specific. " +
            "There are a lot of results with ";
    private static int MAX_RESULTS = 10;

    private final static int convertStringResponseToInt(String message) {
        switch (message.toLowerCase()) {
            case "first":
            case "one":
                return 1;
            case "second":
            case "two":
                return 2;
            case "third":
            case "three":
                return 3;
            case "forth":
            case "four":
                return 4;
            case "fifth":
            case "five":
                return 5;
            case "sixth":
            case "six":
                return 6;
            case "seventh":
            case "seven":
                return 7;
            case "eighth":
            case "eight":
                return 8;
            case "ninth":
            case "nine":
                return 9;
            case "tenth":
            case "ten":
                return 10;
            default:
                return 0;
        }
    }

    /* tries to perform the call */
    public static void attemptPerformCall(Context context, String message) {
        if (negateCall(message)) return; /* return if user is asking not to make a call */
        ArrayList<Message> messages = HomeActivity.messages;
        /* check if we have permissions to Contacts */
        if (!isReadContactsPermission((Activity) context)) {
            // Read contacts permissions have not been granted.
            messages.add(new Message(false, permissionForContactRead));
            Log.i(HomeActivity.Permissions.PERMISSION_TAG, "Contact read permissions has NOT " +
                    "been granted. Requesting permissions.");
            // request permission again.
            HomeActivity.Permissions.requestContactsReadPermission((Activity) context);
            /* FIXME: we can't wait for user to give the access to contacts,
             * one way to fix this will be to put this particular request to callStack
             * then after gaining the access, from onRequestPermissionResults, handle this
             * request. This feature is pending as of now.*/
        } else {
            /* not showing random messages as of now */
            //messages.add(new Message(false, ActionDecider.generateRandomMessage(callResponseMessages)));
            // Read contacts permissions was already granted, we should follow with call.
            String name = extractName(message);
            LinkedHashMap<String, String> probableContacts = parseContacts((Activity) context,
                    name.toLowerCase());
            /* TODO : handle cases:
             * Case 1: when only 1 result
             * Case 2: when no result,
             *          ask the user to select a contact, and map that string to selected contact
             *          for next time onwards
             * Case 3: multiple results */
            switch (probableContacts.size()) {
                case 0: /* no results found */
                    messages.add(new Message(false, noContactsFound + ": " + name));
                    break;
                case 1: /* only one result found */
                    messages.add(new Message(false, ActionDecider.generateRandomMessage(callResponseMessages)));
                    Map.Entry<String, String> entry = probableContacts.entrySet().iterator().next();
                    Pair<String, String> contact = new Pair<>(entry.getKey(), entry.getValue());
                    performCall(context, contact);
                    break;
                default: /* multiple results */
                    /* if there are a lot of results more than MAX_RESULTS,
                     * we ask user to be more specific.
                     * TODO?? We may add action expected for Call but for now not adding that. */
                    if (probableContacts.size() > MAX_RESULTS) {
                        messages.add(new Message(false, lotOfResults + ": " + name));
                        return;
                    }
                    Iterator it = probableContacts.entrySet().iterator();
                    messages.add(new Message(false, "We have found " + probableContacts.size() +
                        " results. Please choose the result number to select a contact."));
                    int i = 1;
                    while(it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        messages.add(new Message(false, i + "- "+ pair.getKey() + ": " + pair.getValue()));
                        i++;
                    }
                    /* now we need input from user to give the contact number,
                     * putting call as an expecting action */
                    HomeActivity.writeActionToSharedPreferences(context, "call", probableContacts);
            }
        }
    }

    private static void actionHandled(Context context) {
        String action = HomeActivity.readActionFromSharedPreferences(context);
        // only reset the action if it was call, should not interfere if action is something else.
        if (action == "call") HomeActivity.writeActionToSharedPreferences(context, null, null);
    }

    public static void actionHandler(Context context, String message) {
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
                    actionHandled(context);
                    break;
                }
                it.next();
                i++;
            }
        } catch (NumberFormatException nfe) {
            Log.e("Call", nfe.getMessage());
            actionHandled(context);
            ActionDecider.performAction(context, message);
        }
    }

    private static String extractName(String message) {
        /* logic to retrieve name from the message */
        /* as of now I am returning whole message and we will check word by word */
        /* to extract the message, we will replace the message to standard format
        *  i.e "Call Someone"
        *
        *  we will replace common known strings to this format and then proceed*/
        message = message.toLowerCase();
        for (int i = 0; i < callKeywords.length; i++) {
            // check message for all call keywords and replace with call
            if (message.contains(callKeywords[i])) {
                message = message.replace(callKeywords[i], "call");
                break; // break once one pattern was found, not handling multiple
                // patterns in one string as that would be insane.
            }
        }

        /* trim the message of all trimmable words */
        for (int i = 0; i < callTrimKeywords.length; i++) {
            if (message.contains(callTrimKeywords[i])) {
                message = message.replace(callTrimKeywords[i], "");
            }
        }
        /* TODO: as of now expecting the name to be last in the sentence, not handling name first */
        String[] splitMessage = message.split("call");
        return splitMessage[splitMessage.length-1].trim();
    }

    private static LinkedHashMap<String, String> parseContacts(Activity activity, String contactName) {
        Cursor phones = activity.getContentResolver().query(ContactsContract
                .CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
        LinkedHashMap<String, String> probableContacts = new LinkedHashMap<>();
        while (phones.moveToNext())
        {
            String name = phones.getString(phones.getColumnIndex(ContactsContract
                    .CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract
                    .CommonDataKinds.Phone.NUMBER));

            /* if contact name matches name, add it to list */
            String[] listOfWords = contactName.split(" ");
            if (containsAllWordsIn(name, listOfWords)) {
                probableContacts.put(name, phoneNumber);
            }
        }
        phones.close();
        return probableContacts;
    }

    /* check if String checked contains all words in list */
    private static boolean containsAllWordsIn(String checked, String[] list) {
        for (int i = 0; i < list.length; i++) {
            if (!checked.toLowerCase().contains(list[i])) {
                return false;
            }
        }
        return true;
    }

    /* returns true if permission is granted */
    private static boolean isReadContactsPermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED);
    }

    /* returns true if permission is granted */
    private static boolean isMakeCallPermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED);
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

    private static void performCall(Context context, Pair<String, String> contact) {
        // make a call to contact.
        ArrayList<Message> messages = HomeActivity.messages;
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + contact.second));
        if (isMakeCallPermission((Activity)context)) {
            messages.add(new Message(false, makingCall + contact.first));
            context.startActivity(intent);
        } else {
            messages.add(new Message(false, "Please give permission to call."));
            /* request permission again */
            HomeActivity.Permissions.checkMakeCallPermission((Activity)context);
        }
    }
}
