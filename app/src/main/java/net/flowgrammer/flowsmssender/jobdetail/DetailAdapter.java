package net.flowgrammer.flowsmssender.jobdetail;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.flowgrammer.flowsmssender.R;
import net.flowgrammer.flowsmssender.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by neox on 5/17/16.
 */
public class DetailAdapter extends BaseAdapter {

    private static final String LOG_TAG = DetailAdapter.class.getSimpleName();

    Context mContext;
    LayoutInflater mInflater;
    JSONArray mJsonArray;
    int selectedItem = -1;

    public DetailAdapter(Context context, LayoutInflater inflater) {
        mContext = context;
        mInflater = inflater;
        mJsonArray = new JSONArray();
    }

    public void updateData(JSONArray jsonArray) {
        mJsonArray = jsonArray;
        notifyDataSetChanged();
    }

    public void updateStatus(int position, Integer status) {
        JSONObject sms = (JSONObject)getItem(position);
        try {
            sms.put("status", status);
            mJsonArray.put(position, sms);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
        }
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
            convertView = mInflater.inflate(R.layout.row_job_detail, null);

            // create a new "Holder" with subviews
            holder = new ViewHolder();
            holder.nameTextView = (TextView) convertView.findViewById(R.id.text_name);
            holder.phonenumberTextView = (TextView) convertView.findViewById(R.id.text_phonenumber);
            holder.stateTextView = (TextView) convertView.findViewById(R.id.text_state);

            // hang onto this holder for future recyclage
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (selectedItem == position) {
            convertView.setBackgroundColor(Color.GRAY);
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        JSONObject jsonObject = (JSONObject) getItem(position);

        String name = jsonObject.optString("name");
        String phonenumber = jsonObject.optString("phonenumber");
        Integer status = jsonObject.optInt("status");
//        Log.e(LOG_TAG, "status : " + status);
        String statusString = "";
        if (status > 0) {
            statusString = "success";
        }
        else if (status < 0) {
            statusString = "fail";
        }

        holder.nameTextView.setText(name);
        holder.phonenumberTextView.setText(phonenumber);
        holder.stateTextView.setText(statusString);

        convertView.setMinimumHeight((int) Util.convertDpToPixel(45, mContext));
        return convertView;
    }

    public void setSelectedItem(int seq) {
        selectedItem = seq;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        public TextView nameTextView;
        public TextView phonenumberTextView;
        public TextView stateTextView;
    }
}
