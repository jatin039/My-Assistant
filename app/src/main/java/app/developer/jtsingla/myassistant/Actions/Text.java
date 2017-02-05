package app.developer.jtsingla.myassistant.Actions;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import app.developer.jtsingla.myassistant.Activity.HomeActivity;
import app.developer.jtsingla.myassistant.DTOs.TextDTO;
import app.developer.jtsingla.myassistant.Decider.ActionDecider;
import app.developer.jtsingla.myassistant.Utils.Message;

import static app.developer.jtsingla.myassistant.Activity.HomeActivity.*;
import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.*;

/**
 * Created by jssingla on 1/23/17.
 */

public class Text {

    private enum actionTypes {
        /* when user said a name which brought many results (1-MAX_RESULTS)
         * take input from user and set this action type. */
        GetResultFromContactList("GetResultFromContactList"),
        /* when user is yet to enter a name, just entered the keyword. */
        GetContact("GetContact"),
        /* when user is yet to enter what to text */
        GetText("GetText"),
        /* when user is yet to enter text Method */
        GetTextMethod("GetTextMethod");

        private String actionType;

        private actionTypes(String actionType) {
            actionType = actionType;
        }
    }

    private enum textMethod {
        WHATSAPPALWAYS("Always use whatsapp?"),
        MESSAGINGALWAYS("Always use messaging?"),
        WHATSAPP("Use whatsapp this time?"),
        MESSAGING("Use messaging this time?");

        private String textMethod;

        private textMethod(String s) {
            textMethod = s;
        }
    }

    private static String[] textKeywordCommonPrefixes = {
            "send a", "send",
            "make a", "make",
            "a",
            "", // no prefix
    };

    private static String[] textKeywordCommonSuffixes = {
            "to", "that",
            "" // none
    };

    private static String[] textKeywords = {
        "text message", "whatsapp message", "whatsapp text", "text" , "message", "whatsapp"
    };

    /* This string array will give the possible prepositions which can be present
       in a string while saying a text.
       eg. Send a text that ....
     */
    private static String[] textPrepositions = {
            "to",
            "that",
            //...
    };

    private static String[] textDividers = {
            "that"
    };

   /* Not using this now.
   private static String[] textResponseMessages = {
            "Ok",
            "Trying to send the text",
            "Will do that just now",
            "At your service, always"
    };*/

    private static final int MAX_RESULTS = 10;
    private static String whomToText = "Whom should I text?";
    private static String whatToText = "What is the message?";
    private static String MYACTION = "text";
    private static String sendingText = "Sending a text to %s : %s";
    private static String whichTextMethod = "Please select a method with method number.";

    public static void attemptSendText(Context context, String message) {
        if (isNegate(message)) {
            return; // if user tells not to make a text. when someone is trying to test the app. :D
        }
        ArrayList<Message> messages = HomeActivity.messages;
        String[] keywords = makeKeywords(textKeywordCommonPrefixes, textKeywordCommonSuffixes, textKeywords);
        Pair<String, String> nameAndText = extractNameAndText(message, keywords,
                                            (Activity)context);
        String contactName = nameAndText.first;
        String text = nameAndText.second;
        TextDTO textDTO = new TextDTO();
        textDTO.setText(text);
        writeTextDTOToSharedPreferences(context, textDTO);
        if (contactName == null) {
            /* only keyword was present in the message, no name */
            /* ask user for further input and mark action as expected */
            messages.add(new Message(false, whomToText));
            markActionAsExpected(context, MYACTION, Text.actionTypes.GetContact.toString(),
                    null/* user is expected to enter a name, so no probablecontacts are needed.*/);
            return;
        }
        /* save text to shared Prefs */

        LinkedHashMap<String, String> probableContacts =
                getProbableContacts((Activity)context, contactName);

        interfacePerformText(probableContacts, contactName, context);
    }

