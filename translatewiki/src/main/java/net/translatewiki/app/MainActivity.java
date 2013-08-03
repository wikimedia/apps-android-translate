package net.translatewiki.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.sample.demos.SampleList;
import com.actionbarsherlock.view.Menu;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.mediawiki.auth.AuthenticatedActivity;
import org.mediawiki.auth.Utils;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AuthenticatedActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String translateToken = null;

    private Message curMessage;
    private MessageListAdapter translations;
    private int curIndex;
    private int selected;
    private static Integer offset = 0;


    private class FetchTranslationsTask extends AsyncTask<Void, Void, ArrayList<Message>> {

        private Activity context;
        private String lang;
        private Integer limit;
        private int msgType;            // 1 - proofread (translated)  2 - translate (untranslated)

        // CTOR
        public FetchTranslationsTask(Activity context, String lang, Integer limit, int type) {
            this.context = context;
            this.lang = lang;
            this.limit = limit;
            this.msgType = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // indicate the fetch starts
            Toast toast =  Toast.makeText(context,"Loading!", Toast.LENGTH_LONG);
            toast.show();
        }

        @Override
        protected ArrayList<Message> doInBackground(Void... params) {
            MWApi api = app.getApi();
            ArrayList<Message> messagesList = new ArrayList<Message>();
            ApiResult result;
            try { // send API request
                String userId = api.getUserID();
                result = api.action("query")
                        .param("list", "messagecollection")
                        .param("mcgroup", "!recent")            // project
                        .param("mclanguage", lang)              // language
                        .param("mclimit", limit.toString()) // number of messages to fetch
                        .param("mcoffset", offset.toString())   // index Offset
                        .param("mcprop", "definition|translation|revision|properties")  // info to get
                        .param("mcfilter", msgType == 1           // different filter for translated/untranslated
                                ? "!last-translator:" + userId + "|!reviewer:" + userId + "|!ignored|translated"
                                : "!ignored|!translated|!fuzzy")
                        .post();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            ArrayList<ApiResult> messages = result.getNodes("/api/query/messagecollection/message");
            Log.d("TWN", "Actual result is" + Utils.getStringFromDOM(result.getDocument()));
            for(ApiResult message: messages) {
                Message m = new Message(message.getString("@key"), lang, message.getString("@definition"), message.getString("@translation"), message.getString("@revision"), message.getNodes("properties/reviewers").size());

                // todo: this is the place to do filtering if needed, such too long messages etc.

                messagesList.add(m);
            }
            return messagesList;
        }

        @Override
        protected void onPostExecute(ArrayList<Message> result) {
            super.onPostExecute(result);
            for(Message m : result) { // add new messages to our data store.
                translations.add(m);
                offset++;
            }
        }
    }

    private class ReviewTranslationTask extends AsyncTask<Void, Void, Boolean> {

        private Activity context;
        private Message message;

        // CTOR
        public ReviewTranslationTask(Activity context, Message message) {
            this.context = context;
            this.message = message;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            translations.remove(message); //accepted message will no longer be showed
            Toast successToast = Toast.makeText(context,message.getKey()+" accepting...", Toast.LENGTH_SHORT);
            successToast.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if(!app.getApi().validateLogin()) {
                    if(((TranslateWikiApp)app).revalidateAuthToken()) {
                        // Validated!
                        Log.d("TWN", "VALIDATED!");
                    } else {
                        Log.d("TWN", "Invalid :(");
                        throw new RuntimeException();
                    }
                }
                if(translateToken == null) {
                    ApiResult tokenResult;
                    tokenResult = app.getApi().action("tokens").param("type", "translationreview").post();
                    Log.d("TWN", "First result is " + Utils.getStringFromDOM(tokenResult.getDocument()));
                    translateToken = tokenResult.getString("/api/tokens/@translationreviewtoken");
                    Log.d("TWN", "Token is " + translateToken);
                }
                // send API request for review message
                ApiResult reviewResult = app.getApi().action("translationreview")
                        .param("revision", message.getRevision())
                        .param("token", translateToken).post();
                Log.d("TWN", Utils.getStringFromDOM(reviewResult.getDocument()));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            // TODO: check indeed successful, we are lying meanwhile...
            Toast successToast = Toast.makeText(context,message.getKey()+" accepted!", Toast.LENGTH_SHORT);
            successToast.show();
        }
    }

    // handles the way our messages data-store is reflected at the UI
    private class MessageListAdapter extends ArrayAdapter<Message> {

        public MessageListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount(){
            return super.getCount()+1; //room for one more cell contains: "More"
        }

        @Override
        // handles how line number "position" will look like.
        public View getView(int position, View convertView, ViewGroup parent) {

            View v;
            if (position == getCount()-1){ // means it's the last line. we populate it with "more" button
                v = getLayoutInflater().inflate(R.layout.listitem_more,parent,false);
                return v;
            }
            //else
            v = convertView;
            if(v == null || !v.getTag().equals("pr")) {
                // means the convertView is not the type we need,
                // otherwise, we could use it without expansive creation of new object.
                v = getLayoutInflater().inflate(R.layout.listitem_translation, parent,false);
            }

            final Message m = this.getItem(position);

            // get all the relevant components
            TextView lblSourceText = (TextView) v.findViewById(R.id.lblSourceText);
            TextView lblTranslatedText = (TextView) v.findViewById(R.id.lblTranslatedText);
            TextView lblAcceptText = (TextView) v.findViewById(R.id.lblAcceptText);
            Button btnReject = (Button) v.findViewById(R.id.btnReject);
            Button btnAccept = (Button) v.findViewById(R.id.btnAccept);

            // bind action for reject
            btnReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    translations.remove(m);
                }
            });

            // bind action for accept
            btnAccept.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    ReviewTranslationTask task = new ReviewTranslationTask(MainActivity.this, m);
                    Utils.executeAsyncTask(task);
                }
            });

            // populate view with message details
            lblSourceText.setText(m.getDefinition());
            lblTranslatedText.setText(m.getTranslation());

            // show Accept Count - iff greater than 0.
            int iACount = m.getAcceptCount();
            if (iACount>0)
            {
                String sACount = (new Integer(iACount)).toString();
                lblAcceptText.setText(sACount);
                lblAcceptText.setVisibility(View.VISIBLE);
            }
            else
            {
                lblAcceptText.setVisibility(View.INVISIBLE);
            }

            // show accept/reject only for selected cell.
            View ol =  v.findViewById(R.id.optionsLayout);
            if(position==selected){
                ol.setVisibility(View.VISIBLE);
            }else{
                ol.setVisibility(View.GONE);
            }

            return v;
        }
    }

    void showMessage(Message message) {
        curMessage = message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(net.translatewiki.app.R.layout.activity_main);

        selected = 0; //the first one is selected at the beginning
        translations = new MessageListAdapter(this, 0);
        ListView listView = (ListView) findViewById(net.translatewiki.app.R.id.listTranslations);
        listView.setAdapter(translations);

        // bind action when clicking on cells
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i==adapterView.getCount()-1){ //in case "more" is clicked
                    fetchTranslations();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), new Long(l).toString(), Toast.LENGTH_SHORT).show(); //DEBUG purpose
                    selected = (selected==i ? -1 : i);  //if already selected - deselect, otherwise move to selected.
                    new Thread() { //this allows the UI update the messagelist at the background.
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // update messageList so the newly selected will be showed with buttons
                                    translations.notifyDataSetChanged();
                                }
                            });
                        }
                    }.start();
                }
            }
        });

        requestAuthToken();
    }

    private void fetchTranslations() { // just applying FetchTranslationsTask in a friendlier way
        String lang = PreferenceManager.getDefaultSharedPreferences(this).getString("language", "en");
        FetchTranslationsTask fetchTranslations = new FetchTranslationsTask(this, "he", 5, 1);
        fetchTranslations.execute();
    }

    private void refreshTranslations() { // exactly as "fetchTranslations", but clear first
        offset = 0;
        translations.clear();
        fetchTranslations();
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        super.onAuthCookieAcquired(authCookie);
        app.getApi().setAuthCookie(authCookie);
        refreshTranslations();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onAuthFailure() {
        super.onAuthFailure();
        Toast failureToast = Toast.makeText(this, net.translatewiki.app.R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        refreshTranslations();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Used to put icons on action bar
        boolean isLight = SampleList.THEME == com.actionbarsherlock.R.style.Theme_Sherlock_Light;

        menu.add("Search")
                .setIcon(isLight ? R.drawable.ic_search_inverse : R.drawable.ic_search)
                .setActionView(R.layout.collapsible_edittext)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);


        menu.add("Refresh")
                .setIcon(isLight ? R.drawable.ic_refresh_inverse : R.drawable.ic_refresh)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        // TODO: also bind some real actions

        return true;
    }
}
