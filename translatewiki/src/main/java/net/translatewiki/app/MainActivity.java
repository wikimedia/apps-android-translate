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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.Gravity;
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
import android.widget.LinearLayout;
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

/**
 * The main activity of the app. It does most of the work in one of the two
 * states: Proofread or Translate
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class MainActivity extends AuthenticatedActivity {

    public enum State {
        UNDEFINED, PROOFREAD, TRANSLATE;
    }

    // global properties:
    public static State     CURRENT_STATE = State.PROOFREAD;    // 1 - proofread   2 - translate
    public static String    CUR_LANG    ;
    public static String    CUR_PROJECT ;
    public static Integer   FETCH_SIZE  ;
    public static int       MAX_MESSAGE_LENGTH;

    public  static RejectedMsgDbHelper mDbHelper      = null; // database helper for rejected messages
    public  static MessageListAdapter  translations   = null; // a data controller for the messages
    public  static LayoutInflater      layoutInflater = null;
    public  static String              reviewToken    = null;
    public  static String              translateToken = null;
    public  static ListView            msgListView    = null;
    public  static Integer             offset         = 0;    // the offset index to fetch from server
    public  static MainActivity        that;

    private static Intent              staticIntent   = null;

    private int     selected    = -1;     // define the selected message index, or -1 if no such.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        that = this;
        Bundle extras =  getIntent().getExtras();
        if (extras != null && extras.getBoolean("should_logout_first")) {
            getApp().setCurrentAccount(null);
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
            i.putExtra("should_logout_first",true);
            startActivity(i);
            finish();
            return;
        }
        staticIntent = getIntent();
        requestAuthToken();

        layoutInflater = getLayoutInflater();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { // for later versions only
            getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_color));
        }
        setContentView(net.translatewiki.app.R.layout.activity_main);
        selected = 0; // first row is selected at the beginning
        translations = new MessageListAdapter(this, 0);
        msgListView = (ListView) findViewById(net.translatewiki.app.R.id.listTranslations);
        msgListView.setAdapter(translations);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        CURRENT_STATE = State.values()
                [(sharedPref.getInt(getString(R.string.state_key), CURRENT_STATE.ordinal()))];
        setTitle(CURRENT_STATE == State.PROOFREAD ? "Proofread" : "Translate");

        // bind action when clicking on cells
        msgListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == adapterView.getCount() - 1) { //in case "more" is clicked
                    fetchTranslations();
                } else if (view.getTag().equals("cm")) {
                    ((MessageAdapter)adapterView.getItemAtPosition(i)).setCommitted(false);   // because we move to edit mode
                    translations.notifyDataSetChanged();
                } else if (view.getTag().equals("pr")) {

                    //deselect the previous one if visible
                    int lowBound = adapterView.getFirstVisiblePosition();
                    int highBound = adapterView.getLastVisiblePosition();
                    if (selected >= lowBound && selected <= highBound) {
                        View previous = adapterView.getChildAt(selected-lowBound);
                        if (previous != null) {
                            previous.findViewById(R.id.optionsLayout).setVisibility(View.GONE);
                            previous.findViewById(R.id.btnEdit).setVisibility(View.GONE);
                        }
                    }
                    selected = (selected == i) ? -1 : i;  //if already selected - deselect, otherwise move to selected.

                    // show accept/reject only for selected cell.
                    view.findViewById(R.id.optionsLayout).setVisibility( selected == i ? View.VISIBLE
                                                                                       : View.GONE);
                    view.findViewById(R.id.btnEdit).setVisibility( selected == i ? View.VISIBLE
                                                                                 : View.GONE);
                }
            }
        });
    }

    /**
     *
     * @return static instance of the {@link MWApiApplication}
     */
    public MWApiApplication getApp() { return app; }

    /**
     *
     * @return static instance of this activity's Intent, useful to push
     * some data from other activities.
     */
    public static Intent getStaticIntent() { return staticIntent; }

    /**
     * helper for action when helpers (suggestions and documentation) are pushed.
     *
     * @param msg a message which the helpers belong to.
     */
    public void postFetchHelpers(MessageAdapter msg) {
        int mPosition = MainActivity.translations.getPosition(msg);
        int minPos = MainActivity.msgListView.getFirstVisiblePosition();
        int maxPos = MainActivity.msgListView.getLastVisiblePosition();
        if (mPosition>=minPos && mPosition<=maxPos) {
            notifyTranslationsOnNewThread();
        }
    }

    /**
     * make the heavy table update happen on UI Thread.
     */
    public void notifyTranslationsOnNewThread() {
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
     * Array adapter which handles the way messages data-store is reflected at the UI
     *
     * @version 1.0 15 Sept 2013
     * @author Or Sagi
     */
    public class MessageListAdapter extends ArrayAdapter<MessageAdapter> {

        public MessageListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        /* 'getViewTypeCount' and 'getItemViewType' improves object reuse and performance. */

        /**  {@inheritDoc} */
        @Override
        public int getViewTypeCount () { return 4; }

        /**  {@inheritDoc} */
        @Override
        public int getItemViewType (int position) {
            if (position == getCount()-1)                        { return 3; } // 'more' button
            MessageAdapter m = getItem(position);
            if (m.getmState() == MessageAdapter.State.PROOFREAD) { return 0; } // proofread message
            if (m.isCommitted())                                 { return 2; } // committed translation
            else                                                 { return 1; } // translation message
        }

        /**  {@inheritDoc} */
        @Override
        public int getCount() {
            return super.getCount() + 1; //room for one more cell contains: "More"
        }

        /**  {@inheritDoc} */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            /* handles how line number "position" will look like */

            View v = convertView;
            TextView lblSourceText = null;
            int viewType = getItemViewType(position);
            final MessageAdapter msg = (viewType < 3) ? this.getItem(position) : null ;

            if (convertView == null) {

                // inflate the right view
                switch (viewType) {
                    case 0:         // proofread view
                        v = layoutInflater.inflate(R.layout.listitem_review, parent,false);
                        break;
                    case 1:         // translation view
                        v = layoutInflater.inflate(R.layout.listitem_translation, parent,false);
                        break;
                    case 2:         // committed translation view
                        v = layoutInflater.inflate(R.layout.listitem_committed, parent,false);
                        break;
                    case 3:         // "more" button
                        v = layoutInflater.inflate(R.layout.listitem_more, parent,false);
                        break;
                }
            }

            if (viewType < 2) {
                lblSourceText = (TextView) v.findViewById(R.id.lblSourceText);
                lblSourceText.setText(msg.getDefinition());
            }

            switch (viewType) {
                case 0:     // proofread view

                    // get all the relevant components
                    TextView lblTranslatedText = (TextView) v.findViewById(R.id.lblTranslatedText);
                    TextView lblAcceptText     = (TextView) v.findViewById(R.id.lblAcceptText);
                    View btnReject = v.findViewById(R.id.btnReject);
                    View btnAccept = v.findViewById(R.id.btnAccept);
                    View btnEdit   = v.findViewById(R.id.btnEdit);

                    // check right to left language
                    lblTranslatedText.setGravity(( msg.getLang().equals("he")
                                                   || msg.getLang().equals("ar")
                                                   || msg.getLang().equals("yi"))
                                                 ? Gravity.RIGHT // if LTR language
                                                 : Gravity.LEFT);

                    // bind action for reject
                    btnReject.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            //add to rejected messages DB
                            SQLiteDatabase db = getmDbHelper().getWritableDatabase();

                            // Create a new map of values, where column names are the keys
                            ContentValues values = new ContentValues();

                            // save only revision number.
                            values.put(RejectedMsgDbHelper.MsgEntry.COLUMN_NAME_ENTRY_ID,msg.getRevision());
                            if (db!=null) db.insert(RejectedMsgDbHelper.MsgEntry.TABLE_NAME, null,  values);
                            translations.remove(msg);
                        }
                    });

                    // bind action for edit
                    btnEdit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            msg.setmState(MessageAdapter.State.TRANSLATE);
                            new FetchHelpersTask((Activity)getContext(),msg).execute();
                            translations.notifyDataSetChanged();
                        }
                    });

                    // bind action for accept
                    btnAccept.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ReviewTranslationTask task = new ReviewTranslationTask(MainActivity.this, msg);
                            Utils.executeAsyncTask(task);
                        }
                    });

                    // populate view with message details
                    lblTranslatedText.setText(msg.getTranslation());

                    // show Accept Count - iff greater than 0.
                    int iACount = msg.getAcceptCount();
                    if (iACount > 0) {
                        String sACount = (new Integer(iACount)).toString();
                        lblAcceptText.setText(sACount);
                        lblAcceptText.setVisibility(View.VISIBLE);
                    } else {
                        lblAcceptText.setVisibility(View.INVISIBLE); // do not show accept count when zero
                    }

                    // show accept/reject only for selected cell.
                    v.findViewById(R.id.optionsLayout).setVisibility(position == selected ? View.VISIBLE
                                                                                          : View.GONE);
                    v.findViewById(R.id.btnEdit).setVisibility(position == selected ? View.VISIBLE
                                                                                    : View.GONE);
                    break;
                case 1:     // translation view
                    View infoBtn = v.findViewById(R.id.infoButton);
                    ListView sugListView = (ListView)v.findViewById(R.id.listSuggestions);
                    WebView infoWebView;
                    final ViewFlipper viewFlipper = (ViewFlipper)v.findViewById(R.id.viewFlipper);
                    if (convertView==null) {
                        ViewGroup infoLayout = (ViewGroup) v.findViewById(R.id.infoLayout);
                        infoWebView = new WebView(getContext());
                        infoWebView.setWebViewClient(new WebViewClient(){
                            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                return true;
                            }
                        });
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        params.setMargins(2, 2, 2, 2);
                        infoWebView.setLayoutParams(params);
                        infoWebView.setBackgroundColor(0); // transparent
                        infoWebView.setVerticalScrollBarEnabled(true);
                        WebSettings ws= infoWebView.getSettings();
                        ws.setTextSize(WebSettings.TextSize.SMALLER);
                        infoLayout.addView(infoWebView);
                        viewFlipper.setInAnimation(getContext(),R.anim.flipin);
                        viewFlipper.setOutAnimation(getContext(),R.anim.flipout);
                    } else {
                        infoWebView = (WebView)((ViewGroup)v.findViewById(R.id.infoLayout)).getChildAt(0);
                    }

                    sugListView.setAdapter(msg.getSuggestionsAdapter());
                    View delBtn = v.findViewById(R.id.deleteButton);
                    View canBtn = v.findViewById(R.id.cancelButton);
                    if (CURRENT_STATE == State.PROOFREAD) {
                        canBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                msg.setmState(MessageAdapter.State.PROOFREAD);
                                translations.notifyDataSetChanged();
                            }
                        });
                        delBtn.setVisibility(View.GONE);
                        canBtn.setVisibility(View.VISIBLE);
                    } else {
                        delBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                translations.remove(msg);
                            }
                        });
                        delBtn.setVisibility(View.VISIBLE);
                        canBtn.setVisibility(View.GONE);
                    }

                    infoBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            msg.flipInfo();
                            viewFlipper.showNext();
                        }
                    });

                    if (viewFlipper.getCurrentView().getTag().equals("info") != msg.isInfoNeedShown()) {
                        // need to change view without animation. it is sadly inefficient...
                        Animation in = viewFlipper.getInAnimation();
                        Animation out = viewFlipper.getOutAnimation();
                        viewFlipper.setInAnimation(null);
                        viewFlipper.setOutAnimation(null);
                        viewFlipper.showNext();
                        viewFlipper.setInAnimation(in);
                        viewFlipper.setOutAnimation(out);
                    }
                    infoBtn.setVisibility(msg.IsDocumentationExists() ? View.VISIBLE : View.GONE);

                    infoWebView.loadData(msg.getDocumentation(), "text/html; charset=UTF-8", null); // load documentation on card

                    // do heavy measurement to fix frame size
                    View suggestionsLayout = v.findViewById(R.id.listSuggestions);
                    int y = 0;
                    StaticLayout layout;
                    TextPaint paint = lblSourceText.getPaint();
                    for (String s : msg.getSuggestionsList()) {
                        layout = new StaticLayout(s, paint, suggestionsLayout.getWidth(),
                                Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                        y += layout.getHeight() + 6;
                    }

                    View suggestionsFrame = v.findViewById(R.id.suggestionsLayout);
                    ViewGroup.LayoutParams newParams = suggestionsFrame.getLayoutParams();
                    newParams.height = y + 94; // added height for input box and other spaces

                    /* due to unknown reason, in API 18 (and up? ) need to add more room */
                    if (Build.VERSION.SDK_INT >= 18) {
                        newParams.height += 22;
                    }
                    suggestionsFrame.setLayoutParams(newParams);

                    // set on click listener for suggestions
                    sugListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            if (i < adapterView.getCount() - 1) { // suggestion has been clicked
                                msg.setSavedInput(adapterView.getItemAtPosition(i).toString());
                                new SubmitTranslationTask((Activity) getContext(), msg).execute();
                                notifyDataSetChanged();
                            }
                        }
                    });
                    break;
                case 2:     // committed translation view
                    TextView lblCommittedText = (TextView) v.findViewById(R.id.lblCommittedText);
                    lblCommittedText.setGravity((msg.getLang().equals("he") || msg.getLang().equals("ar"))
                            ? Gravity.RIGHT
                            : Gravity.LEFT );
                    lblCommittedText.setText(msg.getSavedInput());
                    break;
            }
            return v;
        }
    }

    /**
     * Executes the messages fetch request task
     */
    public void fetchTranslations() {

        // just applying FetchTranslationsTask in a friendlier way
        FetchTranslationsTask fetchTranslations =
                new FetchTranslationsTask(this, CUR_LANG, CUR_PROJECT, FETCH_SIZE,
                                          MessageAdapter.State.values()[CURRENT_STATE.ordinal()],
                                          TranslateWikiApp.MAX_NO_FETCH_TRIALS);
        fetchTranslations.execute();
    }

    /**
     * update preferences and calls to fetch messages from api
     */
    public void refreshTranslations() { // exactly as "fetchTranslations", but clear first
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        CUR_LANG    = sharedPref.getString(getString(R.string.langugage_key),
                                          getString(R.string.lang_pref_default));
        CUR_PROJECT = sharedPref.getString(getString(R.string.projects_key),
                                          getString(R.string.proj_pref_default));
        FETCH_SIZE  = sharedPref.getInt(getString(R.string.fetch_size_key),
                                       Integer.parseInt(getString(R.string.fetch_size_pref_default)));
        MAX_MESSAGE_LENGTH = sharedPref.getInt(getString(R.string.max_message_length_key),
                                       Integer.parseInt(getString(R.string.max_length_pref_default)));
        offset = 0;
        translations.clear();
        fetchTranslations();
    }

    /**
     * inflates a view for the input view
     *
     * @param parent parent view
     * @param msg    a message that the input related to
     * @return input view including all the need feature to take care of.
     */
    public View getViewForInput(ViewGroup parent, final MessageAdapter msg) {
        View view = layoutInflater.inflate(R.layout.listitem_input, parent, false);
        final Button sendBtn  = (Button) view.findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SubmitTranslationTask(that, msg).execute();
                translations.notifyDataSetChanged();
            }
        });

        EditText et = (EditText) view.findViewById(R.id.editText);
        String savedInput = msg.getSavedInput();
        boolean bool = (savedInput != null) && (savedInput.length() > 0);
        et.setText( bool ? savedInput : "" );
        sendBtn.setEnabled(bool);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                msg.setSavedInput(editable.toString());
                sendBtn.setEnabled( editable.toString().length() > 0 );
            }
        });
        return view;
    }

    /** {@inheritDoc} */
    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        super.onAuthCookieAcquired(authCookie);
        app.getApi().setAuthCookie(authCookie);
        refreshTranslations();
    }

    /** {@inheritDoc} */
    @Override
    protected void onAuthFailure() {
        super.onAuthFailure();
        Toast failureToast = Toast.makeText(this, net.translatewiki.app.R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    /** {@inheritDoc} */
    @Override
    protected void onStart() {
        super.onStart();
        Bundle extras =  getIntent().getExtras();

        // check whether opened from changing preferences
        if (extras !=null && extras.getBoolean("should_refresh_translations")) {
            selected = -1;
            refreshTranslations();
        }
    }

    /** {@inheritDoc} */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //Used to put icons on action bar

        // Inflate the menu items for use in the action bar
        com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main, menu);
        boolean isLight = SampleList.THEME == com.actionbarsherlock.R.style.Theme_Sherlock_Light;
        menu.findItem(R.id.action_proofread).setTitle((CURRENT_STATE == State.PROOFREAD) ? "Translate" : "Proofread");

        MenuItem settingsBtn = menu.add("Settings");
        settingsBtn.setIcon(isLight ? R.drawable.settings_icon_inverse : R.drawable.settings_icon).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        settingsBtn.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                openSettings();
                return true;
            }
        });
        menu.findItem(R.id.sub_menu_item).setVisible(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            menu.findItem(R.id.sub_menu_item).setVisible(false);
            getActionBar().setHomeButtonEnabled(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    /** {@inheritDoc} */
    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sub_menu_item:
                break;
            case R.id.action_settings:
                openSettings();
                break;
            case R.id.action_proofread:
                /* falls through */
            default:
                Boolean b = CURRENT_STATE == State.PROOFREAD;
                setTitle     (b ? "Translate" : "Proofread");
                item.setTitle(b ? "Proofread" : "Translate");
                switchState(b ? State.TRANSLATE : State.PROOFREAD);
                break;
        }
        return true;
    }

    /**
     * set activity state into proofread or Translate
     * @param newState the new state to set
     */
    public void switchState(State newState) {
        CURRENT_STATE = newState;
        refreshTranslations();
        SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        sharedPrefEditor.putInt(getString(R.string.state_key), CURRENT_STATE.ordinal());
        sharedPrefEditor.commit(); // save preference
    }

    /**
     * call preferences activity
     */
    private void openSettings() {
        Intent intent =  new Intent(this,TWPreferenceActivity.class);
        startActivity(intent);
    }

    /**
     * @return database helper for rejected messages
     */
    public RejectedMsgDbHelper getmDbHelper() {
        if (mDbHelper == null)
            mDbHelper = new RejectedMsgDbHelper(this);
        return mDbHelper;
    }

    /**  {@inheritDoc} */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        /* happens on device rotation - needs to inflate UI an update adapters */
        super.onConfigurationChanged(newConfig);
        initializeUI();
        notifyTranslationsOnNewThread();
    }

    /**
     *  inflates UI for this activity
     */
    private void initializeUI() {
        layoutInflater = getLayoutInflater();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_color));
        }
        setContentView(net.translatewiki.app.R.layout.activity_main);
        msgListView = (ListView) findViewById(net.translatewiki.app.R.id.listTranslations);
        msgListView.setAdapter(translations);

        setTitle(CURRENT_STATE == State.PROOFREAD ? "Proofread" : "Translate");
    }
}
