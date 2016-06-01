package net.flowgrammer.flowsmssender.jobs;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.flowgrammer.flowsmssender.R;
import net.flowgrammer.flowsmssender.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by neox on 5/17/16.
 */
public class JobsAdapter extends BaseAdapter {

    Context mContext;
    LayoutInflater mInflater;
    JSONArray mJsonArray;

    public JobsAdapter(Context context, LayoutInflater inflater) {
        mContext = context;
        mInflater = inflater;
        mJsonArray = new JSONArray();
    }

    public void clear() {
        mJsonArray = new JSONArray();
        notifyDataSetChanged();
    }

    public void updateData(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.optJSONObject(i);
            mJsonArray.put(object);
        }
//        mJsonArray.put(jsonArray);
//        mJsonArray = jsonArray;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mJsonArray.length();
    }

    @Override
    public Object getItem(int position) {
        return mJsonArray.optJSONObject(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        // check if the view already exists
        // if so, no need to inflate and findViewById again!
        if (convertView == null) {

            // Inflate the custom row layout from your XML.
            convertView = mInflater.inflate(R.layout.row_job, null);

            // create a new "Holder" with subviews
            holder = new ViewHolder();
            holder.titleTextView = (TextView) convertView.findViewById(R.id.text_title);
            holder.dateTextView = (TextView) convertView.findViewById(R.id.text_date);

            // hang onto this holder for future recyclage
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        JSONObject jsonObject = (JSONObject) getItem(position);

        String title = jsonObject.optString("name");
        String date = jsonObject.optString("created");

        holder.titleTextView.setText(title);
        holder.dateTextView.setText(date);

        convertView.setMinimumHeight((int) Util.convertDpToPixel(45, mContext));

        return convertView;
    }

    private static class ViewHolder {
        public TextView titleTextView;
        public TextView dateTextView;
    }
}
