package net.translatewiki.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.mediawiki.auth.MWApiApplication;
import org.mediawiki.auth.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by orsa on 22/8/13.
 */
public class ProjectSearchableListPreference extends SearchableListPreference {

    private static final int MAX_RECENTS = 4;

    String ENTRIES_FILENAME     = "projects_entries_file"    ;
    String VALUES_FILENAME      = "projects_values_file"     ;
    String REC_ENTRIES_FILENAME = "rec_projects_entries_file";
    String REC_VALUES_FILENAME  = "rec_projects_values_file" ;

    private static PairList dataList = new PairList();      // <entry,value>
    private static PairList recentList = new PairList();

    public ProjectSearchableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        recentList  = loadList(REC_ENTRIES_FILENAME,REC_VALUES_FILENAME);
        dataList    = loadList(ENTRIES_FILENAME, VALUES_FILENAME);
        if (dataList==null || dataList.size()==0){
            refreshList();
        }
        dataList.removeAll(recentList);
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
                return new PairList();
            case 2:
                return dataList;
        }
        return null;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String selEnt = itemListAdapter.selectedEntry;
            String s = getValueOfEntry(selEnt);
            recentList.remove(Pair.create(selEnt, s));
            recentList.add(0, Pair.create(selEnt, s));
            while (recentList.size()>MAX_RECENTS) recentList.remove(MAX_RECENTS);
            dataList.removeAll(recentList);
            saveList(REC_ENTRIES_FILENAME,REC_VALUES_FILENAME);
            itemListAdapter.clear();
            itemListAdapter.addAll(extractEntries());
        }
        sv.getOnFocusChangeListener().onFocusChange(sv,false);
    }

    public ArrayList<String> getEntryList() {
        ArrayList<String> entryList = new ArrayList<String>();
        if (dataList==null || dataList.size()==0){
            refreshList();
        }
        else{
            for (Pair<String, String> s : dataList) {
                entryList.add(s.first);
            }
        }

        return entryList;
    }

    public ArrayList<String> getValueList() {
        ArrayList<String> valueList = new ArrayList<String>();
        if (dataList==null || dataList.size()==0){
            refreshList();
        }
        else{
            for (Pair<String, String> s : dataList) {
                valueList.add(s.second);
            }
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

    public void saveList(String key_e , String key_v) {

        FileOutputStream fos;
        try {

            fos = getContext().openFileOutput(key_e, Context.MODE_PRIVATE);
            fos.write(getEntryList().toString().getBytes());
            fos.close();

            fos = getContext().openFileOutput(key_v, Context.MODE_PRIVATE);
            fos.write(getValueList().toString().getBytes());
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
            List<String> loadedEntryList  = new ArrayList<String>(Arrays.asList((new String(b)).substring(1,b.length - 2).split(", ")));

            fis = getContext().openFileInput(key_v);
            b = new byte[fis.available()];
            fis.read(b);
            fis.close();
            List<String> loadedValueList = new ArrayList<String>(Arrays.asList((new String(b)).substring(1,b.length - 2).split(", ")));

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
            List<ApiResult> packedProjects = result.getNodes("/api/query/messagegroups/group");

            return packedProjects;
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

            saveList(ENTRIES_FILENAME,VALUES_FILENAME);
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
