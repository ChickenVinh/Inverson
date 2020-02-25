package com.mahc.custombottomsheet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import timber.log.Timber;

/**
 * A login screen that offers login via mUser/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    SharedPreferences sharedpreferences;
    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private boolean isLoggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sharedpreferences = getSharedPreferences("login", Context.MODE_PRIVATE);
        mEmailView = findViewById(R.id.email);
        mPasswordView = findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.email_login_form);
        mProgressView = findViewById(R.id.login_progress);
        mLoginFormView.setVisibility(View.VISIBLE);
        showProgress(false);
        TextView txtV = findViewById(R.id.txtVersion);
        txtV.setText(BuildConfig.VERSION_NAME);

        checkForUpdate(mEmailView);

        //if already logged in
        if (sharedpreferences.contains("user") && sharedpreferences.contains("pw")) {
            mLoginFormView.setVisibility(View.GONE);

            isLoggedIn = true;
            String email = sharedpreferences.getString("user", "");
            String password = sharedpreferences.getString("pw", "");
            mEmailView.setText(email);
            mPasswordView.setText(password);

            Intent suc = new Intent(LoginActivity.this, MainActivity.class);
            suc.putExtra("User", email);
            startActivity(suc);
            finish();//this one prevents u from going back to login screen
        }

        // Set up the login form.
        mEmailView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                mPasswordView.requestFocus();
                return true;
            }
        });

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mLoginButton = findViewById(R.id.email_sign_in_button);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });




        //DEV#####
        //mEmailView.setText("admin");
        //mPasswordView.setText("admin");
        //attemptLogin();
    }
    void startUpdate(String url){
        UpdateManager updateManager = new UpdateManager(getApplicationContext(), url);
        updateManager.enqueueDownload();
        showProgress(false);

    }
    /**
     * Sends the current Version to the server and
     * receives a URL of newer APK
     */
    public void checkForUpdate(View view){
        showProgress(true);

        String versionName = BuildConfig.VERSION_NAME;
        String url = getResources().getString(R.string.versionScript)
                + "?version=" + versionName;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(!response.trim().isEmpty()) {
                            startUpdate(response);
                        }
                        showProgress(false);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showProgress(false);
            }
        });
        RequestQueueSingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);

        //while(mProgressView.getVisibility() == View.VISIBLE);
    }
    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the mUser entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isUserValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            if(!isLoggedIn) {
                //Save Credentials
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("user", mEmailView.getText().toString());
                editor.putString("pw", mPasswordView.getText().toString());
                editor.apply();
            }
            // Show a progress spinner, and kick off a background task to
            // perform the mUser login attempt.
            showProgress(true);
            queryServer(email,password);
            //Request Permissions

        }
    }

    private void queryServer(final String qemail, String qpassword){
        //http://gastroconsultung-catering.com/testing/vinh/login.php?user=admin&password=admin
        String url = getResources().getString(R.string.server_login_url)
                                                                        +"?user="+qemail
                                                                        +"&password="+qpassword;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response.equals("0")){//login failed
                            mPasswordView.setError(getString(R.string.error_incorrect_password));
                            mPasswordView.requestFocus();
                            showProgress(false);
                        }else if(response.equals("-1")){
                            mPasswordView.setError(getString(R.string.error_login_fatal));
                            mPasswordView.requestFocus();
                            showProgress(false);
                        }else if(!response.trim().isEmpty()){//login success
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString("user", response);
                            editor.apply();

                            Intent suc = new Intent(LoginActivity.this, MainActivity.class);
                            suc.putExtra("User", response);
                            startActivity(suc);
                            finish();//this one prevents u from going back to login screen
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Timber.d(error);
                showProgress(false);
            }
        });
        RequestQueueSingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
    }

    private boolean isUserValid(String email) {
        //TODO: Replace this with your own logic
        return true;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    private void showProgress(final boolean show) {
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}