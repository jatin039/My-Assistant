package app.developer.jtsingla.myassistant.Actions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
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

import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.convertStringResponseToInt;
import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.extractName;
import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.getProbableContacts;
import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.isNegate;
import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.isPermissionGranted;
import static app.developer.jtsingla.myassistant.Utils.CommonAPIs.markActionAsDone;

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

    private static String[] callResponseMessages = {
         "Ok",
         "Trying to make the call",
         "Will do that just now",
         "At your service, always"
    };

    private static final String MYACTION = "call";
    private static String noContactsFound = "Didn't find any contact with name";
    private static String makingCall = "Making a call to ";
    private static String lotOfResults = "Please be more specific. " +
            "There are a lot of results with ";
    private static int MAX_RESULTS = 10;

    /* tries to perform the call */
    public static void attemptPerformCall(Context context, String message) {
        if (isNegate(message)) return; /* return if user is asking not to make a call */
        ArrayList<Message> messages = HomeActivity.messages;
        /* check if we have permissions to Contacts */
        String name = extractName(message, callKeywords, "call");
        LinkedHashMap<String, String> probableContacts =
                getProbableContacts((Activity)context, name);
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
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    messages.add(new Message(false, i + "- " + pair.getKey() + ": " + pair.getValue()));
                    i++;
                }
                /* now we need input from user to give the contact number,
                 * putting call as an expecting action */
                HomeActivity.writeActionToSharedPreferences(context, "call", probableContacts);
        }
    }

    /* action handler for specific action
    * TODO: features to be added,
    * 1. When only Call was mentioned and not the name, (needs to be marked as expecting input also)
    */
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
                    markActionAsDone(context, MYACTION);
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
        } else {
            messages.add(new Message(false, "Please give permission to call."));
            /* request permission again */
            HomeActivity.Permissions.checkMakeCallPermission((Activity)context);
        }
    }
}
