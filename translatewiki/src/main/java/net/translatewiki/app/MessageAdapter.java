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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * A message for translation or proofread, adapted for use as an AdapterView object
 * by keeping its state
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class MessageAdapter extends Message {

    /** enumeration of a message states */
    public enum State { UNDEFINED, PROOFREAD, TRANSLATE }

    private SuggestionListAdapter suggestionsAdapter;

    /** specifies general state for message. */
    private State    mState;

    /** tells whether this message requires showing it's info. */
    private boolean infoNeedShown;

    /** indicates the user submitted a translation, and not in edit mode. */
    private boolean committed;

    /** the input text from the user */
    private String savedInput;

    public MessageAdapter(Context context, String key, String mTitle, String mGrupe,
                          String  lang, String definition, String translation, String revision,
                          int mAcceptCount, State  mState) {
        super(key, mTitle, mGrupe, lang, definition, translation, revision, mAcceptCount);
        this.mState = mState;
        infoNeedShown = false;
        committed = false;
        suggestionsAdapter = new SuggestionListAdapter(context,this);
    }

    /**
     * @return suggestions adapter for this message.
     */
    public SuggestionListAdapter getSuggestionsAdapter() {
        return suggestionsAdapter;
    }

    /**
     * @param suggestionsAdapter  suggestions adapter for this message.
     */
    public void setSuggestionsAdapter(SuggestionListAdapter suggestionsAdapter) {
        this.suggestionsAdapter = suggestionsAdapter;
    }

    /**
     * @return the state of the message.
     */
    public State getmState() {
        return mState;
    }

    /**
     * @param mState state of the message.
     */
    public void setmState(State mState) {
        this.mState = mState;
    }

    /**
     * @return true iff info display is required.
     */
    public boolean isInfoNeedShown() {
        return infoNeedShown;
    }

    /**
     * @param infoNeedShown true iff info display is required.
     */
    public void setInfoNeedShown(boolean infoNeedShown) {
        this.infoNeedShown = infoNeedShown;
    }

    /**
     * flip 'info display required' to the opposite value
     */
    public void flipInfo() {
        setInfoNeedShown(!infoNeedShown);
    }

    /**
     * @return true iff the user submitted a translation, and not reopened for edit.
     */
    public boolean isCommitted() {
        return committed;
    }

    /**
     * @param committed  true iff the user submitted a translation, and not reopened for edit.
     */
    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    /**
     * @return the input text from the user.
     */
    public String getSavedInput() {
        return savedInput;
    }

    /**
     * @param savedInput  the input text from the user.
     */
    public void setSavedInput(String savedInput) {
        this.savedInput = savedInput;
    }

    /**
     * Adapter for suggestions list, handles the way suggestions are reflected for the UI
     *
     * @author      Or Sagi
     * @version     %I%, %G%
     * @since       1.0
     */
    public class SuggestionListAdapter extends ArrayAdapter<String> {

        /* the mesage related to this suggestions */
        private MessageAdapter msg;

        public SuggestionListAdapter(Context context, MessageAdapter msg) {
            super(context, R.layout.listitem_suggestion,R.id.lblSuggestionText, msg.getSuggestionsList());
            this.msg = msg;
        }

        @Override
        public int getViewTypeCount () { return 2; }

        @Override
        public int getItemViewType (int position) {
            return (position == getCount() - 1) ? 1 : 0; // two types: "last item"-1 or "anything else"-0
        }

        @Override
        public int getCount(){
            return super.getCount()+1; //room for one more cell which is the input area
        }

        /* handles how line number "position" will look like. */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (getItemViewType(position) == 1) {  // it's the last line. populate it with input area
                view = convertView != null
                       ? convertView
                       : ((MainActivity)getContext()).getViewForInput(parent, msg);
            } else {
                view = super.getView(position,convertView,parent); // the usual, just use "listitem_suggestion"
                TextView tv = (TextView) view.findViewById(R.id.lblSuggestionText);
                tv.requestFocusFromTouch();
            }
            return view;
        }
    }
}
