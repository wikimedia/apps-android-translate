package net.translatewiki.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

import org.mediawiki.auth.AuthenticatedActivity;
import org.mediawiki.auth.LoginActivity;
import org.mediawiki.auth.MWApiApplication;
import org.mediawiki.auth.Utils;

public class MainActivity extends AuthenticatedActivity {

    // hard-coded properties.
    public static final int     MAX_LENGTH_FOR_SUGGESTION  = 100;
    public static final int     MAX_NO_SUGGESTIONS         = 3;
    public static final Double  MIN_QUALITY_FOR_SUGGESTION = 0.9;

    // global properties:
    public static int       CURRENT_STATE   = 1;    // 1 - proofread    2 - translate
    public static String    CUR_LANG        = "en";
    public static String    CUR_PROJECT     = "!recent";
    public static Integer   FETCH_SIZE      = 6;
    public static int       MAX_LENGTH_FOR_MESSAGE = 100;

    public static RejectedMessagesDbHelper mDbHelper;  //database helper for rejected messages

    public static MessageListAdapter translations; // serve as a data controller for the messages
    public static Integer offset = 0;       // the offset index to fetch from server
    public static LayoutInflater layoutInflater;

    private int selected;
    public static String reviewToken = null;
    public static String translateToken = null;
    public static ListView msgListView;
    private static Intent staticIntent = null;


    public MWApiApplication getApp(){
        return app;
    }

    public static Intent getStaticIntent() {
        return staticIntent;
    }

    public void postFetchHelpers(MessageAdapter m){
        int mPosition = MainActivity.translations.getPosition(m);
        int minPos = MainActivity.msgListView.getFirstVisiblePosition();
        int maxPos = MainActivity.msgListView.getLastVisiblePosition();
        if (mPosition>=minPos && mPosition<=maxPos)
        {
            notifyTranslationsOnNewThread();
        }
    }

