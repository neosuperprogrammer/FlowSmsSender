package net.flowgrammer.flowsmssender;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import net.flowgrammer.flowsmssender.util.Const;
import net.flowgrammer.flowsmssender.util.Setting;
import net.flowgrammer.flowsmssender.util.SslAsyncHttpClient;
import net.flowgrammer.flowsmssender.util.Util;

import org.apache.http.Header;
import org.json.JSONObject;

/**
 * Created by neox on 5/15/16.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName() ;

    ProgressDialog mDialog;

    //    Setting mSetting;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

//        mSetting = new Setting(this);

        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Loading...");
        mDialog.setCancelable(true);

        Button loginButton = (Button)findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login() {
        mDialog.show();

        AsyncHttpClient client = new SslAsyncHttpClient();
        client.addHeader("Cookie", "connect.sid=" + Setting.cookie(getApplicationContext()));
        client.addHeader("Accept", "application/json");

        RequestParams params = new RequestParams();

        EditText username = (EditText)findViewById(R.id.username_text);
        EditText password = (EditText)findViewById(R.id.password_text);
        params.put("username", username.getText().toString());
        params.put("password", password.getText().toString());
        Log.e(LOG_TAG, "login");
        client.post(Const.QUERY_URL + "/login", params,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                mDialog.dismiss();
                Util.saveCookie(getApplicationContext(), headers);
                Log.d("TEST", "success");
                Log.d("TEST", response.toString());
                String result = response.optString("result");
//                String sessionId = response.optString("session_id");
//                mSetting.setAuthKey(sessionId);
                if (result.equalsIgnoreCase("success")) {
//                    Toast.makeText(getApplicationContext(), "success", Toast.LENGTH_LONG).show();
//                    finishActivity(0);
                    setResult(Activity.RESULT_OK);
                    finish();
                }
                else {
                    String message = response.optString("message");
                    if (message.length() < 1) {
                        message = "Login failed";
                    }
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }
//                        Log.d("TEST", response.toString());
//                        super.onSuccess(response);
            }

            @Override
            public void onFailure(int statusCode, Throwable e, JSONObject errorResponse) {
                mDialog.dismiss();
                super.onFailure(statusCode, e, errorResponse);
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}
