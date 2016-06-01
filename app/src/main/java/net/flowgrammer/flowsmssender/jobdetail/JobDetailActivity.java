package net.flowgrammer.flowsmssender.jobdetail;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import net.flowgrammer.flowsmssender.LoginActivity;
import net.flowgrammer.flowsmssender.R;
import net.flowgrammer.flowsmssender.jobs.JobsAdapter;
import net.flowgrammer.flowsmssender.service.SmsIntentService;
import net.flowgrammer.flowsmssender.util.Const;
import net.flowgrammer.flowsmssender.util.Setting;
import net.flowgrammer.flowsmssender.util.SslAsyncHttpClient;
import net.flowgrammer.flowsmssender.util.Util;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;

/**
 * Created by neox on 5/17/16.
 */
public class JobDetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = JobDetailActivity.class.getSimpleName();
//    public static final String ACTION_SMS_SENT = "net.flowgrammer.flowsmssender.SMS_SENT_ACTION";

    public static final String ACTION_SMS_SENT = "net.flowgrammer.flowsmssender.SMS_SENT_ACTION";
    public static final String ACTION_SMS_DELIVERED = "net.flowgrammer.flowsmssender.SMS_DELIVERED_ACTION";

    BroadcastReceiver mReceiver;
    BroadcastReceiver mDeliveredReceiver;

    Button mSendButton;
    ListView mListView;
    DetailAdapter mDetailAdapter;
    Integer mTotalPage;
    Integer mCurrentPage;
    Integer mItemPerPage;
    String mJobID;

    ProgressDialog mDialog;
    Boolean isSendingSms;

    JSONArray mSmsList;
    Integer mJobIndex;

//    BroadcastReceiver mReceiver;

    Intent mSmsIntent;
    ResponseReceiver mResponseReceiver;

    private final Object lock = new Object();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        mCurrentPage = 1;
        isSendingSms = false;

        mJobID = getIntent().getStringExtra("jobID");
        Log.i(LOG_TAG, "job id : " + mJobID);

        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Loading...");
        mDialog.setCancelable(true);

        EditText editContent = (EditText)findViewById(R.id.edit_content);

        mListView = (ListView)findViewById(R.id.listview);
        mDetailAdapter = new DetailAdapter(this, getLayoutInflater());
        mListView.setAdapter(mDetailAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                JSONObject jsonObject = (JSONObject) mDetailAdapter.getItem(position);
                String smsID = jsonObject.optString("_id","");
//                updateSmsStatus(position, smsID, "success");
            }
        });

        mSendButton = (Button)findViewById(R.id.btn_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSendingSms) {
                    mListView.post(new Runnable() {
                        @Override
                        public void run() {
                            hideSoftKeyboard();
                        }
                    });
                    startSmsSending();
                } else {
                    stopSmsSending();
                }
            }
        });

        IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mResponseReceiver = new ResponseReceiver();
        registerReceiver(mResponseReceiver, filter);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = "unknown";
                Integer total = intent.getIntExtra("total", '1');
                Integer seq = intent.getIntExtra("seq", -1);
                Log.e(LOG_TAG, "broadcast receive, total : " + total + ", seq : " + seq);
                Log.e(LOG_TAG, "broadcast receive, current seq : " + mJobIndex);
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("net.flowgrammer.intent.action.MESSAGE_PROCESSED");
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        message = "Message sent!";
                        Log.e(LOG_TAG, "send sms success");
                        if (seq == total) {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            broadcastIntent.putExtra("result", "success");
                            broadcastIntent.putExtra("message", "success");
                            broadcastIntent.putExtra("seq", mJobIndex);
                            getBaseContext().sendBroadcast(broadcastIntent);
                        }

                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        message = "Error.";
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        message = "Error: No service.";
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        message = "Error: Null PDU.";
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        message = "Error: Radio off.";
                    default:
                        Log.e(LOG_TAG, "send sms fail, reason : " + message);

                        broadcastIntent.putExtra("result", "fail");
                        broadcastIntent.putExtra("message", message);
                        broadcastIntent.putExtra("seq", mJobIndex);
                        getBaseContext().sendBroadcast(broadcastIntent);
                        break;
                }
            }
        };
        registerReceiver(mReceiver, new IntentFilter(ACTION_SMS_SENT));


