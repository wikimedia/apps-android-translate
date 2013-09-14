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
 * Handles the task of reviewing (accepting) translated by others messages.
 */
public class ReviewTranslationTask extends AsyncTask<Void, Void, String> {

    private Activity context;
    private MessageAdapter message;

    // CTOR
    public ReviewTranslationTask(Activity context, MessageAdapter message) {
        this.context = context;
        this.message = message;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        MainActivity.translations.remove(message); //accepted message will no longer be showed
        Toast successToast = Toast.makeText(context,message.getKey()+" accepting...", Toast.LENGTH_SHORT);
        successToast.show();
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            TranslateWikiApp app = (TranslateWikiApp) ((MainActivity)context).getApp();
            if(!app.getApi().validateLogin()) {
                if(app.revalidateAuthToken()) {
                    // Validated!
                    // Log.d("TWN", "VALIDATED!"); // DEBUG
                } else {
                    // Log.d("TWN", "Invalid :("); // DEBUG
                    throw new RuntimeException();
                }
            }
            if(MainActivity.reviewToken == null || MainActivity.reviewToken.length()==0) {
                ApiResult tokenResult;
                tokenResult = app.getApi().action("tokens").param("type", "translationreview").post();
                //Log.d("TWN", "First result is " + Utils.getStringFromDOM(tokenResult.getDocument())); // DEBUG

                MainActivity.reviewToken = tokenResult.getString("/api/tokens/@translationreviewtoken");
                //Log.d("TWN", "Token is " + MainActivity.reviewToken); // DEBUG

                if (MainActivity.reviewToken==null ||MainActivity.reviewToken.length()==0)
                {
                    String warning = tokenResult.getString("/api/warnings/tokens");
                    warning = ((warning==null || warning.length()==0 ) ? "no token!" : warning);
                    return warning;

                }

            }

            // send API request for review message
            ApiResult reviewResult = app.getApi().action("translationreview")
                    .param("revision", message.getRevision())
                    .param("token", MainActivity.reviewToken).post();
            //Log.d("TWN", Utils.getStringFromDOM(reviewResult.getDocument())); // DEBUG
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String warning) {
        super.onPostExecute(warning);
        if (warning !=null)
        {
            Toast warningToast = Toast.makeText(context, warning , Toast.LENGTH_LONG);
            warningToast.show();
        }
        else
        {
            Toast successToast = Toast.makeText(context,message.getKey()+" accepted!", Toast.LENGTH_SHORT);
            successToast.show();
        }
    }
}