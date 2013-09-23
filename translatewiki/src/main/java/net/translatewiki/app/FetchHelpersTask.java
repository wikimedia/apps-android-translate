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

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Handles the task of getting helpers (suggestions and documentation).
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class FetchHelpersTask extends AsyncTask<Void, Void, String> {

    private MessageAdapter msg;
    private Activity context;

    public FetchHelpersTask(Activity context, MessageAdapter msg) {
        this.msg = msg;
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        MWApi api = ((MainActivity)context).getApp().getApi();
        ApiResult result;
        try { // send API request
            result = api.action("translationaids")
                    .param("title", msg.getmTitle())
                    .param("group", msg.getmGroup())              // project
                    .param("prop", "mt|ttmserver|documentation")  // info to get
                    .post();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        msg.clearSuggestions();
        int count = 0;

        // extracts suggestions
        ArrayList<ApiResult> packedSuggestions = result.getNodes("/api/helpers/mt/suggestion");
        //Log.d("TWN", "Actual result is" + Utils.getStringFromDOM(result.getDocument())); // DEBUG

        String s;
        // insert suggestions with not longer than "MAX_SUGGESTION_LENGTH",
        // (too long suggestion is empirically bad suggestion)
        // but no more than "MAX_NO_SUGGESTIONS"
        for(ApiResult packedSuggestion : packedSuggestions) {
            if (count< TranslateWikiApp.MAX_NO_SUGGESTIONS) {
                s = packedSuggestion.getString("@target");
                if (s.length() > TranslateWikiApp.MAX_SUGGESTION_LENGTH)
                    continue;

                //Log.d("TWN", "suggestion is" + s); // DEBUG
                if(msg.addSuggestion(s)) // this call also makes sure no to add duplicates
                    count++;
            } else break;
        }

        // insert suggestions with not longer than "MAX_SUGGESTION_LENGTH",
        // with a quality of at least "MIN_SUGGESTION_QUALITY",
        // but no more than "MAX_NO_SUGGESTIONS"
        ArrayList<ApiResult> packedTtms = result.getNodes("/api/helpers/ttmserver/suggestion");
        for(ApiResult packedSuggestion: packedTtms) {
            if (count< TranslateWikiApp.MAX_NO_SUGGESTIONS) {
                if (packedSuggestion.getNumber("@quality") < TranslateWikiApp.MIN_SUGGESTION_QUALITY)
                    continue;
                s = packedSuggestion.getString("@target");
                if (s.length() > TranslateWikiApp.MAX_SUGGESTION_LENGTH)
                    continue;
                //Log.d("TWN", "suggestion is" + s); // DEBUG
                if (msg.addSuggestion(s)) // this call also makes sure no to add duplicates
                    count++;
            } else break;
        }

        // extract documentation
        String doc = result.getNode("/api/helpers/documentation").getString("@html");
        // documentation is a helper which is shown by WebView,
        // since it is written in 'wiki' code, contains patterns etc.

        doc = doc.split("<div class=\"mw-identical-title mw-collapsible mw-collapsed")[0];
        // some of the documentations include a collapsible tail which let the user see
        // similar usage examples. we are not interested in this tail, for now.

        //Log.d("TWN", "doc is: " + doc); // DEBUG
        return doc;
    }

    @Override
    protected void onPostExecute(String doc) {
        super.onPostExecute(doc);
        msg.setDocumentation(doc);
        ((MainActivity)context).postFetchHelpers(msg);
    }
}