//        mDeliveredReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent broadcastIntent) {
//                switch (getResultCode()) {
//
//                    case Activity.RESULT_OK: {
//                        String message = "Message sent!";
////                        Toast.makeText(getBaseContext(), "SMS delivered",
////                                Toast.LENGTH_SHORT).show();
//                        broadcastIntent.putExtra("result", "success");
//                        broadcastIntent.putExtra("message", message);
//                        broadcastIntent.putExtra("seq", mCurrentSeq);
//                        getBaseContext().sendBroadcast(broadcastIntent);
//                        break;
//                    }
//                    case Activity.RESULT_CANCELED: {
//                        Log.e(LOG_TAG, "delivered intent : canceled");
//                        String message = "Message canceled!";
////                        Toast.makeText(getBaseContext(), "SMS not delivered",
////                                Toast.LENGTH_SHORT).show();
//                        broadcastIntent.putExtra("result", "fail");
//                        broadcastIntent.putExtra("message", message);
//                        broadcastIntent.putExtra("seq", mCurrentSeq);
//                        getBaseContext().sendBroadcast(broadcastIntent);
//                        break;
//                    }
//                }
//            }
//        };
//        registerReceiver(mDeliveredReceiver, new IntentFilter(ACTION_SMS_DELIVERED));

        loadJobDetail();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_reload) {
            loadJobDetail();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Hides the soft keyboard
     */
    public void hideSoftKeyboard() {
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Shows the soft keyboard
     */
    public void showSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        view.requestFocus();
        inputMethodManager.showSoftInput(view, 0);
    }

    public class ResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "net.flowgrammer.intent.action.MESSAGE_PROCESSED";

        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("result");
            String message = intent.getStringExtra("message");
            int seq = intent.getIntExtra("seq", -1);

            String status = "fail";
            if (result.equalsIgnoreCase("success")) {
                status = "success";
            }

            Log.e(LOG_TAG, "ResponseReceiver onReceive : " +  result + ", seq : " + seq);

            if (isSendingSms) {
                JSONObject jsonObject = (JSONObject) mDetailAdapter.getItem(seq);
                String smsID = jsonObject.optString("_id","");
                updateSmsStatus(seq, mJobID, smsID, status);
                int count = mDetailAdapter.getCount();
                int scrollPosition = seq + 3;
                if (scrollPosition >= count) {
                    scrollPosition = count - 1;
                }
                mListView.smoothScrollToPosition(scrollPosition);
            }
        }
    }

    private void updateSmsStatus(final int position, String jobID, String smsID, final String status) {
//        mDialog.show();
        AsyncHttpClient client = new SslAsyncHttpClient();
        client.addHeader("Cookie", "connect.sid=" + Setting.cookie(getApplicationContext()));
        client.addHeader("Accept", "application/json");

        RequestParams params = new RequestParams();
        params.put("status", status);

        String requestUrl = Const.QUERY_URL + "/jobs/" + jobID + "/smslist/" + smsID +"?_method=PUT";

        Log.e(LOG_TAG, "request url : " + requestUrl);

        client.post(requestUrl, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                mDialog.dismiss();
                Util.saveCookie(getApplicationContext(), headers);
                Log.d(LOG_TAG, response.toString());
                String result = response.optString("result");
                if (!result.equalsIgnoreCase("success")) {
                    String message = response.optString("message");
//                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                } else {
                    if (status.equalsIgnoreCase("success")) {
                        mDetailAdapter.updateStatus(position, 1);
                    } else {
                        mDetailAdapter.updateStatus(position, -1);
                    }
                }
                if (isSendingSms) {
                    synchronized (lock) {
                        mJobIndex++;
                    }
                    sendNextSms();
                }
            }

            @Override
            public void onFailure(int statusCode, Throwable e, JSONObject errorResponse) {
//                mDialog.dismiss();
                super.onFailure(statusCode, e, errorResponse);
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                if (isSendingSms) {
                    synchronized (lock) {
                        mJobIndex++;
                    }
                    sendNextSms();
                }

            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        if (mDeliveredReceiver != null) {
            unregisterReceiver(mDeliveredReceiver);
        }

        if (mSmsIntent != null) {
            stopService(mSmsIntent);
        }

        if (mResponseReceiver != null) {
            unregisterReceiver(mResponseReceiver);
        }

        super.onDestroy();
    }

    private void stopSmsSending() {
        synchronized (lock) {
            isSendingSms = false;
        }
        mSendButton.setText("Start");
    }

    private void sendNextSms() {
        JSONObject job = getNextSmsJob();
        if (job == null) {
            synchronized (lock) {
                isSendingSms = false;
                mSendButton.setText("Start");
                mDetailAdapter.setSelectedItem(-1);
                mListView.smoothScrollToPosition(0);
                Toast.makeText(getApplicationContext(), "Sms Sending End!!!", Toast.LENGTH_LONG).show();
            }
            return;
        }
        mDetailAdapter.setSelectedItem(mJobIndex);

        String name = job.optString("name");
        String phonenumber = job.optString("phonenumber");
        EditText editContent = (EditText)findViewById(R.id.edit_content);
        String content = editContent.getText().toString();

        mSmsIntent = new Intent(this, SmsIntentService.class);
        mSmsIntent.putExtra("name", name);
        mSmsIntent.putExtra("phonenumber", phonenumber);
        mSmsIntent.putExtra("content", content);
        mSmsIntent.putExtra("seq", mJobIndex);
        startService(mSmsIntent);
    }

    private void startSmsSending() {
        synchronized (lock) {
            mJobIndex = 0;
            isSendingSms = true;
        }
        mSendButton.setText("Stop");
        sendNextSms();
    }

    private synchronized JSONObject getNextSmsJob() {
        Integer count = mSmsList.length();
        for (int i = mJobIndex; i < count; i++) {
            JSONObject job = mSmsList.optJSONObject(i);
            Integer status = job.optInt("status");
            if (status > 0) {
                continue;
            }
            synchronized (lock) {
                mJobIndex = i;
            }
            return job;
        }
        return null;
    }

    private void loadJobDetail() {

        mDialog.show();

        AsyncHttpClient client = new SslAsyncHttpClient();
        client.addHeader("Cookie", "connect.sid=" + Setting.cookie(getApplicationContext()));
        client.addHeader("Accept", "application/json");

        RequestParams params = new RequestParams();
//        params.put("session", mSetting.authKey());

        String requestUrl = Const.QUERY_URL + "/jobs/" + mJobID + "/page/" + mCurrentPage;

        Log.e(LOG_TAG, "request url : " + requestUrl);

        client.get(requestUrl, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                mDialog.dismiss();
                mListView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideSoftKeyboard();
                    }
                }, 300);

                Util.saveCookie(getApplicationContext(), headers);
                Log.d(LOG_TAG, response.toString());
                String result = response.optString("result");
                if (!result.equalsIgnoreCase("success")) {
                    String message = response.optString("message");
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(JobDetailActivity.this, LoginActivity.class);
                    startActivityForResult(intent, 1);
                    return;
                }
                JSONObject job = response.optJSONObject("job");
                mSmsList  = job.optJSONArray("smslist");
                JSONObject author = job.optJSONObject("author");
                String created = job.optString("created");
                String title = job.optString("name");
                String content = job.optString("description");

                TextView textTitle = (TextView)findViewById(R.id.text_title);
                textTitle.setText(title);
                EditText editContent = (EditText)findViewById(R.id.edit_content);
                editContent.setText(content);

                View view = JobDetailActivity.this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                Log.e(LOG_TAG, "author : " + author.toString()
                                + ", created : " + created
                                + ", title ; " + title);
                mDetailAdapter.updateData(mSmsList);
            }

            @Override
            public void onFailure(int statusCode, Throwable e, JSONObject errorResponse) {
                mDialog.dismiss();
                mListView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideSoftKeyboard();
                    }
                }, 300);