    public void notifyTranslationsOnNewThread(){
        new Thread() { //this allows the UI update the messagelist at the background.
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        translations.notifyDataSetChanged();
                    }
                });
            }
        }.start();
    }

    /**
     * handles the way messages data-store is reflected at the UI
     */
    public class MessageListAdapter extends ArrayAdapter<MessageAdapter> {

        public MessageListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        // 'getViewTypeCount' and 'getItemViewType' help the ListAdapter to reuse objects
        // and therefore save performance impact
        @Override
        public int getViewTypeCount (){ return 4; }

        @Override
        public int getItemViewType (int position){
            if (position==getCount()-1) return 3;   // 'more' button
            MessageAdapter m = getItem(position);
            if (m.getmState()==1)       return 0;   // proofread message
            if (m.isCommitted())        return 2;   // committed translation
            else                        return 1;   // translation message
        }

        @Override
        public int getCount(){
            return super.getCount()+1; //room for one more cell contains: "More"
        }

        @Override
        // handles how line number "position" will look like.
        public View getView(int position, View convertView, ViewGroup parent) {

            View v=null;
            TextView lblSourceText = null;
            int viewType = getItemViewType(position);
            final MessageAdapter m = ( viewType<3 ? this.getItem(position) : null );

            if (viewType<2)
            {
                v = convertView==null
                    ? (   viewType==0
                        ? getLayoutInflater().inflate(R.layout.listitem_review, parent,false)
                        : getLayoutInflater().inflate(R.layout.listitem_translation, parent,false))
                    : convertView;
                lblSourceText = (TextView) v.findViewById(R.id.lblSourceText);
                lblSourceText.setText(m.getDefinition());
            }

            switch (viewType){
                case 0:     // proofread view

                    // get all the relevant components
                    assert v != null;
                    TextView lblTranslatedText = (TextView) v.findViewById(R.id.lblTranslatedText);
                    TextView lblAcceptText     = (TextView) v.findViewById(R.id.lblAcceptText);
                    View btnReject = v.findViewById(R.id.btnReject);
                    View btnAccept = v.findViewById(R.id.btnAccept);
                    View btnEdit   = v.findViewById(R.id.btnEdit);

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
                            // save only revision number.

                            if (db!=null) db.insert( RejectedMessagesDbHelper.MsgEntry.TABLE_NAME, null,  values);

                            translations.remove(m);
                        }
                    });

                    // bind action for edit
                    btnEdit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            m.setmState(2);
                            new FetchHelpersTask((Activity)getContext(),m).execute();
                            translations.notifyDataSetChanged();
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
                        lblAcceptText.setVisibility(View.INVISIBLE); // do not show accept count when zero
                    }

                    // show accept/reject only for selected cell.
                    View ol =  v.findViewById(R.id.optionsLayout);
                    if (ol!=null)
                        ol.setVisibility( position==selected ? View.VISIBLE : View.GONE);
                    ol =  v.findViewById(R.id.btnEdit);
                    if (ol!=null)
                        ol.setVisibility( position==selected ? View.VISIBLE : View.GONE);
                    break;

                case 1:     // translation view
                    View delBtn = v.findViewById(R.id.deleteButton);
                    View canBtn = v.findViewById(R.id.cancelButton);
                    View infoBtn = v.findViewById(R.id.infoButton);
                    ListView sugListView = (ListView)v.findViewById(R.id.listSuggestions);
                    WebView infoWebView;
                    final ViewFlipper viewFlipper;
                    if (convertView==null)
                    {
                        assert v != null;
                        ViewGroup infoLayout = (ViewGroup) v.findViewById(R.id.infoLayout);
                        infoWebView = new WebView(getContext());
                        infoWebView.setWebViewClient(new WebViewClient(){
                            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                return true;
                            }
                        });
                        infoWebView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));//viewFlipper.getHeight()));
                        infoWebView.setBackgroundColor(0); //todo: remove this line or the one below
                        infoWebView.setBackgroundColor(getContext().getResources().getColor(R.drawable.infoview_color));
                        infoWebView.setVerticalScrollBarEnabled(true);
                        WebSettings ws= infoWebView.getSettings();
                        ws.setTextSize(WebSettings.TextSize.SMALLER);
                        infoLayout.addView(infoWebView);

                        viewFlipper = (ViewFlipper)v.findViewById(R.id.viewFlipper);
                        viewFlipper.setInAnimation(getContext(),R.anim.flipin);
                        viewFlipper.setOutAnimation(getContext(),R.anim.flipout);

                    }
                    else
                    {
                        infoWebView = (WebView)((ViewGroup)v.findViewById(R.id.infoLayout)).getChildAt(0);
                        viewFlipper = (ViewFlipper)v.findViewById(R.id.viewFlipper);
                    }

                    sugListView.setAdapter(m.suggestionsAdapter);
                    m.suggestionsAdapterView = sugListView; // we need it in order to update suggestions with better performance

                    canBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            m.setmState(1);
                            translations.notifyDataSetChanged();
                        }
                    });

                    delBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            translations.remove(m);
                        }
                    });

                    infoBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            m.flipInfo();
                            viewFlipper.showNext();
                        }
                    });

                    // set on click listener for suggestions
                    sugListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            if (i < adapterView.getCount() - 1) { // suggestion has been clicked
                                m.savedInput = adapterView.getItemAtPosition(i).toString();
                                new SubmitTranslationTask((Activity) getContext(), m).execute();
                                notifyDataSetChanged();
                            }
                        }
                    });

                    if (viewFlipper.getCurrentView().getTag().equals("info") != m.isInfoNeedShown()){

                        // need to change view without animation. it is sadly inefficient...
                        Animation in=viewFlipper.getInAnimation();
                        Animation out=viewFlipper.getOutAnimation();
                        viewFlipper.setInAnimation(null);
                        viewFlipper.setOutAnimation(null);
                        viewFlipper.showNext();
                        viewFlipper.setInAnimation(in);
                        viewFlipper.setOutAnimation(out);
                    }

                    infoBtn.setVisibility(m.IsDocumentationExists() ? View.VISIBLE:View.GONE);

                    infoWebView.loadData(m.getDocumentation(), "text/html; charset=UTF-8", null); // load documentation on card

                    delBtn.setVisibility( CURRENT_STATE==1 ? View.GONE    : View.VISIBLE );
                    canBtn.setVisibility( CURRENT_STATE==1 ? View.VISIBLE : View.GONE    );

                    View suggestionsLayout = v.findViewById(R.id.listSuggestions);
                    int y = 0;
                    StaticLayout layout;
                    TextPaint paint = lblSourceText.getPaint();
                    for (String s : m.getSuggestionsList()){

                        layout = new StaticLayout(s,paint,suggestionsLayout.getWidth(), Layout.Alignment.ALIGN_NORMAL,1.0f, 0.0f, false);
                        y += layout.getHeight()+6;
                    }

                    View suggestionsFrame = v.findViewById(R.id.suggestionsLayout);
                    ViewGroup.LayoutParams newParams = suggestionsFrame.getLayoutParams();
                    newParams.height = y+94;
                    suggestionsFrame.setLayoutParams(newParams);

                    // we may want to select a better .png for the frame
