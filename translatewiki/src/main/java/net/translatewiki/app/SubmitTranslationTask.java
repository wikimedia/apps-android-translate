package net.translatewiki.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.mediawiki.api.ApiResult;
import org.mediawiki.auth.Utils;

import java.io.IOException;

/**
 * Handles the task of contributing new translation (edit) for a message.
 */
public class SubmitTranslationTask extends AsyncTask<Void, Void, String> {

    private Activity context;
    private MessageAdapter message;

    // CTOR
    public SubmitTranslationTask(Activity context, MessageAdapter message) {
        this.context = context;
        this.message = message;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        message.setCommitted(true);
        Toast successToast = Toast.makeText(context,"committing "+message.getRevision(), Toast.LENGTH_SHORT);
        successToast.show();
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            TranslateWikiApp app = (TranslateWikiApp)((MainActivity)context).getApp();
            if(!app.getApi().validateLogin()) {
                if(((TranslateWikiApp)app).revalidateAuthToken()) {
                    // Validated!
                    Log.d("TWN", "VALIDATED!");
                } else {
                    Log.d("TWN", "Invalid :(");
                    throw new RuntimeException();
                }
            }
            if(MainActivity.translateToken == null || MainActivity.translateToken.length()==0)
            {
                ApiResult tokenResult;
                tokenResult = app.getApi().action("tokens").param("type", "edit").post();
                Log.d("TWN", "First result is " + Utils.getStringFromDOM(tokenResult.getDocument()));
                MainActivity.translateToken = tokenResult.getString("/api/tokens/@edittoken");
                Log.d("TWN", "Token is " + MainActivity.translateToken);

                if (MainActivity.translateToken==null || MainActivity.translateToken.length()==0)
                {
                    String warning = tokenResult.getString("/api/warnings/tokens");
                    warning = ((warning==null || warning.length()==0 ) ? "no token!" : warning);
                    return warning;
                }

            }


            // send API request for review message
            ApiResult editResult = app.getApi().action("edit")
                    .param("title", message.getmTitle())
                    .param("text", message.savedInput)
                    .param("token", MainActivity.translateToken).post();
            Log.d("TWN", Utils.getStringFromDOM(editResult.getDocument()));

            String error = editResult.getString("/api/error/@info");
            if (error!=null && error.length()>0)
                return error;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String warning) {
        super.onPostExecute(warning);
        if (warning!=null)
        {
            Toast warningToast = Toast.makeText(context, warning , Toast.LENGTH_LONG);
            warningToast.show();
        }
        else
        {
            // TODO: check indeed successful, we are lying meanwhile...
            Toast successToast = Toast.makeText(context, "committed!", Toast.LENGTH_SHORT);
            successToast.show();
            ((MainActivity)context).notifyTranslationsOnNewThread();
        }
    }
}

