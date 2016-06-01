package net.flowgrammer.flowsmssender;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import net.flowgrammer.flowsmssender.jobs.JobsListActivity;
import net.flowgrammer.flowsmssender.util.Const;
import net.flowgrammer.flowsmssender.util.Setting;
import net.flowgrammer.flowsmssender.util.SslAsyncHttpClient;

import org.apache.http.Header;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    ProgressDialog mDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Loading...");
        mDialog.setCancelable(true);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        Button startButton = (Button)findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(this.getClass().toString(), "click");
                Intent intent = new Intent(MainActivity.this, JobsListActivity.class);
//                detailIntent.putExtra("coverID", coverID);
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            mDialog.show();
            AsyncHttpClient client = new SslAsyncHttpClient();
            client.addHeader("Cookie", "connect.sid=" + Setting.cookie(getApplicationContext()));
            client.addHeader("Accept", "application/json");

            RequestParams params = new RequestParams();
//        params.put("session", mSetting.authKey());

            client.get(Const.QUERY_URL + "/logout", new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    mDialog.dismiss();
                    Log.d(LOG_TAG, "logout");
                    Setting.setCookie(getApplicationContext(), "");
                    Toast.makeText(getApplicationContext(), "Logout Success", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(int statusCode, Throwable e, JSONObject errorResponse) {
                    mDialog.dismiss();
                    super.onFailure(statusCode, e, errorResponse);
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();

                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