//                    float r =  (float)suggestionsFrame.getWidth() / (float)newParams.height;
//                    if (r >= 3.7)
//                        suggestionsFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.new_frame_1));
//                    else if (newParams.height >= 2.3)
//                        suggestionsFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.new_frame_2));
//                    else
//                        suggestionsFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.new_frame_3));

                    break;

                case 2:     // committed translation view

                    v = convertView==null ? getLayoutInflater().inflate(R.layout.listitem_committed, parent,false)
                            : convertView;

                    assert v != null;
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
    public void fetchTranslations() {
        FetchTranslationsTask fetchTranslations = new FetchTranslationsTask(this, CUR_LANG, CUR_PROJECT, FETCH_SIZE, CURRENT_STATE);
        fetchTranslations.execute();
    }

    public void refreshTranslations() { // exactly as "fetchTranslations", but clear first
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        CUR_LANG    = sharedPref.getString(getString(R.string.langugage_key), CUR_LANG);
        CUR_PROJECT = sharedPref.getString(getString(R.string.projects_key), CUR_PROJECT);
        FETCH_SIZE  = sharedPref.getInt(getString(R.string.fetch_size_key), FETCH_SIZE);
        MAX_LENGTH_FOR_MESSAGE = sharedPref.getInt(getString(R.string.max_length_for_message_key), MAX_LENGTH_FOR_MESSAGE);
        offset = 0;
        translations.clear();
        fetchTranslations();
    }

    public class sendButtonListener implements View.OnClickListener {

        private MessageAdapter m;
        private Context context;

        public sendButtonListener(Context context, MessageAdapter m){
            this.m = m;
            this.context = context;
        }

        @Override
        public void onClick(View view) {
            // m.input already set on every change
            new SubmitTranslationTask((Activity) context, m).execute();
            translations.notifyDataSetChanged();
        }
    }

    // inflates a view for the input view
    public View getViewForInput(ViewGroup parent, final MessageAdapter m) {
        View v = layoutInflater.inflate(R.layout.listitem_input, parent, false);

        assert v != null;
        EditText et = (EditText) v.findViewById(R.id.editText);
        final Button sendBtn  = (Button) v.findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new sendButtonListener(this, m));

        if (m.savedInput!=null && m.savedInput.length()>0){
            et.setText(m.savedInput);
            sendBtn.setEnabled(true);
        }
        else{
            et.setText("");
            sendBtn.setEnabled(false);
        }

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void afterTextChanged(Editable editable) {
                m.savedInput = editable.toString();
                if (editable.toString().length()>0){
                    sendBtn.setEnabled(true);
                }
                else{
                    sendBtn.setEnabled(false);
                }
            }
        });

        return v;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras =  getIntent().getExtras();
        if (extras !=null && extras.getBoolean("should_logout_first")){
            getApp().setCurrentAccount(null);
            Intent i= new Intent(this, LoginActivity.class);//homescreen of your app.
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
            i.putExtra("should_logout_first",true);
            startActivity(i);
            finish();
            return;
        }
        staticIntent = getIntent();
        requestAuthToken();

        getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_color));

        setContentView(net.translatewiki.app.R.layout.activity_main);

        layoutInflater = getLayoutInflater();
        selected = 0; //the first one is selected at the beginning
        translations = new MessageListAdapter(this, 0);
        msgListView = (ListView) findViewById(net.translatewiki.app.R.id.listTranslations);
        msgListView.setAdapter(translations);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        CURRENT_STATE   = sharedPref.getInt(getString(R.string.state_key), CURRENT_STATE);
        setTitle(CURRENT_STATE==1 ? "Proofread" : "Translate");

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
                        translations.notifyDataSetChanged();
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
                                ol =  previous.findViewById(R.id.btnEdit);
                                if (ol!=null)
                                    ol.setVisibility(View.GONE);
                            }
                        }

                        selected = (selected==i ? -1 : i);  //if already selected - deselect, otherwise move to selected.

                        // show accept/reject only for selected cell.
                        ol =  view.findViewById(R.id.optionsLayout);
                        if (ol!=null)
                            ol.setVisibility(selected==i ? View.VISIBLE : View.GONE);
                        ol =  view.findViewById(R.id.btnEdit);
                        if (ol!=null)
                            ol.setVisibility(selected==i ? View.VISIBLE : View.GONE);
                    }
                }
            }
        });
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        super.onAuthCookieAcquired(authCookie);
        app.getApi().setAuthCookie(authCookie);
        refreshTranslations();
    }

    @Override
    protected void onAuthFailure() {
        super.onAuthFailure();
        Toast failureToast = Toast.makeText(this, net.translatewiki.app.R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bundle extras =  getIntent().getExtras();
        if (extras !=null && extras.getBoolean("should_refresh_translations")){
            selected = -1;
            refreshTranslations();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Used to put icons on action bar

        // Inflate the menu items for use in the action bar
        com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main, menu);

        boolean isLight = SampleList.THEME == com.actionbarsherlock.R.style.Theme_Sherlock_Light;

        //MenuItem searchMenuItem = menu.add("Search");
        //searchMenuItem.setIcon(isLight ? R.drawable.ic_search_inverse : R.drawable.ic_search)
        //              .setActionView(R.layout.collapsible_edittext)
        //              .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        menu.findItem(R.id.action_proofread).setTitle(CURRENT_STATE==1 ? "Translate" : "Proofread");

        // new version
        menu.findItem(R.id.sub_menu_item).setVisible(false);
        MenuItem settingsBtn = menu.add("Settings");
        settingsBtn.setIcon(isLight ? R.drawable.settings_icon_inverse : R.drawable.settings_icon).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        settingsBtn.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                openSettings();
                return true;
            }
        });

        getActionBar().setHomeButtonEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item)
    {
        switch (item.getItemId()){
            case R.id.sub_menu_item:
                break;
            case R.id.action_settings:
                openSettings();
                break;
            case R.id.action_proofread:
            default:
                if (CURRENT_STATE!=1){
                    setTitle("Proofread");
                    item.setTitle("Translate");
                    switchState(1);
                } else if (CURRENT_STATE!=2){
                    setTitle("Translate");
                    item.setTitle("Proofread");
                    switchState(2);
                } else return false;
                break;
        }
        return true;
    }

    public void switchState(int newState){
        CURRENT_STATE = newState;
        refreshTranslations();
        SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        sharedPrefEditor.putInt(getString(R.string.state_key), CURRENT_STATE);
        sharedPrefEditor.commit();
    }

    public void openSettings(){
        Intent intent =  new Intent(this,SettingsActivity.class);
        startActivity(intent);
    }
}
