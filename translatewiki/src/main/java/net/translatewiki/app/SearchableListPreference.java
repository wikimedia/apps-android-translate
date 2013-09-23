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
 * abstract list preference which allows search and filtering.
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public abstract class SearchableListPreference extends DialogPreference implements SharedPreferences.OnSharedPreferenceChangeListener {

    protected ItemListAdapter itemListAdapter;
    protected SearchView      searchView;
    protected ListView        listView;
    protected String          initialValue;

    /**
     * tells the number of sections in the list. It helps to to display numerous
     * lists easily on the same list, with separation of the item types by icons.
     *
     * @return number of sections for this list.
     */
    public abstract Integer getNumberOfSections();

    /**
     * @param section section index number. first section is 0.
     * @return A {@link net.translatewiki.app.PairList} for the specified section.
     */
    public abstract PairList getListForSection(int section);

    /**
     * @param section section index number. first section is 0.
     * @return number of items in the specified section.
     */
    public abstract int getSizeOfSection(int section);

    /**
     * @return a value to be selected in the list by default.
     */
    public abstract String getDefaultValue();

    /**
     * tells whether showing a refresh button is required.
     *
     * @return true if showing a refresh button is required.
     */
    public abstract boolean showRefreshBtn();

    /**
     * action to be done on the event of pushing the refresh button
     * (if shown).
     */
    public void onClickRefresh() {} // empty by default

    public SearchableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        String currentVal = getPersistedString(getDefaultValue());
        itemListAdapter = new ItemListAdapter(context);
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * simplifies all pair-lists in all sections to one simple list of entries
     *
     * @return list of all entries in all sections by the same order.
     */
    protected List<String> extractEntries(){
        List<String> entryList = new ArrayList<String>();
        for (int i=0; i<getNumberOfSections(); i++) {
            entryList.addAll(getListForSection(i).getEntries());
        }
        return entryList;
    }

    /** {@inheritDoc} */
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

        searchView = (SearchView)view.findViewById(R.id.prefItemListSearchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) { // happens when push 'submit'
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (TextUtils.isEmpty(s)) {
                    itemListAdapter.getFilter().filter("");
                    listView.clearTextFilter(); // clear filtering on empty text
                } else {
                    itemListAdapter.getFilter().filter(s.toString()); // do the filtering
                }
                return true;
            }
        });

        searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) { // if we lost focus clear filter
                    listView.clearTextFilter();
                }
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                listView.clearTextFilter();
                return true;
            }
        });

        listView = (ListView)view.findViewById(R.id.prefItemListView);
        listView.setAdapter(itemListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RadioButton radioButton;
                int selectedIndex = itemListAdapter.getPosition(itemListAdapter.selectedEntry);
                if (selectedIndex >= 0) {

                    // check the previously selected button indeed viewable
                    int lowBound = adapterView.getFirstVisiblePosition();
                    int highBound = adapterView.getLastVisiblePosition();
                    if (selectedIndex >= lowBound && selectedIndex <= highBound) {
                        View v = adapterView.getChildAt(selectedIndex - lowBound);
                        radioButton = (RadioButton) (v.findViewById(R.id.list_item_radioButton));
                        radioButton.setChecked(false);
                    }
                }

                // update the newly selected
                itemListAdapter.selectedEntry = itemListAdapter.getItem(i);
                radioButton = (RadioButton) view.findViewById(R.id.list_item_radioButton);
                radioButton.setChecked(true);
            }
        });
        itemListAdapter.clear();
        itemListAdapter.addAll(extractEntries());
        itemListAdapter.getFilter().filter("");
        listView.clearTextFilter();
        itemListAdapter.notifyDataSetChanged();
        itemListAdapter.setSelectedEntry(getPersistedString(getDefaultValue()));
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            String def = ( defaultValue instanceof String )
                         ? (String)defaultValue
                         : (defaultValue != null ? defaultValue.toString() : getDefaultValue());
            initialValue = getPersistedString(def);
        }
        else initialValue = (String)defaultValue;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(0);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            String selEnt = itemListAdapter.selectedEntry;
            String s = getValueOfEntry(selEnt);
            if (s != null) {
                persistString(s);
            }
        }
        searchView.getOnFocusChangeListener().onFocusChange(searchView,false); //lose focus of search
    }

    /**
     * look for a value of specific entry in all sections
     *
     * @param entry the entry to look for.
     * @return the first value for the specified entry, or empty string if not found.
     */
    public String getValueOfEntry(String entry) {
        String val;
        for (int i=0; i<getNumberOfSections(); i++){
            val = getListForSection(i).getValueOfEntry(entry);
            if (val != null)
                return val;
        }
        return "";
    }

    /**
     * ArrayAdapter that handles the way preference items are reflected for the UI.
     *
     * @author      Or Sagi
     * @version     %I%, %G%
     * @since       1.0
     */
    public class ItemListAdapter extends ArrayAdapter<String> {

        /* specifies for each section the number of items in filtered mode, or -1 if not filtered */
        private int[] filteredInSec = new int[getNumberOfSections()];

        private String selectedEntry;

        /**
         * @return the selected entry in the list.
         */
        public String getSelectedEntry() {
            return selectedEntry;
        }

        /**
         * @param val value of the entry to be selected
         * @return the entry that actually selected, or empty string if not found
         */
        public String setSelectedEntry(String val) {
            for (int i=0; i<getNumberOfSections(); i++) {
                for (Pair<String, String> pair : getListForSection(i)) {
                    if (val.equals(pair.second)) {
                        selectedEntry = pair.first;
                        return selectedEntry;
                    }
                }
            }
            return "";
        }

        public ItemListAdapter(Context context) {
            super (context, R.layout.list_pref_item, R.id.list_item_radioButton, extractEntries());
            for (int i=0; i<getNumberOfSections();i++) {
                filteredInSec[i] = -1;
            }
        }

//        @Override
//        public int getViewTypeCount (){ return 1; }
//
//        @Override
//        public int getItemViewType (int position){ return 0; }

        /** {@inheritDoc} */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position,convertView,parent);
            RadioButton radioButton = (RadioButton)view.findViewById(R.id.list_item_radioButton);
            radioButton.setChecked(getItem(position).equals(selectedEntry));
            ImageView img = (ImageView)view.findViewById(R.id.specialImg);

            // show 'specialImg' only at sections 0 or 1
            if (position < getFilteredSizeOfSection(0) + getFilteredSizeOfSection(1)) {
                img.setImageResource(position < getFilteredSizeOfSection(0)
                                    ? android.R.drawable.ic_menu_recent_history
                                    : R.drawable.local);
                img.setVisibility(View.VISIBLE);
            } else {
                img.setVisibility(View.GONE);
            }
            return view;
        }

        /** {@inheritDoc} */
        @Override
        public android.widget.Filter getFilter() {
            return new android.widget.Filter() {
                @Override
                protected android.widget.Filter.FilterResults performFiltering(CharSequence constraint) {
                    constraint = constraint.toString().toLowerCase();
                    android.widget.Filter.FilterResults result = new android.widget.Filter.FilterResults();
                    List<String> entries = extractEntries();
                    if (constraint != null && constraint.toString().length() > 0) {
                        List<String> found = new ArrayList<String>();   // items survived filtering
                        List<String> candidates;                        // items to look in
                        for (int i=0; i<getNumberOfSections(); i++) {
                            filteredInSec[i]=0; // 'activate' filtering for this section
                            candidates = getListForSection(i).getEntries();
                            for(String item: candidates) {
                                if(item.toString().toLowerCase().contains(constraint)) {
                                    found.add(item);
                                    filteredInSec[i]++;
                                }
                            }
                        }
                        result.values = found;
                        result.count = found.size();
                    } else { // search is empty so deactivate filtering
                        for (int i=0; i<getNumberOfSections(); i++) {
                            filteredInSec[i] = -1;
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

        /**
         *
         * @param i section index number to check, starting from 0.
         * @return number of items in the section to be shown, after filtering if any.
         */
        public int getFilteredSizeOfSection(int i) {
            return (filteredInSec[i]==-1 ? getSizeOfSection(i) : filteredInSec[i]);
        }
    }
}
