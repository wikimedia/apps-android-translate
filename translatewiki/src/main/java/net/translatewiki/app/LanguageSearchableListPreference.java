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
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Pair;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * concrete list preference which allows search and filtering for projects,
 * including save the data in files.
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class LanguageSearchableListPreference extends SearchableListPreference {

    // constants
    private static final int MAX_RECENTS = 4;
    private static final int MAX_LOCALS = 3;
    private static final String REC_ENTRIES_FILENAME = "rec_languages_entries_file";
    private static final String REC_VALUES_FILENAME  = "rec_languages_values_file" ;

    private static Context staticContext; // needed for statically save/load to to/from files
    private static PairList dataList = new PairList();
    private static PairList recentList = new PairList();

    public LanguageSearchableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        staticContext = context;
        recentList = loadRecentList();
        dataList = loadFullList();
        setSummaryOfValue();
    }

    /**
     * set the current language as a summery, taken from sharedPreferences or
     * from persisted value, after conversion value to entry.
     */
    public void setSummaryOfValue() {
        String summaryVal = PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getString(getContext().getString(R.string.langugage_key),"");
        if (summaryVal == null || summaryVal.length() == 0)
            summaryVal = getPersistedString(getDefaultValue());

        String summaryEntry = recentList.getEntryOfValue(summaryVal);
        if (summaryEntry == null)
            summaryEntry = dataList.getEntryOfValue(getPersistedString(getDefaultValue()));

        setSummary(summaryEntry != null ? summaryEntry : "");
    }

    /** {@inheritDoc} */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getContext().getString(R.string.langugage_key)))
            setSummaryOfValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean showRefreshBtn() { return false; }

    /** {@inheritDoc} */
    @Override
    public int getSizeOfSection(int section) {
        switch (section) {
            case 0:
                return recentList.size();
            case 1:
                return 0;
            case 2:
                return dataList.size();
        }
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public PairList getListForSection(int section) {
        switch (section) {
            case 0:
                return recentList;
            case 1:
                return new PairList();
            case 2:
                return dataList;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String selEnt = itemListAdapter.getSelectedEntry();
            String s = getValueOfEntry(selEnt);
            if (selEnt != null && s != null){
                recentList.remove(Pair.create(selEnt, s));
                recentList.add(0, Pair.create(selEnt, s));
                while (recentList.size() > MAX_RECENTS){
                    recentList.remove(MAX_RECENTS);
                }
                dataList = loadFullList();
                saveRecentList();
                itemListAdapter.clear();
                itemListAdapter.addAll(extractEntries());
            }
        }
        searchView.getOnFocusChangeListener().onFocusChange(searchView, false);
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultValue() { return "en"; }

    /** {@inheritDoc} */
    @Override
    public Integer getNumberOfSections() { return 3; }

    /**
     * load recent languages entry list and value list into one unified {@link PairList}
     *
     * @return list of entry-value generated from the files
     */
    private PairList loadRecentList() {
        PairList list = new PairList();
        FileInputStream fis;
        try {
            fis = getContext().openFileInput(REC_ENTRIES_FILENAME);
            List<String> loadedEntryList = extractListFromFile(fis);
            fis = getContext().openFileInput(REC_VALUES_FILENAME);
            List<String> loadedValueList = extractListFromFile(fis);
            for (int i=0; i<Math.min(loadedEntryList.size(), loadedValueList.size());i++) {
                list.add(Pair.create(loadedEntryList.get(i), loadedValueList.get(i)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /* a helper for extracting a list from raw data in file */
    private List<String> extractListFromFile(FileInputStream fis) throws IOException {
        byte[] b;
        b = new byte[fis.available()];
        fis.read(b);
        fis.close();
        return (b.length > 2)
               ? new ArrayList<String>(Arrays.asList((new String(b)).substring(1, b.length - 1).split(", ")))
               : new ArrayList<String>();
    }

    /**
     * saves recent languages entry list and value list into two files.
     */
    private static void saveRecentList() {
        FileOutputStream fos;
        try {
            fos = staticContext.openFileOutput(REC_ENTRIES_FILENAME, Context.MODE_PRIVATE);
            String s = recentList.getEntries().toString();
            fos.write(s.getBytes());
            fos.close();
            fos = staticContext.openFileOutput(REC_VALUES_FILENAME, Context.MODE_PRIVATE);
            s=recentList.getValues().toString();
            fos.write(s.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * loads full languages list and value list into one unified {@link PairList}
     *
     * @return language list in form of entry-value
     */
    private PairList loadFullList() {
        PairList list = new PairList();
        String[] entries =  getContext().getResources().getStringArray(R.array.pref_languages_list_titles);
        String[] values  =  getContext().getResources().getStringArray(R.array.pref_languages_list_values);
        for (int i=0 ; i<entries.length ; i++) {
            list.add(Pair.create(entries[i],values[i]));
        }
        list.removeAll(recentList);
        return list;
    }

    /**
     * delete the saved data which is the recent languages files
     */
    public static void deleteSavedData() {
        recentList = new PairList();
        saveRecentList(); // override files with an empty list.
    }
}
