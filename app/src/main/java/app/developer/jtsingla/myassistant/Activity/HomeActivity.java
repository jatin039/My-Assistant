package app.developer.jtsingla.myassistant.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import android.Manifest;

import app.developer.jtsingla.myassistant.Actions.Call;
import app.developer.jtsingla.myassistant.Actions.Text;
import app.developer.jtsingla.myassistant.DTOs.TextDTO;
import app.developer.jtsingla.myassistant.Decider.ActionDecider;
import app.developer.jtsingla.myassistant.Utils.ListAdapter;
import app.developer.jtsingla.myassistant.Utils.Message;
import app.developer.jtsingla.myassistant.R;

public class HomeActivity extends AppCompatActivity {

    public static ArrayList<Message> messages;
    public final static String CHAT = "chat";
    public final static String MESSAGES = "messages";

    public final static String EXPECTED_RESPONSE = "expectedResponse";
    public final static String ACTION = "action";
    public final static String SUBACTION = "subAction";
    public final static String TEXTMETHOD = "textMethod";
    public final static String TEXTDTO = "textDTO";
    public final static String[] ACTIONS = {
            "call", "reminder" // ... TODO: add as and when necessary
    };
    public final static String ACTION_OBJECT = "object";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        messages = readMessagesFromSharedPreferences(this);
        displayMessages();
        Permissions.checkAllPermissions(HomeActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static TextDTO readTextDTOFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXPECTED_RESPONSE,
                                                                        context.MODE_PRIVATE);
        String text = sharedPreferences.getString(TEXTDTO, null);
        TextDTO textDTO = new TextDTO();
        Gson gson = new Gson();
        Type type = new TypeToken<TextDTO>() {}.getType();
        textDTO = gson.fromJson(text, type);
        return textDTO;
    }

    public static void writeTextDTOToSharedPreferences(Context context, TextDTO textDTO) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXPECTED_RESPONSE,
                                                                        context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(textDTO);
        editor.putString(TEXTDTO, json);
        editor.commit();
    }

    public static String readActionFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXPECTED_RESPONSE,
                                                                        context.MODE_PRIVATE);
        String action = sharedPreferences.getString(ACTION, null);
        return action;
    }

    public static String readSubActionFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXPECTED_RESPONSE,
                                                                            context.MODE_PRIVATE);
        String subAction = sharedPreferences.getString(SUBACTION, null);
        return subAction;
    }

    public static String readJsonActionObjectFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXPECTED_RESPONSE,
                                                                        context.MODE_PRIVATE);
        String jsonActionResponse = sharedPreferences.getString(ACTION_OBJECT, null);
        return jsonActionResponse;
    }

    public static void writeActionToSharedPreferences(Context context, String action,
                                                            String subAction, Object object) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXPECTED_RESPONSE,
                                                                            context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ACTION, action);
        editor.putString(SUBACTION, subAction);
        Gson gson = new Gson();
        String json = gson.toJson(object);
        editor.putString(ACTION_OBJECT, json);
        editor.commit();
    }

    public static ArrayList<Message> readMessagesFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(CHAT,
                                                            context.MODE_PRIVATE);
        String json = sharedPreferences.getString(MESSAGES, null);
        if (json == null) {
            return (messages = new ArrayList<Message>());
        }
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Message>>() {}.getType();
        messages = gson.fromJson(json, type);
        return messages;
    }

    public static void writeMessagesToSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(CHAT, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(messages);
        editor.putString(MESSAGES, json);
        editor.commit();
    }

    /* return text method if specified by user
     * i.e whatsapp always?
     *    message always! */
    public static String readTextMethodFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXPECTED_RESPONSE,
                                                                        context.MODE_PRIVATE);
        return sharedPreferences.getString(TEXTMETHOD, null);
    }

    public static void writeTextMethodToSharedPreferences(Context context, String textMethod) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXPECTED_RESPONSE,
                context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TEXTMETHOD, textMethod);
        editor.commit();
    }

    public void displayMessages() {
        ListAdapter adapter = new ListAdapter(this, messages);
        ListView listView = (ListView) findViewById(R.id.message_list);
        listView.setAdapter(adapter);
        listView.setSelection(adapter.getCount()-1);

        /* set the status bar icon to black */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View v = (View) findViewById(R.id.inputText);
            v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    public void handleMessage(View v) {
        EditText editText = (EditText) findViewById(R.id.inputText);

        /* if user entered only spaces plainly return */
        if (editText.getText().toString().trim().equals("")) return;

        messages.add(new Message(true, editText.getText().toString().trim()));
        String action = readActionFromSharedPreferences(this);
        if (action != null) {
            /* some class is expecting action, so this message should be delivered to that class */
            handleAction(action, editText.getText().toString().trim(), this);
        } else {
            /* decide new action as no class is expecting action */
            ActionDecider.performAction(this, editText.getText().toString());
        }
        displayMessages();
        editText.setText("");
        writeMessagesToSharedPreferences(this);
    }

    public static void handleAction(String action, String message, Context context) {
        switch (action) {
            case "call":
                //call action handler for call
                Call.actionHandler(context, message);
                break;
            case "text":
                //call action handler for text
                Text.actionHandler(context, message);
            case "reminder":
                //call action handler for reminder
                break;
            default:
                /* should never come here */
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        if(activity.getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity
                    .getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus()
                    .getWindowToken(), 0);
        }
    }

    public static class Permissions {
        private static final String[] PERMISSIONS_CONTACT = {Manifest.permission.READ_CONTACTS};
        private static final String[] PERMISSION_CALLS = {Manifest.permission.CALL_PHONE};
        private static final String[] PERMISSION_SMS = {Manifest.permission.SEND_SMS};
        private static final int REQUEST_CONTACTS_READ = 1;
        private static final int MAKE_PHONE_CALLS = 2;
        private static final int SEND_SMS = 3;

        public static final String PERMISSION_TAG = "permissions_tag";

        public static void requestContactsReadPermission(final Activity activity) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.READ_CONTACTS)) {
                View v = activity.findViewById(R.id.edit_bar);
                /* hide the keyboard before displaying snackbar, otherwise it will be covered by
                 * the keyboard */
                hideSoftKeyboard(activity);
                Snackbar.make(v, R.string.permission_contacts_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat
                                        .requestPermissions(activity, PERMISSIONS_CONTACT,
                                                REQUEST_CONTACTS_READ);
                            }
                        })
                        .show();
            } else {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_CONTACT,
                        REQUEST_CONTACTS_READ);
            }
        }

        public static void requestSendSMSPermission(final Activity activity) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.SEND_SMS)) {
                View v = activity.findViewById(R.id.edit_bar);
                /* hide the keyboard before displaying snackbar, otherwise it will be covered by
                 * the keyboard */
                hideSoftKeyboard(activity);
                Snackbar.make(v, R.string.send_sms,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat
                                        .requestPermissions(activity, PERMISSION_SMS,
                                                SEND_SMS);
                            }
                        })
                        .show();
            } else {
                ActivityCompat.requestPermissions(activity, PERMISSION_SMS, SEND_SMS);
            }
        }

        public static void requestMakeCallPermission(final Activity activity) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.CALL_PHONE)) {
                View v = activity.findViewById(R.id.edit_bar);
                /* hide the keyboard before displaying snackbar, otherwise it will be covered by
                 * the keyboard */
                hideSoftKeyboard(activity);
                Snackbar.make(v, R.string.make_calls,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat
                                        .requestPermissions(activity, PERMISSION_CALLS,
                                                MAKE_PHONE_CALLS);
                            }
                        })
                        .show();
            } else {
                ActivityCompat.requestPermissions(activity, PERMISSION_CALLS, MAKE_PHONE_CALLS);
            }
        }

        public static void checkAllPermissions(Activity activity) {
            /* check Contacts Read Permission */
            checkContactReadPermission(activity);
            checkMakeCallPermission(activity);
            checkSendSMSPermission(activity);
        }

        public static void checkContactReadPermission(Activity activity) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Read contacts permissions have not been granted.
                Log.i(PERMISSION_TAG, "Contact read permissions has NOT been granted. Requesting" +
                        " permissions.");
                requestContactsReadPermission(activity);
            } else {
                // Contact permissions have been granted.
                Log.i(PERMISSION_TAG,
                        "Contact read permissions have already been granted.");
            }
        }

        public static void checkMakeCallPermission(Activity activity) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Make calls permissions have not been granted.
                Log.i(PERMISSION_TAG, "Make call permission has not been granted. Requesting" +
                        " permissions.");
                requestMakeCallPermission(activity);
            } else {
                // Make calls permission has been granted.
                Log.i(PERMISSION_TAG, "Make call permission have already been granted.");
            }
        }

        public static void checkSendSMSPermission(Activity activity) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Make calls permissions have not been granted.
                Log.i(PERMISSION_TAG, "Send SMS permission has not been granted. Requesting" +
                        " permissions.");
                requestSendSMSPermission(activity);
            } else {
                // Make calls permission has been granted.
                Log.i(PERMISSION_TAG, "Send SMS permission have already been granted.");
            }
        }
    }
}
