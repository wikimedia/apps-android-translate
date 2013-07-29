package net.translatewiki.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;

import java.io.IOException;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onClick(View v){
        TWApi api = TWApi.getInstance(); //get api singleton

        //delete saved credentials
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key),this.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.saved_username),"");
        editor.putString(getString(R.string.saved_password),"");
        editor.commit();

        // make logout request
        api.logout(new IFunction() {
            @Override
            public void execute(String result) {
                // move to login activity
                Intent intent =  new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                MainActivity.this.finish(); //close the  activity so the user wont be able to go back
            }
        });


    }
}
