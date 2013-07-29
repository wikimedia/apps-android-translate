package net.translatewiki.app;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_USERNAME = "net.translatewiki.app.login.username";
    final Activity context = this; //do not remove
    Button loginButton;
    EditText usernameEdit;
    EditText passwordEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.preference_file_key),context.MODE_PRIVATE);
        String savedUser = sharedPref.getString(getString(R.string.saved_username),"");
        String savedPass = sharedPref.getString(getString(R.string.saved_password),"");

        TWApi api = TWApi.getInstance();
        if (savedUser.length()>0 && savedPass.length()>0)
        {
            api.login(savedUser, savedPass,new IFunction() {
                @Override
                public void execute(String result) {

                    //show result from login request on the textView

                    if( result.equals("Success")){
                        // move to translation activity
                        Intent intent =  new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        LoginActivity.this.finish(); //close the login activity so the user wont be able to go back
                    }
                    else {
                        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.preference_file_key),context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(getString(R.string.saved_username),"");
                        editor.putString(getString(R.string.saved_password),"");
                        editor.commit();

                        prepareLayout();
                    }
                }
            });
        }
        else{
            prepareLayout();
        }
    }

    private void prepareLayout(){

        setContentView(R.layout.activity_login);

        // make login button enable when input suffice
        loginButton = (Button) findViewById(R.id.button);
        usernameEdit = (EditText) findViewById(R.id.usernameText);
        passwordEdit = (EditText) findViewById(R.id.passwordText);

        TextWatcher loginEnabler = new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }

            public void onTextChanged(CharSequence charSequence, int start, int count, int after) { }

            public void afterTextChanged(Editable editable) {
                if(usernameEdit.getText().length() != 0 && passwordEdit.getText().length() != 0) {
                    loginButton.setEnabled(true);
                } else {
                    loginButton.setEnabled(false);
                }
            }
        };

        usernameEdit.addTextChangedListener(loginEnabler);
        passwordEdit.addTextChangedListener(loginEnabler);

        // make login on press <return>
        passwordEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (loginButton.isEnabled() && (actionId == EditorInfo.IME_ACTION_DONE || (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ) {
                    clickLogin(null);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    public void clickLogin(View view){

        TWApi api = TWApi.getInstance(); //get api singleton

        TextView resTv = (TextView) findViewById(R.id.loginResultTextView);
        resTv.setText("process...");

        // read credentials
        EditText usernamEt = (EditText) findViewById(R.id.usernameText);
        EditText passEt = (EditText) findViewById(R.id.passwordText);
        final String username = usernamEt.getText().toString();
        final String password = passEt.getText().toString();

        // make login request
        api.login(username, password,new IFunction() {
            @Override
            public void execute(String result) {

                //show result from login request on the textView
                TextView resTv = (TextView) findViewById(R.id.loginResultTextView);
                resTv.setText(result);

                if( result.equals("Success")){

                    //save credentials
                    SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.preference_file_key),context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.saved_username),username);
                    editor.putString(getString(R.string.saved_password),password);
                    editor.commit();

                    /*
                    final Account account = new Account(username, "net.translatewiki.app");
                    boolean accountCreated = AccountManager.get(context).addAccountExplicitly(account, password, null);

                    Bundle extras = context.getIntent().getExtras();
                    if (extras != null && accountCreated) {
                         // Pass the new account back to the account manager
                            AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                            Bundle authResult = new Bundle();
                            authResult.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                            authResult.putString(AccountManager.KEY_ACCOUNT_TYPE, "net.translatewiki.app");
                            response.onResult(authResult);

                    }*/

                    // move to translation activity
                    Intent intent =  new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    LoginActivity.this.finish(); //close the login activity so the user wont be able to go back
                }
            }
        });

    }
}
