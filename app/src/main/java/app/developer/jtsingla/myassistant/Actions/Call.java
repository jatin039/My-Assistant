package app.developer.jtsingla.myassistant.Actions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
            messages.add(new Message(false, ActionDecider.generateRandomMessage(callResponseMessages)));
            // Read contacts permissions was already granted, we should follow with call.
            String name = extractName(message);
            HashMap<String, String> probableContacts = parseContacts((Activity) context,
                    name.toLowerCase());
            /* TODO : handle cases:
             * Case 1: when only 1 result
             * Case 2: when no result,
             *          ask the user to select a contact, and map that string to selected contact
             *          for next time onwards
             * Case 3: multiple results */
            Iterator it = probableContacts.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                messages.add(new Message(false, pair.getKey() + ": " + pair.getValue()));
            }
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

    private static HashMap<String, String> parseContacts(Activity activity, String contactName) {
        Cursor phones = activity.getContentResolver().query(ContactsContract
                .CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
        HashMap<String, String> probableContacts = new HashMap<>();
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
