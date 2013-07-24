package com.translatewiki.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Log.d("DEBUG", "print1");



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        Log.d("DEBUG", "print2");
        return true;
    }

    public void onClick(View v){
        twapi api = twapi.getInstance();

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        TextView resTv = (TextView) findViewById(R.id.loginResultTextView);
        resTv.setText("process...");

        EditText usernamEt = (EditText) findViewById(R.id.usernameText);
        EditText passEt = (EditText) findViewById(R.id.passwordText);
        String username = usernamEt.getText().toString();
        String password = passEt.getText().toString();

        api.login(username, password, new IFunction() {
            @Override
            public void execute(String result) {
                TextView resTv = (TextView) findViewById(R.id.loginResultTextView);
                resTv.setText(result);
            }
        });
    }
    
}
