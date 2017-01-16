package app.developer.jtsingla.myassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
}
