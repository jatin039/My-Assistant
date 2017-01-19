package app.developer.jtsingla.myassistant.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import app.developer.jtsingla.myassistant.R;
import app.developer.jtsingla.myassistant.Utils.Message;

/**
 * Created by jssingla on 1/15/17.
 */

public class ListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private ArrayList<Message> objects;

    private class ViewHolder {
        TextView textView1, textView2;
    }

    public ListAdapter(Context context, ArrayList<Message> objects) {
        inflater = LayoutInflater.from(context);
        this.objects = objects;
    }

    public int getCount() {
        return objects.size();
    }

    public Message getItem(int position) {
        return objects.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        holder = new ViewHolder();
        convertView = inflater.inflate(R.layout.listview, null);
        if (objects.get(position).isRight()) {
            holder.textView1 = (TextView) convertView.findViewById(R.id.right_label);
            holder.textView2 = (TextView) convertView.findViewById(R.id.left_label);
        } else {
            holder.textView1 = (TextView) convertView.findViewById(R.id.left_label);
            holder.textView2 = (TextView) convertView.findViewById(R.id.right_label);
        }
        convertView.setTag(holder);
        holder.textView1.setText(objects.get(position).getMessageText());
        holder.textView2.setVisibility(View.GONE);
        return convertView;
    }
}
