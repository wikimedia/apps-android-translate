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

/**
 * A message for translation or proofread.
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class Message {

    /** key identifier for the message */
    private String mKey;

    /** title is the message name given from server */
    private String mTitle;

    /** group is the project which the message belongs to */
    private String mGroup;

    /** destination language - to be translate to */
    private String mLanguage;

    /** source text to translate */
    private String mDefinition;

    /** translated text in destination language */
    private String mTranslation;

    /** number of the current change given by the server */
    private String mRevision;

    /** context helper */
    private String documentation;

    /** number of positive reviews for the current revision */
    private int    mAcceptCount;

    /** list of suggestions as a helper for translation */
    private List<String> suggestionsList;


    public Message(String key, String mTitle, String mGroup, String lang, String definition,
                   String translation, String revision, int mAcceptCount) {
        this.mKey = key;
        this.mTitle = mTitle;
        this.mGroup = mGroup;
        this.mLanguage = lang;
        this.mDefinition = definition;
        this.mTranslation = translation;
        this.mRevision = revision;
        this.mAcceptCount = mAcceptCount;
        this.suggestionsList = new ArrayList<String>(0);
    }

    /**
     * @return key of the message.
     */
    public String getKey() {
        return mKey;
    }

    /**
     * @return language to translate.
     */
    public String getLang() {
        return mLanguage;
    }

    /**
     * @return source text to translate.
     */
    public String getDefinition() {
        return mDefinition;
    }

    /**
     * @return translation text in the destination language.
     */
    public String getTranslation() {
        return mTranslation;
    }

    /**
     * @return revision number.
     */
    public String getRevision() {
        return mRevision;
    }

    /**
     * @return the number of positive reviews for this message.
     */
    public int getAcceptCount() {
        return mAcceptCount;
    }

    /**
     * @return title text for the message.
     */
    public String getmTitle() {
        return mTitle;
    }

    /**
     * @return the groupe name text which this message belongs to.
     */
    public String getmGroup() {
        return mGroup;
    }

    /**
     * adds a suggestion to to suggestions list, unless it is already contained in it.
     *
     * @param   s   suggesstion to be added
     * @return      true iff suggestion added
     */
    public boolean addSuggestion(String s) {
        // make sure it is not duplicated first
        return !suggestionsList.contains(s) && suggestionsList.add(s);
    }

    /**
     * clears suggestions list
     */
    public void clearSuggestions(){
        suggestionsList.clear();
    }

    /**
     * @return list of suggestions to translate this message.
     */
    public List<String> getSuggestionsList() {
        return suggestionsList;
    }

    /**
     * @return the documentation text for the message.
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * @param documentation the documentation text for the message.
     */
    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    /**
     * @return true iff documentation helper isn't empty.
     */
    public boolean IsDocumentationExists() {
        return (documentation!=null && documentation.trim().length()>0);
    }
}