//                super.onFailure(statusCode, e, errorResponse);
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }

        });
    }

    private void updateSmsStatus(final int position, String smsID, String status) {

        mDialog.show();

        AsyncHttpClient client = new SslAsyncHttpClient();
        client.addHeader("Cookie", "connect.sid=" + Setting.cookie(getApplicationContext()));
        client.addHeader("Accept", "application/json");

        RequestParams params = new RequestParams();
        params.put("status", status);

        String requestUrl = Const.QUERY_URL + "/jobs/" + mJobID + "/smslist/" + smsID +"?_method=PUT";

        Log.e(LOG_TAG, "request url : " + requestUrl);

        client.post(requestUrl, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                mDialog.dismiss();
                Util.saveCookie(getApplicationContext(), headers);
                Log.d(LOG_TAG, response.toString());
                String result = response.optString("result");
                if (!result.equalsIgnoreCase("success")) {
                    String message = response.optString("message");
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    stopSmsSending();
                    return;
                }
                mDetailAdapter.updateStatus(position, 1);
            }

            @Override
            public void onFailure(int statusCode, Throwable e, JSONObject errorResponse) {
                mDialog.dismiss();
//                super.onFailure(statusCode, e, errorResponse);
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                stopSmsSending();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                loadJobDetail();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
            }
        }

    }
}
