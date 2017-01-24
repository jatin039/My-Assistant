package app.developer.jtsingla.myassistant.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import app.developer.jtsingla.myassistant.Activity.HomeActivity;

import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.parseContacts;

/**
 * Created by jssingla on 1/23/17.
 */

public class CommonAPIs {
    /* string to be displayed as messages to user */
    private static String permissionForContactRead = "Please provide me the permission to " +
            "read your contacts, so that I can proceed.";

    private static String[] negateKeywords = {
            "don't",
            "do not",
            "not",
            "dont"
    };

    /* some trimmable words, helpful in trimming down input string
     * when making a call or text. */
    private static String[] trimContactKeywords = {
        "my",
        "now",
        "mine",
        "girlfriend",
        "girl friend",
        "friend",
        "good"
    };

    /* this api will return whether the message contains
     * negative sense, like user is asking not to do something.
     */
    public static boolean isNegate(String message) {
        for (int i = 0; i < negateKeywords.length; i++) {
            if (message.toLowerCase().contains(negateKeywords[i])) {
                return true;
            }
        }
        return false;
    }

    /* This api will return the message after trimming down wasteful information
     * in message which may not be required for app to proceed to contact someone*/
    public static String trimMessageForContact(String message) {
        for (int i = 0; i < trimContactKeywords.length; i++) {
            if (message.contains(trimContactKeywords[i])) {
                message = message.replace(trimContactKeywords[i], "");
            }
        }
        return message;
    }

    /* This API will take three inputs,
     * Array of strings and a keyword and a message
     * replace VERY FIRST **ONLY ONE** string from the string array contained by message
     * and replace it with keyword,
     * useful when making logic to handle call, text.
     * like "make a call to " will be converted to "call"
     */
    public static String replaceStringWithKeyword(String[] array, String keyword, String message) {
        message = message.toLowerCase();
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].toLowerCase();
            if (message.contains(array[i])) {
                return message.replace(array[i], keyword);
            }
        }
        /* none of the patterns were found */
        return message;
    }

    /* this API removes all the redundant information from message and tries to extract name,
     * takes Inputs:
      * 1. Message to be trimmed.
      * 2. Keywords which we can remove. eg callKeywords. textKeywords
      * 3. Keyword you want to split message on,
      *    this will vary depending on action i.e call, text.. eg. "call", "text"
      * */
    public static String extractName(String message, String[] keywords,
                                     String action) throws ArrayIndexOutOfBoundsException {
        try {
            /* logic to retrieve name from the message */
            /* to extract the message, we will replace the message to standard format
            *  i.e "Call Someone" from "Make a call to Someone"
            *  i.e "Text Someone" from "Make a text to Someone"
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

    /* this API will return list of probable contacts if you have contact read permission
     * otherwise returns null */
    public static LinkedHashMap<String, String> getProbableContacts(Activity activity,
                                                                    String name) {
        if (!isReadContactsPermission(activity)) {
            // Read contacts permissions have not been granted.
            ArrayList<Message> messages = HomeActivity.messages;
            messages.add(new Message(false, permissionForContactRead));
            Log.i(HomeActivity.Permissions.PERMISSION_TAG, "Contact read permissions has NOT " +
                    "been granted. Requesting permissions.");
            // request permission again.
            HomeActivity.Permissions.requestContactsReadPermission(activity);
            /* FIXME: we can't wait for user to give the access to contacts,
             * one way to fix this will be to put this particular request to callStack
             * then after gaining the access, from onRequestPermissionResults, handle this
             * request. This feature is pending as of now. expecting user to make the request again*/
        } else {
            // Read contacts permissions was already granted, we should proceed.
            return parseContacts(activity,
                    name.toLowerCase());
        }
        return null;
    }

    /* this API is used for parsing the contacts against specific contactName <string>,
     * this API should only be called if you have read contacts permission from user.
     * takes contact name and
     * returns linked hashmap of probable contacts matching with that name */
    public static LinkedHashMap<String, String> parseContacts(Activity activity, String contactName) {
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
            /* If contact display name(stored in user contacts)
             contains all the words in contactName, then add it to probable contacts */
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
        return isPermissionGranted(activity, Manifest.permission.READ_CONTACTS);
    }

    public final static int convertStringResponseToInt(String message) {
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

    /* This API marks the action as done if calling action is same as action that was expected */
    public static void markActionAsDone(Context context, String callingAction) {
        try {
            String expectedAction = HomeActivity.readActionFromSharedPreferences(context);
            // only reset the action if it was call, should not interfere if action is something else.
            if (expectedAction.equals(callingAction)) HomeActivity
                    .writeActionToSharedPreferences(context, null, null, null);
        } catch (NullPointerException npe) {
            /* null pointer exception will be thrown if no action was expected */
            Log.e("markActionAsDone", npe.getMessage());
            return;
        }
    }

    public static void markActionAsExpected(Context context, String action, String subAction,
                                            Object actionObject) {
        HomeActivity.writeActionToSharedPreferences(context, action, subAction, actionObject);
    }

    public static boolean isPermissionGranted(Activity activity, @NonNull String permission) {
        return (ActivityCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED);
    }
}
