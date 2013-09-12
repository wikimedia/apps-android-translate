package net.translatewiki.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.mediawiki.auth.Utils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Handles the task of getting messages (translated fo proofread or else for translation),
 * filtering and storing to MessageListAdapter
 */
public class FetchTranslationsTask extends AsyncTask<Void, Void, ArrayList<MessageAdapter>> {

    private Activity context;
    private String lang;
    private String proj;
    private Integer limit;
    private int msgState;            // 1 - proofread (translated)  2 - translate (untranslated)

    public FetchTranslationsTask(Activity context, String lang, String proj, Integer limit, int state) {
        this.context = context;
        this.lang = lang;
        this.proj = proj;
        this.limit = limit;
        this.msgState = state;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // indicate the fetch starts
        Toast toast =  Toast.makeText(context,"Loading!", Toast.LENGTH_LONG);
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
                    .param("mcgroup", proj.equals("!recent") && msgState==2
                                    ? "!additions" : proj) // project
                    .param("mclanguage", lang)          // language
                    .param("mclimit", limit.toString()) // number of messages to fetch
                    .param("mcoffset", MainActivity.offset.toString())   // index Offset
                    .param("mcprop", "definition|translation|revision|properties")  // info to get
                    .param("mcfilter", msgState == 1           // different filter for translated/untranslated
                            ? "!last-translator:" + userId + "|!reviewer:" + userId + "|!ignored|translated"
                            : "!ignored|!translated|!fuzzy")
                    .post();
        } catch (IOException e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(context,"Load Fail", Toast.LENGTH_LONG);
            toast.show();
            this.cancel(true);
            return null;
        }

        ArrayList<ApiResult> messages = result.getNodes("/api/query/messagecollection/message");
        Log.d("TWN", "Actual result is" + Utils.getStringFromDOM(result.getDocument()));

        MainActivity.offset += messages.size(); //advance offset

        String definition;
        for(ApiResult message: messages) {

            definition =   message.getString("@definition");
            if (definition.length()>MainActivity.MAX_LENGTH_FOR_MESSAGE) // skip over too long translations
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

    @Override
    protected void onPostExecute(ArrayList<MessageAdapter> result) {
        super.onPostExecute(result);
        if (result!= null  && result.size()>0)
        {
            //prepare SQL db for query
            if (MainActivity.CURRENT_STATE==1 && MainActivity.mDbHelper==null){
                MainActivity.mDbHelper = new RejectedMessagesDbHelper(context);
            }

            int count = 0;
            Cursor c;
            for(MessageAdapter m : result) { // add new messages to our data store.

                if (m.getmState() != MainActivity.CURRENT_STATE) // drop if state has been changed
                    continue;

                if (MainActivity.CURRENT_STATE==1){
                    // query rejected msgs database
                    c = MainActivity.mDbHelper.queryRevision(m.getRevision());
                    if (c.getCount()!=0)     // iff not found as rejected
                        continue;
                }
                MainActivity.translations.add(m);
                count++;

                //get suggestionsAdapter for this message
                if (m.getmState() == 2){  // iff non-translated message
                    new FetchHelpersTask(context,m).execute();
                }
            }
            // completion fetch
            if (count<limit && msgState==MainActivity.CURRENT_STATE)
                new FetchTranslationsTask(context,lang, proj, limit-count,msgState).execute();
        }
    }
}

