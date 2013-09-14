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
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by orsa on 20/8/13.
 */
public abstract class SearchableListPreference extends DialogPreference implements SharedPreferences.OnSharedPreferenceChangeListener {

    ItemListAdapter itemListAdapter;
    SearchView sv;
    ListView lv;
    String initialValue;

    public abstract boolean showRefreshBtn();
    public abstract PairList getListForSection(int section);
    public abstract int getSizeOfSection(int section);
    public abstract String getDefaultValue();
    public abstract Integer getNumberOfSections();
    public void onClickRefresh() {}

    public SearchableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        String currentVal = getPersistedString(getDefaultValue());
        itemListAdapter = new ItemListAdapter(context,currentVal);
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
    }

    protected List<String> extractEntries(){
        List<String> entryList = new ArrayList<String>();
        for (int i=0; i<getNumberOfSections(); i++)
        for (Pair<String, String> s : getListForSection(i)) {
            entryList.add(s.first);
        }
        return entryList;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        View refreshBtn = view.findViewById(R.id.refreshBtn);
        refreshBtn.setVisibility(showRefreshBtn() ? View.VISIBLE : View.GONE);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickRefresh();
            }
        });

        sv = (SearchView)view.findViewById(R.id.prefItemListSearchView);

        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) { return false; }

            @Override
            public boolean onQueryTextChange(String s) {
                if (TextUtils.isEmpty(s)) {
                    itemListAdapter.getFilter().filter("");
                    lv.clearTextFilter();
                } else {
                    itemListAdapter.getFilter().filter(s.toString());
                }
                return true;
            }
        });

        sv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b)
                {
                    lv.clearTextFilter();
                }
            }
        });

        sv.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                lv.clearTextFilter();
                return true;
            }
        });

        lv = (ListView)view.findViewById(R.id.prefItemListView);
        lv.setAdapter(itemListAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RadioButton b;

                int selectedIndex = itemListAdapter.getPosition(itemListAdapter.selectedEntry);
                if (selectedIndex>=0)
                {
                    int lowBound = adapterView.getFirstVisiblePosition();
                    int highBound = adapterView.getLastVisiblePosition();
                    if (selectedIndex >= lowBound && selectedIndex <= highBound)
                    {
                        View v = adapterView.getChildAt(selectedIndex - lowBound);
                        assert v != null;
                        b = (RadioButton)(v.findViewById(R.id.list_item_radioButton));
                        b.setChecked(false);
                    }
                }
                itemListAdapter.selectedEntry = itemListAdapter.getItem(i);
                b = (RadioButton)view.findViewById(R.id.list_item_radioButton);
                b.setChecked(true);
                //itemListAdapter.notifyDataSetChanged();
            }
        });
        itemListAdapter.clear();
        itemListAdapter.addAll(extractEntries());
        itemListAdapter.getFilter().filter("");
        lv.clearTextFilter();
        itemListAdapter.notifyDataSetChanged();
        itemListAdapter.setSelectedEntry(getPersistedString(getDefaultValue()));
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
    {
        if ( restorePersistedValue ) {
            String def = ( defaultValue instanceof String )
                    ? (String)defaultValue
                    : ( defaultValue!=null ? defaultValue.toString() : getDefaultValue());
            initialValue = getPersistedString(def);
        }
        else initialValue = (String)defaultValue;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        String s = a.getString(0);
        return s;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            String selEnt = itemListAdapter.selectedEntry;
            String s = getValueOfEntry(selEnt);
            if (s!=null){
                persistString(s);
            }
        }
        sv.getOnFocusChangeListener().onFocusChange(sv,false);
    }

    public String getValueOfEntry(String s){
        String v;
        for (int i=0; i<getNumberOfSections(); i++){
            v = getListForSection(i).getValueOfEntry(s);
            if (v!=null)
                return v;
        }
        return ""; // TODO: choose default value in case not found
    }

    /**
     * handles the way preference items are reflected for the UI
     */
    public class ItemListAdapter extends ArrayAdapter<String> {

        public String selectedEntry;
        private int[] filteredInSec = new int[getNumberOfSections()];

        public ItemListAdapter(Context context, String selectedVal) {
            super ( context,
                    R.layout.list_pref_item,
                    R.id.list_item_radioButton,
                    extractEntries());
            for (int i=0; i<getNumberOfSections();i++)
            {
                filteredInSec[i] = -1;
            }
        }

        @Override
        public int getViewTypeCount (){ return 1; }

        @Override
        public int getItemViewType (int position){ return 0; }

        @Override
        // handles how line number "position" will look like.
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position,convertView,parent);
            assert v != null;
            RadioButton b = (RadioButton)v.findViewById(R.id.list_item_radioButton);
            if (b!=null)
                b.setChecked(getItem(position).equals(selectedEntry));

            ImageView img = (ImageView)v.findViewById(R.id.specialImg);
            if (img!=null)
                if (position<getFilteredSizeOfSection(0)+getFilteredSizeOfSection(1)){
                    img.setImageResource( position<getFilteredSizeOfSection(0)
                                        ? android.R.drawable.ic_menu_recent_history
                                        : R.drawable.local);
                    img.setVisibility(View.VISIBLE);
                }
                else
                    img.setVisibility(View.GONE);

            return v;
        }

        public String setSelectedEntry(String s){
            for (int i=0; i<getNumberOfSections(); i++){
                for (Pair<String, String> pair : getListForSection(i)) {
                    if (s.equals(pair.second)) {
                        selectedEntry = pair.first;
                        return selectedEntry;
                    }
                }
            }
            return "";
        }

        @Override
        public android.widget.Filter getFilter(){
            return new android.widget.Filter(){
                @Override
                protected android.widget.Filter.FilterResults performFiltering(CharSequence constraint) {
                    constraint = constraint.toString().toLowerCase();
                    android.widget.Filter.FilterResults result = new android.widget.Filter.FilterResults();
                    List<String> entries = extractEntries();
                    if (constraint != null && constraint.toString().length() > 0) {
                        List<String> found = new ArrayList<String>();

                        List<String> candidates;
                        for (int i=0; i<getNumberOfSections();i++)
                        {
                            filteredInSec[i]=0;
                            candidates = getListForSection(i).getEntries();
                            for(String item: candidates)
                            {
                                if(item.toString().toLowerCase().contains(constraint)){
                                    found.add(item);
                                    filteredInSec[i]++;
                                }
                            }
                        }
                        result.values = found;
                        result.count = found.size();
                    }else {
                        for (int i=0; i<getNumberOfSections(); i++) {
                            filteredInSec[i]=-1;
                        }
                        result.values = entries;
                        result.count = entries.size();
                    }
                    return result;

                }
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    for (String item : (List<String>) results.values) {
                        add(item);
                    }
                    notifyDataSetChanged();
                }
            };
        }

        public int getFilteredSizeOfSection(int i){
            return (filteredInSec[i]==-1 ? getSizeOfSection(i) : filteredInSec[i]);
        }
    }
}
