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

import java.util.ArrayList;
import java.util.List;

public class Message {
    private String mKey;
    private String mTitle;
    private String mGrupe;     // aka project
    private String mLanguage;
    private String mDefinition;
    private String mTranslation;
    private String mRevision;
    private String documentation;
    private int    mAcceptCount;
    private int    mState;
    private List<String> suggestionsList;


    public Message(String key, String mTitle, String mGrupe, String lang, String definition, String translation, String revision, int mAcceptCount, int mState)
    {
        this.mKey = key;
        this.mTitle = mTitle;
        this.mGrupe = mGrupe;
        this.mLanguage = lang;
        this.mDefinition = definition;
        this.mTranslation = translation;
        this.mRevision = revision;
        this.mAcceptCount = mAcceptCount;
        this.mState = mState;

        //this.infoNeedShown = false;
        this.suggestionsList = new ArrayList<String>(0);
    }

    public String getKey() {
        return mKey;
    }
    
    public String getLang() {
        return mLanguage;
    }
    
    public String getDefinition() {
        return mDefinition;
    }
    
    public String getTranslation() {
        return mTranslation;
    }
    
    public String getRevision() {
        return mRevision;
    }

    public int getAcceptCount() {
        return mAcceptCount;
    }

    public int getmState() {
        return mState;
    }

    public void setmState(int mState) {
        this.mState = mState;
    }

    public String getmTitle() {
        return mTitle;
    }

    public String getmGrupe() {
        return mGrupe;
    }

    public boolean addSuggestion(String s) {
        // make sure it is not duplicated first
        if (!suggestionsList.contains(s))
        {
            return suggestionsList.add(s);
        }
        return false;
    }

    public String getSuggestion(int i) {
        return suggestionsList.get(i);
    }

    public void clearSuggestions(){
        suggestionsList.clear();
    }

    public List<String> getSuggestionsList() {
        return suggestionsList;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public boolean IsDocumentationExists(){
        return (documentation!=null && documentation.length()>0);
    }

}
