package net.translatewiki.app;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by orsa on 5/9/13.
 */
public class PairList extends ArrayList<Pair<String, String>> {

    public List<String> getEntries(){

        List<String> list = new ArrayList<String>();
        for(Pair<String, String> pair : this){
            list.add(pair.first);
        }
        return list;
    }

    public List<String> getValues(){

        List<String> list = new ArrayList<String>();
        for(Pair<String, String> pair : this){
            list.add(pair.second);
        }
        return list;
    }

    public String getValueOfEntry(String s){
        for (Pair<String, String> pair : this) {
            if (s==pair.first) {
                return pair.second;
            }
        }
        return null;
    }

    public String getEntryOfValue(String s){
        for (Pair<String, String> pair : this) {
            if (s==pair.second) {
                return pair.first;
            }
        }
        return null;
    }

}
