package net.translatewiki.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.sample.demos.SampleList;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.mediawiki.auth.AuthenticatedActivity;
import org.mediawiki.auth.Utils;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AuthenticatedActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    // hard-coded properties. TODO: change this
    public static final String  CUR_PROJECT     = "!additions";
    public static final Integer FETCH_SIZE      = 7;
    public static final int     CURRENT_STATE   = 2;    // 1 - proofread    2 - translate
    public static final String  CUR_LANG        = "hi";

    private String reviewToken = null;
    private String translateToken = null;

    private Message curMessage;
    private static MessageListAdapter translations; // serve as a data controller for the messages
    private int selected;
    private static Integer offset = 0;       // the offset index to fetch from server
    private static LayoutInflater layoutInflater;

    private ListView msgListView;

    /**
     * Handles the task of getting messages (translated fo proofread or else for translation),
     * filtering and storing to MessageListAdapter
     */
    private class FetchTranslationsTask extends AsyncTask<Void, Void, ArrayList<MessageAdapter>> {

        private Activity context;
        private String lang;
        private Integer limit;
        private int msgState;            // 1 - proofread (translated)  2 - translate (untranslated)

        // CTOR
        public FetchTranslationsTask(Activity context, String lang, Integer limit, int state) {
            this.context = context;
            this.lang = lang;
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
            MWApi api = app.getApi();
            ArrayList<MessageAdapter> messagesList = new ArrayList<MessageAdapter>();
            ApiResult result;
            try { // send API request
                String userId = api.getUserID();
                result = api.action("query")
                        .param("list", "messagecollection")
                        .param("mcgroup", CUR_PROJECT)            // project TODO: dont use hard-coded
                        .param("mclanguage", lang)              // language
                        .param("mclimit", limit.toString()) // number of messages to fetch
                        .param("mcoffset", offset.toString())   // index Offset
                        .param("mcprop", "definition|translation|revision|properties")  // info to get
                        .param("mcfilter", msgState == 1           // different filter for translated/untranslated
                                ? "!last-translator:" + userId + "|!reviewer:" + userId + "|!ignored|translated"
                                : "!ignored|!translated|!fuzzy")
                        .post();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            ArrayList<ApiResult> messages = result.getNodes("/api/query/messagecollection/message");
            Log.d("TWN", "Actual result is" + Utils.getStringFromDOM(result.getDocument()));

            // todo: this might be good place to do filtering if needed, such too long messages etc.


            for(ApiResult message: messages) {
                MessageAdapter m = new MessageAdapter(context,
                                                      message.getString("@key"),
                                                      message.getString("@title"),
                                                      CUR_PROJECT,
                                                      lang,
                                                      message.getString("@definition"),
                                                      message.getString("@translation"),
                                                      message.getString("@revision"),
                                                      message.getNodes("properties/reviewers").size(),
                                                      msgState);

                    messagesList.add(m);

                //get suggestionsAdapter for this message
                if (m.getmState() == 2){  // iff non-translated message
                    new FetchHelpersTask(m).execute(null);
                }
            }
            return messagesList;
        }

        @Override
        protected void onPostExecute(ArrayList<MessageAdapter> result) {
            super.onPostExecute(result);

            //prepare SQL db for query
            RejectedMessagesDbHelper mDbHelper = new RejectedMessagesDbHelper(context);

            for(MessageAdapter m : result) { // add new messages to our data store.

                // query rejected msgs database
                Cursor c = mDbHelper.queryRevision(m.getRevision());

                if (c.getCount()==0){     // iff not found as rejected

                    translations.add(m);
                }

                offset++;
            }
        }
    }

    /**
     * Handles the task of reviewing (accepting) translated by others messages.
     */
    private class ReviewTranslationTask extends AsyncTask<Void, Void, Boolean> {

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
                if(reviewToken == null) {
                    ApiResult tokenResult;
                    tokenResult = app.getApi().action("tokens").param("type", "translationreview").post();
                    Log.d("TWN", "First result is " + Utils.getStringFromDOM(tokenResult.getDocument()));
                    reviewToken = tokenResult.getString("/api/tokens/@translationreviewtoken");
                    Log.d("TWN", "Token is " + reviewToken);
                }
                // send API request for review message
                ApiResult reviewResult = app.getApi().action("translationreview")
                        .param("revision", message.getRevision())
                        .param("token", reviewToken).post();
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


    /**
     * Handles the task of contributing new translation (edit) for a message.
     */
    private class SubmitTranslationTask extends AsyncTask<Void, Void, Boolean> {

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
                    tokenResult = app.getApi().action("tokens").param("type", "edit").post();
                    Log.d("TWN", "First result is " + Utils.getStringFromDOM(tokenResult.getDocument()));
                    translateToken = tokenResult.getString("/api/tokens/@edittoken");
                    Log.d("TWN", "Token is " + translateToken);
                }
                // send API request for review message
                ApiResult reviewResult = app.getApi().action("edit")
                        .param("title", message.getmTitle())
                        .param("text", message.savedInput)
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
            Toast successToast = Toast.makeText(context, "committed!", Toast.LENGTH_SHORT);
            successToast.show();

            new Thread() { //this allows the UI update the messagelist at the background.
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // update messageList so the newly selected will be showed with buttons
                            translations.notifyDataSetChanged();
                            //m.suggestionsAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }.start();
        }
    }


    /**
     * Handles the task of getting helpers (suggestions and documentation).
     */
    private class FetchHelpersTask extends AsyncTask<Void, Void, Void>{

        public MessageAdapter m;

        // CTOR
        public FetchHelpersTask(MessageAdapter m) {
            this.m = m;
        }

        @Override
        protected Void doInBackground(Void... params) {
            MWApi api = app.getApi();
            ApiResult result;
            try { // send API request
                result = api.action("translationaids")
                        .param("title", m.getmTitle())
                        .param("group", m.getmGrupe())            // project TODO: dont use hard-coded
                        .param("prop", "mt|ttmserver|documentation")  // info to get
                        .post();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            // extracts suggestions
            ArrayList<ApiResult> packedSuggestions = result.getNodes("/api/helpers/mt/suggestion");
            Log.d("TWN", "Actual result is" + Utils.getStringFromDOM(result.getDocument()));
            for(ApiResult packedSuggestion: packedSuggestions)
            {
                String s = packedSuggestion.getString("@target");
                Log.d("TWN", "suggestion is" + s);
                //m.suggestionsAdapter.add(s);
                m.addSuggestion(s);
            }

            // TODO: need to add here suggestions of "ttmserver"
            // TODO: filter suggestions do not show more than {N} or longer than {L} suggestions

            // extract documentation
            String d = result.getNode("/api/helpers/documentation").getString("@html");
            // documentation is a helper which is shown by WebView,
            // since it is written in 'wiki' code, contains patterns etc.

            d = d.split("<div class=\"mw-identical-title mw-collapsible mw-collapsed\">")[0];
            // some of the documentations include a collapsible tail which let the user see
            // similar usage examples. we are not interested in this tail, for now.

            m.setDocumentation(d);

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);

            int mPosition = translations.getPosition(m);
            int minPos = msgListView.getFirstVisiblePosition();
            int maxPos = msgListView.getLastVisiblePosition();
            if (mPosition>=minPos && mPosition<=maxPos)
            {
                //View updateView = mAdapterView.getChildAt(mPosition-minPos);
                //AdapterView final sugAdapterView = (AdapterView) updateView.findViewById(R.id.listSuggestions);

                new Thread() { //this allows the UI update the messagelist at the background.
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // update messageList so the newly selected will be showed with buttons
                                   translations.notifyDataSetChanged();
                                //m.suggestionsAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }.start();
            }
        }
    }


    /**
     * handles the way messages data-store is reflected at the UI
     */
    private class MessageListAdapter extends ArrayAdapter<MessageAdapter> {

        public MessageListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getViewTypeCount (){
            return 4;
        }

        @Override
        public int getItemViewType (int position){

            if (position==getCount()-1)
               return 3;

            MessageAdapter m = getItem(position);
            if (m.getmState()==1)
               return 0;

            if (m.isCommitted())
               return 2;
            else
               return 1;

        }

        @Override
        public int getCount(){
            return super.getCount()+1; //room for one more cell contains: "More"
        }

        @Override
        // handles how line number "position" will look like.
        public View getView(int position, View convertView, ViewGroup parent) {

            View v=null;
            TextView lblSourceText;
            final MessageAdapter m;
            int viewType = getItemViewType(position);
            switch (viewType){
                case 0:     // proofread view
                    m = this.getItem(position);
                    v = convertView==null ? getLayoutInflater().inflate(R.layout.listitem_review, parent,false)
                                          : convertView;

                    // get all the relevant components
                    lblSourceText = (TextView) v.findViewById(R.id.lblSourceText);
                    TextView lblTranslatedText = (TextView) v.findViewById(R.id.lblTranslatedText);
                    TextView lblAcceptText = (TextView) v.findViewById(R.id.lblAcceptText);
                    Button btnReject = (Button) v.findViewById(R.id.btnReject);
                    Button btnAccept = (Button) v.findViewById(R.id.btnAccept);

                    // bind action for reject
                    btnReject.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //add to rejected messages DB
                            RejectedMessagesDbHelper mDbHelper = new RejectedMessagesDbHelper(getContext());

                            // Gets the data repository in write mode
                            SQLiteDatabase db = mDbHelper.getWritableDatabase();

                            // Create a new map of values, where column names are the keys
                            ContentValues values = new ContentValues();
                            values.put(RejectedMessagesDbHelper.MsgEntry.COLUMN_NAME_ENTRY_ID,m.getRevision());
                            // we save only one value for now: revision number.

                            if (db != null) {
                                db.insert( RejectedMessagesDbHelper.MsgEntry.TABLE_NAME, null,  values);
                            }

                            // now remove from our list
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

                    break;

                case 1:     // translation view
                    m = this.getItem(position);
                    v = convertView==null ? getLayoutInflater().inflate(R.layout.listitem_translation, parent,false)
                            : convertView;

                    lblSourceText = (TextView) v.findViewById(R.id.lblSourceText); // TODO: remove code duplication
                    lblSourceText.setText(m.getDefinition());                     // TODO: also here

                    ListView suglistView = (ListView)v.findViewById(R.id.listSuggestions);
                    suglistView.setAdapter(m.suggestionsAdapter);
                    m.suggestionsAdapterView = suglistView;         // yes, we need it in order to update suggestions with better performance

                    final ViewFlipper viewFlipper = (ViewFlipper)v.findViewById(R.id.viewFlipper);

                    if (viewFlipper.getCurrentView().getTag().equals("info") && !m.isInfoNeedShown()){
                        viewFlipper.showNext();
                    }

                    Animation animationFlipIn  = AnimationUtils.loadAnimation(getContext(), R.anim.flipin);
                    Animation animationFlipOut = AnimationUtils.loadAnimation(getContext(), R.anim.flipout);
                    viewFlipper.setInAnimation(animationFlipIn);
                    viewFlipper.setOutAnimation(animationFlipOut);

                    View infoBtn = v.findViewById(R.id.infoButton);
                    infoBtn.setVisibility(m.IsDocumentationExists() ? View.VISIBLE:View.GONE);
                    infoBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            m.flipInfo();
                            viewFlipper.showNext();
                        }
                    });

                    // prepare web view
                    WebView wv = (WebView) v.findViewById(R.id.webView);
                    wv.setBackgroundColor(0); //transparent
                    wv.setFocusableInTouchMode(false);
                    wv.setFocusable(false);
                    wv.setVerticalScrollBarEnabled(true);
                    wv.loadData(m.getDocumentation(), "text/html", null);  // load documentation on card

                    // set on click listener for suggestions
                    suglistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            if (i<adapterView.getCount()-1){ // suggestion has been clicked
                                m.savedInput = adapterView.getItemAtPosition(i).toString();
                                new SubmitTranslationTask((Activity) getContext(),m).execute();
                            }
                        }
                    });

                    break;
                case 2:     // committed translation view
                    m = this.getItem(position);

                    v = convertView==null ? getLayoutInflater().inflate(R.layout.listitem_committed, parent,false)
                            : convertView;

                    TextView lblCommittedText = (TextView) v.findViewById(R.id.lblCommittedText); // TODO: remove code duplication
                    lblCommittedText.setText(m.savedInput);

                    break;
                case 3:     // "more" button
                    v = convertView==null ? getLayoutInflater().inflate(R.layout.listitem_more, parent,false)
                            : convertView;
                    return v;
            }
            return v;

        }
    }

    // just applying FetchTranslationsTask in a friendlier way
    private void fetchTranslations() {
        String lang = PreferenceManager.getDefaultSharedPreferences(this).getString("language", "en");
        FetchTranslationsTask fetchTranslations = new FetchTranslationsTask(this, CUR_LANG, FETCH_SIZE, CURRENT_STATE);
        fetchTranslations.execute();
    }

    private void refreshTranslations() { // exactly as "fetchTranslations", but clear first
        offset = 0;
        translations.clear();
        fetchTranslations();
    }

    void showMessage(Message message) {
        curMessage = message;
    }


    public class sendButtonListener implements View.OnClickListener {

        private MessageAdapter m;
        private Context context;

        public sendButtonListener(Context context, MessageAdapter m){
            this.m= m;
            this.context=context;
        }

        @Override
        public void onClick(View view) {
            new SubmitTranslationTask((Activity) context, m).execute();
        }
    }

    // inflates a view for the input view
    public View getViewForInput(ViewGroup parent, final MessageAdapter m) {
        View v = layoutInflater.inflate(R.layout.listitem_input, parent, false);
        EditText et = (EditText) v.findViewById(R.id.editText);
        et.setText(m.savedInput);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void afterTextChanged(Editable editable) {
                m.savedInput = editable.toString();
            }
        });

        Button sendBtn = (Button) v.findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new sendButtonListener(this, m));


        return v;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(net.translatewiki.app.R.layout.activity_main);

        layoutInflater = getLayoutInflater();
        selected = 0; //the first one is selected at the beginning
        translations = new MessageListAdapter(this, 0);
        msgListView = (ListView) findViewById(net.translatewiki.app.R.id.listTranslations);
        msgListView.setAdapter(translations);

        // bind action when clicking on cells
        msgListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i==adapterView.getCount()-1){ //in case "more" is clicked
                    fetchTranslations();
                }
                else
                {
                    //Toast.makeText(getApplicationContext(), new Long(l).toString(), Toast.LENGTH_SHORT).show(); //DEBUG purpose
                    View ol;

                    if (view.getTag().equals("cm"))
                    {
                        ((MessageAdapter)adapterView.getItemAtPosition(i)).setCommitted(false);   // because we move to edit mode
                        view = getLayoutInflater().inflate(R.layout.listitem_committed, adapterView,false);

                        TextView lblCommittedText = (TextView) view.findViewById(R.id.lblCommittedText); // TODO: remove code duplication
                        lblCommittedText.setText(((MessageAdapter)adapterView.getItemAtPosition(i)).savedInput);

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
                    else if (view.getTag().equals("pr"))
                    {
                        //deselect the previous one if visible
                        int lowBound = adapterView.getFirstVisiblePosition();
                        int highBound = adapterView.getLastVisiblePosition();
                        if (selected >= lowBound && selected <= highBound)
                        {
                            View previous = adapterView.getChildAt(selected-lowBound);

                            if (previous!=null) {
                                ol =  previous.findViewById(R.id.optionsLayout);
                                if (ol!=null)
                                    ol.setVisibility(View.GONE);
                            }
                        }

                        selected = (selected==i ? -1 : i);  //if already selected - deselect, otherwise move to selected.

                        // show accept/reject only for selected cell.
                        ol =  view.findViewById(R.id.optionsLayout);
                        if (ol!=null)
                            ol.setVisibility(selected==i ? View.VISIBLE : View.GONE);
                    }
                }
            }
        });

        requestAuthToken();
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

        MenuItem searchMenuItem = menu.add("Search");
        searchMenuItem.setIcon(isLight ? R.drawable.ic_search_inverse : R.drawable.ic_search)
                      .setActionView(R.layout.collapsible_edittext)
                      .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        MenuItem refreshMenuItem = menu.add("Refresh");
        refreshMenuItem.setIcon(isLight ? R.drawable.ic_refresh_inverse : R.drawable.ic_refresh)
                       .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        refreshMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Toast refreshToast = Toast.makeText(getApplicationContext(), "Refresh", Toast.LENGTH_SHORT);
                refreshToast.show();
                translations.notifyDataSetChanged();
                return true;
            }
        });


        // TODO: also bind some real actions

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item)
    {
            return true;
    }
}
