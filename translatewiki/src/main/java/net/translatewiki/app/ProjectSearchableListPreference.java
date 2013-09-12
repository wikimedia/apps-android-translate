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
 * Created by orsa on 22/8/13.
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getContext().getString(R.string.projects_key)))
            setSummaryofValue();
    }

    @Override
    public boolean showRefreshBtn() {
        return true;
    }

    @Override
    public PairList getListForSection(int section) {
        switch (section){
            case 0:
                return recentList;
            case 1:
                return new PairList(); // we don't use this section for projects
            case 2:
                return dataList;
        }
        return null;
    }

    @Override
    public int getSizeOfSection(int section) {
        switch (section){
            case 0:
                return recentList.size();
            case 1:
                return 0;
            case 2:
                return dataList.size();
        }
        return 0;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String selEnt = itemListAdapter.selectedEntry;
            String s = getValueOfEntry(selEnt);
            if (selEnt!=null && s!=null){
                recentList.remove(Pair.create(selEnt, s));
                recentList.add(0, Pair.create(selEnt, s));
                while (recentList.size()>MAX_RECENTS) recentList.remove(MAX_RECENTS);
                dataList    = loadList(ENTRIES_FILENAME, VALUES_FILENAME);
                dataList.removeAll(recentList);
                saveList(REC_ENTRIES_FILENAME,REC_VALUES_FILENAME,recentList.getEntries(),recentList.getValues());
                itemListAdapter.clear();
                itemListAdapter.addAll(extractEntries());
            }
        }
        sv.getOnFocusChangeListener().onFocusChange(sv,false);
    }

    public List<String> getEntryList() {
        ArrayList<String> entryList = new ArrayList<String>();
        if (dataList==null || dataList.size()==0){
            refreshList();
        }
        else{
            entryList.addAll(dataList.getEntries());
        }

        return entryList;
    }

    public List<String> getValueList() {
        ArrayList<String> valueList = new ArrayList<String>();
        if (dataList==null || dataList.size()==0){
            refreshList();
        }
        else{
            valueList.addAll(dataList.getValues());
        }
        return valueList;
    }

    @Override
    public String getDefaultValue() { return "!recent"; }

    @Override
    public Integer getNumberOfSections() { return 3; }

    @Override
    public void onClickRefresh() {
        refreshList();
        //this.itemListAdapter.notifyDataSetChanged();
    }

    public void refreshList() {
        new FetchProjectsTask().execute();
    }

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
            if (b.length>2){
                tempStr = new String(b).trim();
                manipulatedStr = tempStr.substring(1,tempStr.length()-1).trim();
                loadedEntryList  = new ArrayList<String>(Arrays.asList(manipulatedStr.split(", ")));
            }
            else
                loadedEntryList  = new ArrayList<String>();

            fis = getContext().openFileInput(key_v);
            b = new byte[fis.available()];
            fis.read(b);
            fis.close();
            if (b.length>2){
                tempStr = new String(b).trim();
                manipulatedStr = tempStr.substring(1,tempStr.length()-1).trim();
                loadedValueList  = new ArrayList<String>(Arrays.asList(manipulatedStr.split(", ")));
            }
            else
                loadedValueList  = new ArrayList<String>();

            int i;
            for (i=0; i<Math.min(loadedEntryList.size(), loadedValueList.size());i++){
                list.add(Pair.create(loadedEntryList.get(i), loadedValueList.get(i)));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void deleteSavedData(){

        dataList = new PairList();
        that.refreshList();
        recentList = new PairList();
        saveList(REC_ENTRIES_FILENAME,REC_VALUES_FILENAME,recentList.getEntries(),recentList.getValues());
        saveList(ENTRIES_FILENAME,VALUES_FILENAME,dataList.getEntries(),dataList.getValues());
    }

    /**
     * Handles the task of getting the project list.
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

            // extracts suggestions
            return result.getNodes("/api/query/messagegroups/group");
        }

        @Override
        protected void onPostExecute(List<ApiResult> packedProjects) {
            super.onPostExecute(packedProjects);

            String label, id;
            for(ApiResult packedProj: packedProjects)
            {
                label = packedProj.getString("@label");
                id = packedProj.getString("@id");
                if (label!=null && id!=null && !dataList.contains(Pair.create(label,id))) {
                    dataList.add(Pair.create(label,id));
                  //  entryList.add(label);
                  //  valueList.add(id);
                }
                else {

                    // TODO: handle error in getting projects (got null or duplication)
                }
            }

            // make a special project

            dataList.remove(Pair.create("Recent translations", "!recent"));
            dataList.remove(Pair.create("Recent additions"   , "!additions"));
            if( !dataList.contains(Pair.create("Recent Contributions","!recent"))) {
               dataList.add(Pair.create("Recent Contributions", "!recent"));
            }

            saveList(ENTRIES_FILENAME,VALUES_FILENAME,getEntryList(),getValueList());
            dataList.removeAll(recentList);


            if (itemListAdapter!=null)
            {
                int i = itemListAdapter.getCount();
                if (i>0)
                    itemListAdapter.clear();
                itemListAdapter.addAll(extractEntries());
            }
        }
    }


}