    /* action handler for specific action
   * TODO: features to be added,
   * 1. When only Text was mentioned and not the name, (needs to be marked as expecting input also)
   */
    public static void actionHandler(Context context, String message) {
        /* get the subAction for text */
        String subAction = HomeActivity.readSubActionFromSharedPreferences(context);

        /* **ALARM** if subAction was not inserted */
        if (subAction == null) {
            Log.e("textActionHandler", "No subAction found. should not happen. call decideAction.");
            ActionDecider.performAction(context, message);
            return;
        }

        if (subAction.equals(Text.actionTypes.GetContact.toString())) {
            handleGetContactAction(context, message);
        } else if (subAction.equals(Text.actionTypes.GetResultFromContactList.toString())) {
            handleGetResultFromContactListAction(context, message);
        } else if (subAction.equals(actionTypes.GetText.toString())) {
            handleGetTextAction(context, message);
        } else if (subAction.equals(actionTypes.GetTextMethod.toString())) {
            handleGetTextMethod(context, message);
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
            Pair<String, String> nameAndText = extractNameAndText(message, textKeywords,
                    (Activity)context);
            String name = nameAndText.first;
            LinkedHashMap<String, String> probableContacts = getProbableContacts((Activity) context,
                    name);
            interfacePerformText(probableContacts, name, context);
        } catch (NullPointerException npe) {
            messages.add(new Message(false, ActionDecider.generateRandomMessage(invalidNames)));
            Log.e("textHandleGetContact", npe.getMessage());
            /* do not mark action as done, as user will give valid name next time.*/
            //markActionAsDone(context, MYACTION);
            return;
        }
    }

