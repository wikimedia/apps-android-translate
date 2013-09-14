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
            if (s.equals(pair.first)) {
                return pair.second;
            }
        }
        return null;
    }

    public String getEntryOfValue(String s){
        for (Pair<String, String> pair : this) {
            if (s.equals(pair.second)) {
                return pair.first;
            }
        }
        return null;
    }

}
