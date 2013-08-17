package net.translatewiki.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import java.util.List;

/**
 * Created by orsa on 5/8/13.
 */


public class MessageAdapter extends Message {

    public SuggestionListAdapter suggestionsAdapter;
    public AdapterView suggestionsAdapterView;

    private boolean infoNeedShown;
    private boolean committed;             // indicates the user sent translation, and not in edit mode.
    public String savedInput;              // to hold the input text so it won't disappear on refresh

    //CTOR
    public MessageAdapter(Context context, String key, String mTitle, String mGrupe, String lang, String definition, String translation, String revision, int mAcceptCount, int mState) {
        super(key, mTitle, mGrupe, lang, definition, translation, revision, mAcceptCount, mState);

        infoNeedShown=false;
        committed=false;
        suggestionsAdapter = new SuggestionListAdapter(context,this);
    }

    public boolean isInfoNeedShown() {
        return infoNeedShown;
    }

    public void setInfoNeedShown(boolean infoNeedShown) {
        this.infoNeedShown = infoNeedShown;
    }

    public void flipInfo() {
        setInfoNeedShown(!infoNeedShown);
    }

    public boolean isCommitted() {
        return committed;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    /**
     * handles the way suggestions are reflected for the UI
     */
    public class SuggestionListAdapter extends ArrayAdapter<String> {

        private MessageAdapter m;

        public SuggestionListAdapter(Context context, MessageAdapter m ) {
            super(context, R.layout.listitem_suggestion,R.id.lblSuggestionText,m.getSuggestionsList());
            this.m = m;
        }

        @Override
        public int getViewTypeCount (){
            return 2;
        }

        @Override
        public int getItemViewType (int position){

            if (position==getCount()-1)
                return 1;
            else
                return 0;
        }

        @Override
        public int getCount(){
            return super.getCount()+1; //room for one more cell which is the input area
        }

        @Override
        // handles how line number "position" will look like.
        public View getView(int position, View convertView, ViewGroup parent) {

            View v;
            if (position == getCount()-1){
                // means it's the last line. we populate it with input area

                return convertView!=null ? convertView
                                         : ((MainActivity)getContext()).getViewForInput(parent, m);
            }
            //else
            return super.getView(position,convertView,parent); // the usual, just use "listitem_suggestion"
        }
    }
}