    private static void handleGetTextAction(Context context, String message) {
        /* take all of message as text to be sent */
        TextDTO textDTO = readTextDTOFromSharedPreferences(context);
        textDTO.setText(message);
        writeTextDTOToSharedPreferences(context, textDTO);
        performText(context, message);
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
                    performText(context, contact);
                    break;
                }
                it.next();
                i++;
            }
        } catch (NumberFormatException nfe) {
            Log.e(MYACTION, nfe.getMessage());
            markActionAsDone(context, MYACTION);
            ActionDecider.performAction(context, message);
        }
    }

    private static void handleGetTextMethod(Context context, String message) {
        try {
            int idx = convertStringResponseToInt(message);
            if (idx == 0) idx = Integer.parseInt(message);
            if (idx <= 0 || idx > 4) {
                messages.add(new Message(false, "Please select a number from 1 - 4"));
                /* still expect a new number from user, i.e do not mark action as done */
                return;
            }
            /* get TextDTO from shared prefs */
            TextDTO textDTO = readTextDTOFromSharedPreferences(context);
            if (textDTO == null || textDTO.isDummyContact() || textDTO.isDummyText()) {
                /* should not be the case, we ensured before asking method for all other fields
                to have proper values. ALARM
                 */
                return;
            }
            switch (idx) {
                case 1:
                    /* use messaging this time,
                    no need to put this in shared pref as it is one time thing*/
                    sendTextUsingMessaging(context, textDTO.getContact(), textDTO.getText());
                    break;
                case 2:
                    /* use whatsapp this time,
                     * no need to put this in shared prefs as it is one time thing*/
                    sendTextUsingWhatsapp(context, textDTO.getContact(), textDTO.getText());
                    break;
                case 3:
                    /* use messaging always,
                     * put this in shared pref. TODO: later on give an option to change somehow */
                    writeTextMethodToSharedPreferences(context, textMethod
                            .MESSAGINGALWAYS.toString());
                    sendTextUsingMessaging(context, textDTO.getContact(), textDTO.getText());
                    break;
                case 4:
                    /* use whatsapp always,
                     * put this in shared pref, TODO: later on give an option to change somehow */
                    writeTextMethodToSharedPreferences(context, textMethod
                            .WHATSAPPALWAYS.toString());
                    sendTextUsingWhatsapp(context, textDTO.getContact(), textDTO.getText());
                    break;
                default:
            }
        } catch (NumberFormatException nfe) {
            Log.e(MYACTION, nfe.getMessage());
            ActionDecider.performAction(context, message);
        }
        /* mark action as done */
        markActionAsDone(context, MYACTION);
    }

    public static void interfacePerformText(LinkedHashMap<String, String> probableContacts,
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
                performText(context, contact);
                break;
            default: /* multiple results */
                /* if there are a lot of results more than MAX_RESULTS,
                 * we ask user to be more specific.
                 * TODO?? We may add action expected for Text but for now not adding that. */
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
                        Text.actionTypes.GetResultFromContactList.toString(), probableContacts);
        }
    }

    /* This API extracts contact name and text from message
    * assumes message to be in this format :
    * message keyword + preposition/or not + "contact" + preposition + "text"
    * TODO: not handling below case as of now
    * message keyword + preposition + "text" + preposition + "contact"*/

    private static Pair<String /*name*/, String /*text*/> extractNameAndText(String message,
                                                    String[] keywords, Activity activity) {
        String name, text;

        message = message.toLowerCase();
        String strippedMessage = message;
        for (String a : keywords) {
            if (message.contains(a)) {
                strippedMessage = message.replaceAll(a, "");
                break;
            }
        }
        strippedMessage.trim();
        final String PREP = "prepositon_found";
        // split message on prepositions
        /* initially mark this as stripped message */
        String[] splitMessageOnPrepositions = {strippedMessage};
        for (String a : textPrepositions) {
            if (strippedMessage.contains(a)) {
                strippedMessage = strippedMessage.replaceFirst(a, PREP);
                splitMessageOnPrepositions = strippedMessage.split(PREP);
                break;
            }
        }


        switch (splitMessageOnPrepositions.length) {
            case 1:
                /* return null if empty string */
                if (splitMessageOnPrepositions[0].equals("")) {
                    return new Pair<>(null, null);
                }
                /* either name or message, LETS Find out */
                LinkedHashMap<String, String> probableContacts = getProbableContacts(activity,
                        splitMessageOnPrepositions[0]);
                if (probableContacts.size() != 0) {
                    /* Woaah, its a contact */
                    name = splitMessageOnPrepositions[0];
                    text = null;
                } else {
                    /* Ohh, that means it's a text */
                    text = splitMessageOnPrepositions[0];
                    name = null;
                }
                return new Pair<>(name, text);
            case 2:
                /* both name and message, assign blindly?
                * TODO: may need to check when we handle the cases
                * TODO: where text may come first then message */
                name = splitMessageOnPrepositions[0];
                text = splitMessageOnPrepositions[1];
                return new Pair<>(name, text);
            default:
                /* not possible unless empty string i.e 0 length*/
                return new Pair<>(null, null);
        }
    }

    /* this API asks the user for specifying the method and set the action as expected. */
    private static void askMethod(Context context) {
        messages.add(new Message(false, whichTextMethod));
        messages.add(new Message(false, "1: " + textMethod.MESSAGING.toString()));
        messages.add(new Message(false, "2: " + textMethod.WHATSAPP.toString()));
        messages.add(new Message(false, "3: " + textMethod.MESSAGINGALWAYS.toString()));
        messages.add(new Message(false, "4: " + textMethod.WHATSAPPALWAYS.toString()));
        markActionAsExpected(context, MYACTION, actionTypes.GetTextMethod.toString(),
                null/* do not need any object for this action to be marked */);
    }

    private static void performText(Context context, String text) {
        /* Here it will come if we got message, but contact may or may not have been specified */
        TextDTO textDTO = readTextDTOFromSharedPreferences(context);

        if (textDTO == null) {
            textDTO = new TextDTO();
        }

        if (textDTO.isDummyContact()) {
            /* save text to sharedPreferences */
            textDTO.setText(text);
            writeTextDTOToSharedPreferences(context, textDTO);
            /* get contact from user */
            markActionAsExpected(context, MYACTION, actionTypes.GetText.GetContact.toString(),
                    null /* no probable contacts are needed */);
            return;
        }

        /* if we have both text and contact */
        actuallySendText(context, textDTO.getContact(), text);
    }

    private static void performText(Context context, Pair<String, String> contact) {
        /* Here it will come if we got the contact, but message either is not specified yet
         * or we need to find from shared prefs.*/
        TextDTO textDTO = readTextDTOFromSharedPreferences(context);

        if (textDTO == null) {
            textDTO = new TextDTO();
        }

        if (textDTO.isDummyText()) {
            /* save the contact to shared preferences */
            textDTO.setContact(contact);
            writeTextDTOToSharedPreferences(context, textDTO);
            /* Text is not specified, ask for text from user */
            messages.add(new Message(false, whatToText));
            markActionAsExpected(context, MYACTION, actionTypes.GetText.toString(),
                    null /* no probable contacts needed as contact is already specified
                            and we will write that contact to textDTO*/);
            return;
        }

        /* if we have both text and contact */
        actuallySendText(context, contact, textDTO.getText());
    }

    private static void actuallySendText(Context context, Pair<String, String> contact,
                                         String text) {
        /* feeW! Finally! check if user has specified how to send the text i.e Text Method*/
        String textMethod = readTextMethodFromSharedPreferences(context);

        String sendingMessage = String.format(sendingText, contact.first , text);
        if (textMethod != null &&
                textMethod.equals(Text.textMethod.MESSAGINGALWAYS.toString())) {
            messages.add(new Message(false, sendingMessage));
            /* always use messaging for sending messages */
            sendTextUsingMessaging(context, contact, text);
        } else if (textMethod != null &&
                textMethod.equals(Text.textMethod.WHATSAPPALWAYS.toString())) {
            messages.add(new Message(false, sendingMessage));
            /* always use whatsapp for sending messages */
            sendTextUsingWhatsapp(context, contact, text);
        } else {
            /* ask user about how to send the text */
            /* save text data to shared preferences,
                so that we can use it after method is specified */
            TextDTO textDTO = readTextDTOFromSharedPreferences(context);
            textDTO.setText(text);
            textDTO.setContact(contact);
            writeTextDTOToSharedPreferences(context, textDTO);
            askMethod(context);
            return;
        }
        /* mark textDTO as dummy for subsequent requests */
        writeTextDTOToSharedPreferences(context, null);
        markActionAsDone(context, MYACTION);
    }

    private static void sendTextUsingMessaging(Context context, Pair<String, String> contact,
                                               String text) {
        try {
            if (isSendSMSPermission((Activity)context)) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(contact.second, null, text, null, null);
                messages.add(new Message(false, "SMS successfully sent."));
            } else {
                messages.add(new Message(false, "Please give permission to send SMS."));
                /* request permission again */ // TODO: add permission expected response
                HomeActivity.Permissions.checkSendSMSPermission((Activity)context);
            }
        } catch (Exception ex) {
            messages.add(new Message(false, "Error while sending SMS."));
            ex.printStackTrace();
        }
    }

    private static boolean isSendSMSPermission(Activity activity) {
        return isPermissionGranted(activity, Manifest.permission.SEND_SMS);
    }

    private static void sendTextUsingWhatsapp(Context context, Pair<String, String> contact,
                                              String text) {
/*
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");

        intent.setPackage("com.whatsapp");
        if (intent != null) {
            intent.putExtra(Intent.EXTRA_TEXT, text);//
            context.startActivity(Intent.createChooser(intent, text));
        } else {
            Toast.makeText(context, "App not found", Toast.LENGTH_SHORT)
                    .show();
        }
*/
        String countryCode = "91"; // hard coding for india as of now for testing
/*
        Uri uri = Uri.parse("smsto:" + countryCode +
                contact.second.substring(contact.second.length()-10) */
/* taking last 10 characters*//*
);
        Intent i = new Intent(Intent.ACTION_SEND, uri);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        i.setPackage("com.whatsapp");
        context.startActivity(i);

*/
        Intent sendIntent = new Intent("android.intent.action.MAIN");
        sendIntent.setComponent(new ComponentName("com.whatsapp","com.whatsapp.Conversation"));
        sendIntent.putExtra("jid", PhoneNumberUtils.stripSeparators(countryCode +
                contact.second.substring(contact.second.length()-10))+"@s.whatsapp.net");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        sendIntent.setAction(Intent.ACTION_SEND);
        context.startActivity(sendIntent);
    }
}
