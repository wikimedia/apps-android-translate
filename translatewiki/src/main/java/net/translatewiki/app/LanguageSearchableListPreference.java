package net.translatewiki.app;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by orsa on 22/8/13.
 */
public class LanguageSearchableListPreference extends SearchableListPreference {

    private static final int MAX_RECENTS = 4;
    private static final int MAX_LOCALS = 3;
    String REC_ENTRIES_FILENAME = "rec_languages_entries_file";
    String REC_VALUES_FILENAME  = "rec_languages_values_file" ;

    private static PairList dataList = new PairList();
    private static PairList recentList = new PairList();

    public LanguageSearchableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        recentList = loadRecentList();
        dataList = loadAllList();
    }

    @Override
    public boolean showRefreshBtn() {
        return false;
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
            saveRecentList();
            itemListAdapter.clear();
            itemListAdapter.addAll(extractEntries());
        }
        sv.getOnFocusChangeListener().onFocusChange(sv,false);
    }

    @Override
    public String getDefaultValue() { return "he"; }


    @Override
    public Integer getNumberOfSections() { return 3; }

    private PairList loadRecentList(){
        PairList list = new PairList();

        FileInputStream fis;
        byte[] b;
        try {

            fis = getContext().openFileInput(REC_ENTRIES_FILENAME);
            b = new byte[fis.available()];
            fis.read(b);
            fis.close();
            List<String> loadedEntryList  = new ArrayList<String>(Arrays.asList((new String(b)).substring(1,b.length - 2).split(", ")));

            fis = getContext().openFileInput(REC_VALUES_FILENAME);
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

    private  void saveRecentList()
    {
        FileOutputStream fos;
        try {

            fos = getContext().openFileOutput(REC_ENTRIES_FILENAME, Context.MODE_PRIVATE);
            fos.write(recentList.getEntries().toString().getBytes());
            fos.close();

            fos = getContext().openFileOutput(REC_VALUES_FILENAME, Context.MODE_PRIVATE);
            fos.write(recentList.getValues().toString().getBytes());
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PairList loadAllList(){
        PairList list = new PairList();

        String[] entries =  getContext().getResources().getStringArray(R.array.pref_languages_list_titles);
        String[] values  =  getContext().getResources().getStringArray(R.array.pref_languages_list_values);
        for (int i=0 ; i<entries.length ; i++)
        {
            list.add(Pair.create(entries[i],values[i]));
        }

        list.removeAll(recentList);
        return list;
    }
}
