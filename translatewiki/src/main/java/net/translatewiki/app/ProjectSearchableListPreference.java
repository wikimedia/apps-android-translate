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
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.Toast;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.mediawiki.auth.MWApiApplication;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * concrete list preference which allows search and filtering for projects,
 * including fetch them from api server and save the data in files.
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class ProjectSearchableListPreference extends SearchableListPreference {

    private static final int MAX_RECENTS = 4;
    private static Context staticContext = null;
    private static ProjectSearchableListPreference that;

    static String  ENTRIES_FILENAME     = "projects_entries_file"    ;
    static String  VALUES_FILENAME      = "projects_values_file"     ;
    static String  REC_ENTRIES_FILENAME = "rec_projects_entries_file";
    static String  REC_VALUES_FILENAME  = "rec_projects_values_file" ;

    private static PairList dataList = new PairList();      // <entry,value>
    private static PairList recentList = new PairList();

    public ProjectSearchableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        staticContext = context;
        that = this;
        recentList  = loadList(REC_ENTRIES_FILENAME,REC_VALUES_FILENAME);
        dataList    = loadList(ENTRIES_FILENAME, VALUES_FILENAME);
        if (dataList==null || dataList.size()==0){ // in that case we fetch project from server automatically
            refreshList();
        }
        setSummaryofValue();
        dataList.removeAll(recentList);
    }

    public void setSummaryofValue(){
        String summaryVal = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getContext().getString(R.string.projects_key), "");
        if (summaryVal==null || summaryVal.length()==0)
            summaryVal = getPersistedString(getDefaultValue());

        String summaryEntry = recentList.getEntryOfValue(summaryVal);
        if (summaryEntry==null)
            summaryEntry = dataList.getEntryOfValue(getPersistedString(getDefaultValue()));

        setSummary(summaryEntry!=null? summaryEntry : "" );
    }

    /** {@inheritDoc} */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getContext().getString(R.string.projects_key)))
            setSummaryofValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean showRefreshBtn() { return true; }

    /** {@inheritDoc} */
    @Override
    public PairList getListForSection(int section) {
        switch (section) {
            case 0:
                return recentList;     // section of recently used projects
            case 1:
                return new PairList(); // we don't use this section for projects
            case 2:
               return dataList;        // section of all the other projects
        }
        return null;
    }

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
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String selEnt = itemListAdapter.getSelectedEntry();
            String s = getValueOfEntry(selEnt);
            if (selEnt != null && s != null) {
                recentList.remove(Pair.create(selEnt, s));  // avoid duplication
                recentList.add(0, Pair.create(selEnt, s));  // insert at the beginning
                while (recentList.size() > MAX_RECENTS) {   // remove outstanding items from the end
                    recentList.remove(MAX_RECENTS);
                }
                dataList = loadList(ENTRIES_FILENAME, VALUES_FILENAME); // load from file
                dataList.removeAll(recentList); // filter recents from the general list

                // save recents in file
                saveList(REC_ENTRIES_FILENAME,REC_VALUES_FILENAME,recentList.getEntries(),recentList.getValues());

                // update adapter
                itemListAdapter.clear();
                itemListAdapter.addAll(extractEntries());
            }
        }
        searchView.getOnFocusChangeListener().onFocusChange(searchView,false);
    }

    /**
     * refreshes the project list if empty.
     *
     * @return the current entry list of the general section, before refreshing.
     */
    public List<String> getEntryList() {
        ArrayList<String> entryList = new ArrayList<String>();
        if (dataList == null || dataList.size() == 0) {
            refreshList();
        } else {
            entryList.addAll(dataList.getEntries());
        }
        return entryList;
    }

    /**
     * refreshes the project list if empty.
     *
     * @return the current value list of the general section, before refreshing.
     */
    public List<String> getValueList() {
        ArrayList<String> valueList = new ArrayList<String>();
        if (dataList == null || dataList.size() == 0) {
            refreshList();
        } else {
            valueList.addAll(dataList.getValues());
        }
        return valueList;
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultValue() { return "!recent"; }

    /** {@inheritDoc} */
    @Override
    public Integer getNumberOfSections() { return 3; }

    /** {@inheritDoc} */
    @Override
    public void onClickRefresh() {
        refreshList(); // fetch projects from api
    }

    /**
     * calls the fetch projects task to request project list from the api sever.
     */
    public void refreshList() {
        new FetchProjectsTask().execute();
    }

    /**
     * saves entry list and value list into two files.
     *
     * @param key_e key for a file to save entry list
     * @param key_v key for a file to save value list
     * @param entryList entry list to save
     * @param valueList value list to save
     */
    public static void saveList(String key_e , String key_v, List<String> entryList,List<String> valueList) {
        FileOutputStream fos;
        try {
            fos = staticContext.openFileOutput(key_e, Context.MODE_PRIVATE);
            fos.write(entryList.toString().getBytes());
            fos.close();

            fos = staticContext.openFileOutput(key_v, Context.MODE_PRIVATE);
            fos.write(valueList.toString().getBytes());
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * load entry list and value list into one unified {@link PairList}
     *
     * @param key_e key for a file to load entry list
     * @param key_v key for a file to load value list
     * @return list of entry-value generated from the specified files
     */
    public PairList loadList(String key_e , String key_v) {
        PairList list = new PairList();
        FileInputStream fis;
        byte[] b;
        try {
            fis = getContext().openFileInput(key_e);
            b = new byte[fis.available()];
            fis.read(b);
            fis.close();
            List<String> loadedEntryList, loadedValueList;
            String tempStr, manipulatedStr;
            if (b.length > 2) {
                tempStr = new String(b).trim();
                manipulatedStr = tempStr.substring(1, tempStr.length() - 1).trim();
                loadedEntryList = new ArrayList<String>(Arrays.asList(manipulatedStr.split(", ")));
            } else {
                loadedEntryList = new ArrayList<String>();
            }
            fis = getContext().openFileInput(key_v);
            b = new byte[fis.available()];
            fis.read(b);
            fis.close();
            if (b.length > 2) {
                tempStr = new String(b).trim();
                manipulatedStr = tempStr.substring(1, tempStr.length() - 1).trim();
                loadedValueList = new ArrayList<String>(Arrays.asList(manipulatedStr.split(", ")));
            } else {
                loadedValueList = new ArrayList<String>();
            }
            for (int i=0; i<Math.min(loadedEntryList.size(), loadedValueList.size()); i++) {
                list.add(Pair.create(loadedEntryList.get(i), loadedValueList.get(i)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * make all the files empty and refresh projects from api server
     */
    public static void deleteSavedData() {
        dataList   = new PairList();
        recentList = new PairList();
        saveList(REC_ENTRIES_FILENAME, REC_VALUES_FILENAME, recentList.getEntries(), recentList.getValues());
        saveList(ENTRIES_FILENAME, VALUES_FILENAME, dataList.getEntries(), dataList.getValues());
        that.refreshList();
    }

    /**
     * Task that handles requesting and getting the project list from api server
     *
     * @author      Or Sagi
     * @version     %I%, %G%
     * @since       1.0
     */
    private class FetchProjectsTask extends AsyncTask<Void, Void, List<ApiResult>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dataList  = new PairList();
            Toast toast =  Toast.makeText(getContext(),"refreshing!", Toast.LENGTH_SHORT);
            toast.show();
        }

        @Override
        protected List<ApiResult> doInBackground(Void... params) {
            assert ((MWApiApplication)getContext().getApplicationContext()) != null;
            MWApi api = ((MWApiApplication)getContext().getApplicationContext()).getApi();
            ApiResult result;
            try { // send API request
                result = api.action("query")
                        .param("meta", "messagegroups")
                        .param("mgdepth", "0")
                        .param("mgformat", "tree")
                        .param("prop", "id|label")  // info to get
                        .post();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return result.getNodes("/api/query/messagegroups/group"); // extract the proper nodes
        }

        @Override
        protected void onPostExecute(List<ApiResult> packedProjects) {
            super.onPostExecute(packedProjects);
            String label, id;
            for(ApiResult packedProj: packedProjects) {
                label = packedProj.getString("@label");
                id = packedProj.getString("@id");
                if (label!=null && id!=null && !dataList.contains(Pair.create(label,id))) {
                    dataList.add(Pair.create(label,id));
                } else {
                    // TODO: handle error in getting projects (got null or duplication)
                }
            }

            // make a special project
            dataList.remove(Pair.create("Recent translations", "!recent"));
            dataList.remove(Pair.create("Recent additions"   , "!additions"));
            if( !dataList.contains(Pair.create("Recent Contributions","!recent"))) {
               dataList.add(Pair.create("Recent Contributions", "!recent"));
            }

            // save the newly fetched projects into file
            saveList(ENTRIES_FILENAME,VALUES_FILENAME,getEntryList(),getValueList());
            dataList.removeAll(recentList); // filter recent projects from general section

            // update adapter
            if (itemListAdapter != null) {
               itemListAdapter.clear();
                itemListAdapter.addAll(extractEntries());
            }
        }
    }
}
