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

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of pairs \<key,value\>
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class PairList extends ArrayList<Pair<String, String>> {

    /**
     * @return List of all keys in this PairList.
     */
    public List<String> getEntries(){
        List<String> list = new ArrayList<String>();
        for(Pair<String, String> pair : this) {
            list.add(pair.first);
        }
        return list;
    }

    /**
     * @return List of all values in this PairList.
     */
    public List<String> getValues(){

        List<String> list = new ArrayList<String>();
        for(Pair<String, String> pair : this) {
            list.add(pair.second);
        }
        return list;
    }

    /**
     * lookup for a value for a specified entry.
     *
     * @param entry key to lookup
     * @return the value related to the specified entry, or null if does not exist.
     */
    public String getValueOfEntry(String entry){
        for (Pair<String, String> pair : this) {
            if (entry.equals(pair.first)) {
                return pair.second;
            }
        }
        return null;
    }

    /**
     * lookup for an entry for a specified value.
     *
     * @param value value to lookup
     * @return the first entry related to the specified value, or null if does not exist.
     */
    public String getEntryOfValue(String value){
        for (Pair<String, String> pair : this) {
            if (value.equals(pair.second)) {
                return pair.first;
            }
        }
        return null;
    }

}
