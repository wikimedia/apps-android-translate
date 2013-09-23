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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Application;

import org.apache.http.HttpVersion;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.mediawiki.api.MWApi;
import org.mediawiki.auth.MWApiApplication;

import java.io.IOException;

/**
 * Application class to hold any global property or cross module object.
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class TranslateWikiApp extends Application implements MWApiApplication {

    // hard-coded properties.
    public static final int MAX_SUGGESTION_LENGTH = 100;
    public static final int     MAX_NO_SUGGESTIONS         = 3;
    public static final int     MAX_NO_FETCH_TRIALS        = 5;
    public static final Double MIN_SUGGESTION_QUALITY = 0.9;

    private static MWApi api = createMWApi(); //singleton

    private Account currentAccount = null; // Unlike a savings account...
    public static final String API_URL = "https://translatewiki.net/w/api.php";
   
    public static MWApi createMWApi() {
        DefaultHttpClient client = new DefaultHttpClient();
        // Because WMF servers support only HTTP/1.0. Biggest difference that
        // this makes is support for Chunked Transfer Encoding. 
        // I have this here so if any 1.1 features start being used, it 
        // throws up. 
        client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, 
                HttpVersion.HTTP_1_0);
        return new MWApi(API_URL, client);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate() {
        super.onCreate();
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.INFO);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.INFO);

        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
    }

    /** @return api instance for this application */
    @Override
    public MWApi getApi() {
        return api;
    }

    /** @return api instance for this application */
    @Override
    public Account getCurrentAccount() {
        if(currentAccount == null) {
            AccountManager accountManager = AccountManager.get(this);
            Account[] allAccounts = accountManager.getAccountsByType(getString(R.string.account_type_identifier));
            if(allAccounts.length != 0) {
                currentAccount = allAccounts[0];
            }
        }
        return currentAccount;
    }
    
    public Boolean revalidateAuthToken() {
        AccountManager accountManager = AccountManager.get(this);
        Account curAccount = getCurrentAccount();
       
        if(curAccount == null) {
            return false; // This should never happen
        }
        
        accountManager.invalidateAuthToken(getString(R.string.account_type_identifier), api.getAuthCookie());
        try {
            String authCookie = accountManager.blockingGetAuthToken(curAccount, "", false);
            api.setAuthCookie(authCookie);
            return true;
        } catch (OperationCanceledException e) {
            e.printStackTrace();
            return false;
        } catch (AuthenticatorException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setCurrentAccount(Account account) {
       currentAccount = account; 
    }
}
