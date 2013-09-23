/*
 * @(#)TranslateWikiApp       1.0 15/9/2013
 *
 *  Copyright (c) 2013 Or Sagi.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package net.translatewiki.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import org.mediawiki.api.ApiResult;

import java.io.IOException;

/**
 * Handles the task of contributing new translation (edit) for a message.
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class SubmitTranslationTask extends AsyncTask<Void, Void, String> {

    private Activity context;
    private MessageAdapter message;

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
                    //Log.d("TWN", "VALIDATED!"); // DEBUG
                } else {
                    //Log.d("TWN", "Invalid :("); // DEBUG
                    throw new RuntimeException();
                }
            }
            if(!validToken()) {
                ApiResult tokenResult;
                tokenResult = app.getApi().action("tokens").param("type", "edit").post();
                //Log.d("TWN", "First result is " + Utils.getStringFromDOM(tokenResult.getDocument())); // DEBUG
                MainActivity.translateToken = tokenResult.getString("/api/tokens/@edittoken");
                //Log.d("TWN", "Token is " + MainActivity.translateToken); // DEBUG

                if (!validToken()) {
                    String warning = tokenResult.getString("/api/warnings/tokens");
                    warning = ((warning == null || warning.length() == 0 ) ? "no token!" : warning);
                    return warning;
                }
            }

            // send API request for review message
            ApiResult editResult = app.getApi().action("edit")
                    .param("title", message.getmTitle())
                    .param("text", message.getSavedInput())
                    .param("token", MainActivity.translateToken).post();
            //Log.d("TWN", Utils.getStringFromDOM(editResult.getDocument())); // DEBUG

            String error = editResult.getString("/api/error/@info");
            if (error != null && error.length() > 0) {
                return error;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String warning) {
        super.onPostExecute(warning);
        if (warning != null) {
            Toast warningToast = Toast.makeText(context, warning , Toast.LENGTH_LONG);
            warningToast.show();
        } else {
            Toast successToast = Toast.makeText(context, "committed!", Toast.LENGTH_SHORT);
            successToast.show();
            ((MainActivity)context).notifyTranslationsOnNewThread();
        }
    }

    /* verify token exist and not empty */
    private boolean validToken() {
        return MainActivity.translateToken != null && MainActivity.translateToken.length() > 0;
    }
}
