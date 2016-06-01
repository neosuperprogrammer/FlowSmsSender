package net.flowgrammer.flowsmssender.jobs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import net.flowgrammer.flowsmssender.LoginActivity;
import net.flowgrammer.flowsmssender.MainActivity;
import net.flowgrammer.flowsmssender.R;
import net.flowgrammer.flowsmssender.jobdetail.JobDetailActivity;
import net.flowgrammer.flowsmssender.util.Const;
import net.flowgrammer.flowsmssender.util.MySSLSocketFactory;
import net.flowgrammer.flowsmssender.util.Setting;
import net.flowgrammer.flowsmssender.util.SslAsyncHttpClient;
import net.flowgrammer.flowsmssender.util.Util;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Created by neox on 5/15/16.
 */
public class JobsListActivity extends AppCompatActivity {

    private static final String LOG_TAG = JobsListActivity.class.getSimpleName();

    JobsAdapter mJobsAdapter;
    Integer mTotalPage = 1;
    Integer mTotalCount = 0;
    Integer mCurrentPage = 0;
    Integer mItemPerPage;

    ProgressDialog mDialog;
    ListView mlistView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobs);

        mlistView = (ListView)findViewById(R.id.listview);
        mJobsAdapter = new JobsAdapter(this, getLayoutInflater());
        mlistView.setAdapter(mJobsAdapter);
        mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                JSONObject jsonObject = (JSONObject) mJobsAdapter.getItem(position);
                String jobID = jsonObject.optString("_id","");
                Intent detailIntent = new Intent(JobsListActivity.this, JobDetailActivity.class);
                detailIntent.putExtra("jobID", jobID);
                startActivity(detailIntent);
            }
        });

        mlistView.setOnScrollListener(new EndlessScrollListener());
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Loading...");
        mDialog.setCancelable(true);

        loadJobsList();
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
            mTotalPage = 1;
            mTotalCount = 0;
            mCurrentPage = 0;
            mJobsAdapter.clear();

//            mJobsAdapter.clear();
            loadJobsList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadJobsList() {

        if (mCurrentPage >= mTotalPage) {
            Log.e(LOG_TAG, "no more page to return, return!!!!");
            return;
        }

//        mCurrentPage++;
        int requestIndex = mCurrentPage + 1;

        mDialog.show();

        AsyncHttpClient client = new SslAsyncHttpClient();
        client.addHeader("Cookie", "connect.sid=" + Setting.cookie(getApplicationContext()));
        client.addHeader("Accept", "application/json");

        RequestParams params = new RequestParams();
//        params.put("session", mSetting.authKey());

        String requestUrl = Const.QUERY_URL + "/jobs/page/" + requestIndex;

        Log.e(LOG_TAG, requestUrl);

        client.get(requestUrl, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                mDialog.dismiss();
                Util.saveCookie(getApplicationContext(), headers);
                Log.d(LOG_TAG, response.toString());
                String result = response.optString("result");
                if (!result.equalsIgnoreCase("success")) {
                    String message = response.optString("message");
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(JobsListActivity.this, LoginActivity.class);
                    startActivityForResult(intent, 1);
                    return;
                }
                final JSONArray list = response.optJSONArray("jobs");
                mTotalCount = Integer.valueOf(response.optString("totalJobCount"));
                mCurrentPage = Integer.valueOf(response.optString("currentPage"));
                mItemPerPage = Integer.valueOf(response.optString("itemPerPage"));
                mTotalPage = mTotalCount / mItemPerPage + ((mTotalCount % mItemPerPage) > 0 ? 1 : 0);
                Log.i(LOG_TAG, "totalCount : " + mTotalCount
                        + ", currentPage : " + mCurrentPage
                        + ", itemPerPage ; " + mItemPerPage
                        + ", totalPage ; " + mTotalPage
                );
//                Log.d(LOG_TAG, list.toString());
//                if (result.equalsIgnoreCase("success")) {
//                    Toast.makeText(getApplicationContext(), "success", Toast.LENGTH_LONG).show();
//                }
                mlistView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mJobsAdapter.updateData(list);
                    }
                }, 300);
            }

            @Override
            public void onFailure(int statusCode, Throwable e, JSONObject errorResponse) {
                mDialog.dismiss();
//                super.onFailure(statusCode, e, errorResponse);
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                finish();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                loadJobsList();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
            }
        }

    }

    private class EndlessScrollListener implements AbsListView.OnScrollListener {

        private int visibleThreshold = 5;
        private int currentPage = 0;
        private int previousTotal = 0;
        private boolean loading = true;

        public EndlessScrollListener() {
        }
        public EndlessScrollListener(int visibleThreshold) {
            this.visibleThreshold = visibleThreshold;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            Log.e(LOG_TAG, "onScroll, totalItemCount : " + totalItemCount + ", mTotalCount : " + mTotalCount);

            if (totalItemCount >= mTotalCount) {
                Log.e(LOG_TAG, "totalItemCount : " + totalItemCount + " is greater or equals to mTotalCount : " + mTotalCount);
                return;
            }

            boolean loadMore = /* maybe add a padding */
                    firstVisibleItem + visibleItemCount >= totalItemCount;

            Log.e(LOG_TAG, "loadMore : " + loadMore + ", isLoading : " + loading);

            if (loading) {
                loading = false;
                if (totalItemCount > previousTotal) {
                    previousTotal = totalItemCount;
                }
            }
            if (!loading && loadMore) {
                loadJobsList();
                loading = true;
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }
}
