package org.mediawiki.auth;

import java.io.IOException;

import net.translatewiki.app.R;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_USERNAME = "org.mediawiki.auth.login.username";

    private MWApiApplication app;

    Button loginButton;
    EditText usernameEdit;
    EditText passwordEdit;

    private class LoginTask extends AsyncTask<String, String, String> {

        Activity context;
        ProgressDialog dialog;
        String username;
        String password;

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("Commons", "Login done!");
            if (result.equals("Success")) {
                dialog.dismiss();
                Toast successToast = Toast.makeText(context, R.string.login_success, Toast.LENGTH_SHORT);
                successToast.show();
                Account account = new Account(username, getString(R.string.account_type_identifier));
                boolean accountCreated = AccountManager.get(context).addAccountExplicitly(account, password, null);

                Bundle extras = context.getIntent().getExtras();
                if (extras != null) {
                    if (accountCreated) { // Pass the new account back to the account manager
                        AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                        Bundle authResult = new Bundle();
                        authResult.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                        authResult.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type_identifier));
                        response.onResult(authResult);
                    }
                }
                context.finish();
            } else {
                Toast failureToast = Toast.makeText(context, R.string.login_failed, Toast.LENGTH_LONG);
                dialog.dismiss();
                failureToast.show();
            }

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(context);
            dialog.setIndeterminate(true);
            dialog.setTitle(getString(R.string.logging_in_title));
            dialog.setMessage(getString(R.string.logging_in_message));
            dialog.show();
        }

        LoginTask(Activity context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            username = params[0];
            password = params[1];
            try {
                return app.getApi().login(username, password);
            } catch (IOException e) {
                // Do something better!
                return "Failure";
            }
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ((MWApiApplication)this.getApplicationContext());
        setContentView(R.layout.activity_login);
        loginButton = (Button) findViewById(R.id.loginButton);
        usernameEdit = (EditText) findViewById(R.id.loginUsername);
        passwordEdit = (EditText) findViewById(R.id.loginPassword);
        final Activity that = this;
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEdit.getText().toString();
                // Because Mediawiki is upercase-first-char-then-case-sensitive :)
                String canonicalUsername = username.substring(0,1).toUpperCase() + username.substring(1);

                String password = passwordEdit.getText().toString();
                
                Log.d("Commons", "Login to start!");
                LoginTask task = new LoginTask(that);
                 task.execute(canonicalUsername, password);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
