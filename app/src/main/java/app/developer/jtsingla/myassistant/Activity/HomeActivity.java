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
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import android.Manifest;

import app.developer.jtsingla.myassistant.Decider.ActionDecider;
import app.developer.jtsingla.myassistant.Utils.ListAdapter;
import app.developer.jtsingla.myassistant.Utils.Message;
import app.developer.jtsingla.myassistant.R;

public class HomeActivity extends AppCompatActivity {

    public static ArrayList<Message> messages;
    public final static String CHAT = "chat";
    public final static String MESSAGES = "messages";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        messages = readFromSharedPreferences(this);
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

    public static ArrayList<Message> readFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(CHAT, context.MODE_PRIVATE);
        String json = sharedPreferences.getString(MESSAGES, null);
        if (json == null) {
            return (messages = new ArrayList<Message>());
        }
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Message>>() {}.getType();
        messages = gson.fromJson(json, type);
        return messages;
    }

    public static void writeToSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(CHAT, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(messages);
        editor.putString(MESSAGES, json);
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

        messages.add(new Message(true, editText.getText().toString()));
        ActionDecider.performAction(this, editText.getText().toString());
        displayMessages();
        editText.setText("");
        writeToSharedPreferences(this);
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
        private static final String[] PERMISSIONS_CONTACT = {Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS};
        private static final int REQUEST_CONTACTS_READ = 1;

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
                ActivityCompat.requestPermissions(activity, PERMISSIONS_CONTACT, REQUEST_CONTACTS_READ);
            }
        }

        public static void checkAllPermissions(Activity activity) {
            /* check Contacts Read Permission */
            checkContactReadPermission(activity);
        }

        public static void checkContactReadPermission(Activity activity) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Read contacts permissions have not been granted.
                Log.i(PERMISSION_TAG, "Contact read permissions has NOT been granted. Requesting permissions.");
                requestContactsReadPermission(activity);
            } else {
                // Contact permissions have been granted.
                Log.i(PERMISSION_TAG,
                        "Contact read permissions have already been granted.");
            }
        }
    }
}
