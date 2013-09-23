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
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Handles the task of getting messages (translated fo proofread or else for translation),
 * filtering and storing to MessageListAdapter
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class FetchTranslationsTask extends AsyncTask<Void, Void, ArrayList<MessageAdapter>> {

    private Activity context;
    private String lang;
    private String proj;
    private Integer limit;
    private MessageAdapter.State msgState;  // 1 - proofread (translated)  2 - translate (untranslated)
    private int numOfTrials;
    private int error = 0;     // in case of error. a handler can identify error type via this var

    public FetchTranslationsTask(Activity context, String lang, String proj, Integer limit,
                                 MessageAdapter.State state, int numOfTrials) {
        this.context = context;
        this.lang = lang;
        this.proj = proj;
        this.limit = limit;
        this.msgState = state;
        this.numOfTrials = numOfTrials;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // indicate the fetch starts
        Toast toast =  Toast.makeText(context,context.getString(R.string.loading_msgs), Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    protected ArrayList<MessageAdapter> doInBackground(Void... params) {
        MWApi api = ((MainActivity)context).getApp().getApi();
        ArrayList<MessageAdapter> messagesList = new ArrayList<MessageAdapter>();
        ApiResult result;
        try { // send API request
            String userId = api.getUserID();
            result = api.action("query")
                    .param("list", "messagecollection")
                    .param("mcgroup", proj.equals("!recent") && msgState == MessageAdapter.State.TRANSLATE
                                    ? "!additions" : proj) // project
                    .param("mclanguage", lang)             // language
                    .param("mclimit", limit.toString())    // number of messages to fetch
                    .param("mcoffset", MainActivity.offset.toString())             // index Offset
                    .param("mcprop", "definition|translation|revision|properties") // info to get
                    .param("mcfilter", msgState == MessageAdapter.State.PROOFREAD
                            ? "!last-translator:" + userId + "|!reviewer:" + userId + "|!ignored|translated"
                            : "!ignored|!translated|!fuzzy")
                    .post();
        } catch (IOException e) {
            e.printStackTrace();
            error = 2;
            return null;
        }

        ArrayList<ApiResult> messages = result.getNodes("/api/query/messagecollection/message");
        //Log.d("TWN", "Actual result is" + Utils.getStringFromDOM(result.getDocument()));  //DEBUG

        if (messages.size() == 0) {
            error = 1;
            return null;
        } else {
            MainActivity.offset += messages.size(); //advance offset
            String definition;
            for(ApiResult message: messages) {
                definition =   message.getString("@definition");
                if (definition.length()>MainActivity.MAX_MESSAGE_LENGTH) // skip over too long translations
                    continue;
                MessageAdapter m = new MessageAdapter(context,
                        message.getString("@key"),
                        message.getString("@title"),
                        proj,
                        lang,
                        definition,
                        message.getString("@translation"),
                        message.getString("@revision"),
                        message.getNodes("properties/reviewers").size(),
                        msgState);

                messagesList.add(m);
            }
            return messagesList;
        }
    }

    @Override
    protected void onPostExecute(ArrayList<MessageAdapter> result) {
        super.onPostExecute(result);
        Toast toast;
        if (result == null) { // in case of error follow the error type to handle
           switch (error) {
            case 1:
                toast = Toast.makeText(context,context.getString(R.string.no_more_msg), Toast.LENGTH_SHORT);
                toast.show();
                break;
            case 2:
                toast = Toast.makeText(context,context.getString(R.string.load_messages_failed), Toast.LENGTH_LONG);
                toast.show();
                break;
            default:
                break;
           }
        }

        if (result != null && result.size() > 0) {
            //prepare SQL db for query
            if (MainActivity.CURRENT_STATE == MainActivity.State.PROOFREAD
                    && MainActivity.mDbHelper == null) {
                MainActivity.mDbHelper = new RejectedMsgDbHelper(context);
            }

            int count = 0;
            for (MessageAdapter m : result) { // add new messages to our data store.
                if (msgState.ordinal() != MainActivity.CURRENT_STATE.ordinal())
                    continue; // drop if state has been changed

                if (MainActivity.CURRENT_STATE == MainActivity.State.PROOFREAD
                        && MainActivity.mDbHelper.containsRevision(m.getRevision()))
                    continue; // drop iff found as rejected

                MainActivity.translations.add(m);
                count++;

                //get suggestionsAdapter for this message
                if (msgState == MessageAdapter.State.TRANSLATE) {  // iff non-translated message
                    new FetchHelpersTask(context,m).execute();
                }
            }
            // completion fetch - next trial
            if ((numOfTrials > 0) && (count < limit) && (msgState.ordinal() == MainActivity.CURRENT_STATE.ordinal())) {
                new FetchTranslationsTask(context, lang, proj, limit - count, msgState, numOfTrials - 1)
                        .execute();
            }
        }
    }
}
