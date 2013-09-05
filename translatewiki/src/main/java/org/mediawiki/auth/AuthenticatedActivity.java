package org.mediawiki.auth;

import java.io.IOException;

import net.translatewiki.app.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.AsyncTask;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AuthenticatedActivity extends SherlockActivity {
    
    protected MWApiApplication app;
    
    private class GetAuthCookieTask extends AsyncTask<Void, String, String> {
        private Account account;
        private AccountManager accountManager;
        public GetAuthCookieTask(Account account, AccountManager accountManager) {
            this.account = account;
            this.accountManager = accountManager;
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result != null) {
                onAuthCookieAcquired(result);
            } else {
                onAuthFailure();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return accountManager.blockingGetAuthToken(account, "", false);
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                return null;
            } catch (AuthenticatorException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
   
    
    private class AddAccountTask extends AsyncTask<Void, String, String> {
        private AccountManager accountManager;
        public AddAccountTask(AccountManager accountManager) {
            this.accountManager = accountManager;
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result != null) {
                Account[] allAccounts = accountManager.getAccountsByType(getString(R.string.account_type_identifier));
                Account curAccount = allAccounts[0];
                GetAuthCookieTask getCookieTask = new GetAuthCookieTask(curAccount, accountManager);
                getCookieTask.execute();
            } else {
                onAuthFailure();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            AccountManagerFuture<Bundle> resultFuture = accountManager.addAccount(getString(R.string.account_type_identifier), null, null, null, AuthenticatedActivity.this, null, null);
            Bundle result;
            try {
                result = resultFuture.getResult();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                return null;
            } catch (AuthenticatorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
            if(result.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
                return result.getString(AccountManager.KEY_ACCOUNT_NAME);
            } else {
                return null;
            }
            
        }
    }
    protected void requestAuthToken() {
        AccountManager accountManager = AccountManager.get(this);
        Account curAccount = app.getCurrentAccount();
        if(curAccount == null) {
            AddAccountTask addAccountTask = new AddAccountTask(accountManager);
            // This AsyncTask blocks until the Login Activity returns
            // And since in Android 4.x+ only one background thread runs all AsyncTasks
            // And since LoginActivity can't return until it's own AsyncTask (that does the login)
            // returns, we have a deadlock!
            // Fixed by explicitly asking this to be executed in parallel
            // See: https://groups.google.com/forum/?fromgroups=#!topic/android-developers/8M0RTFfO7-M
            Utils.executeAsyncTask(addAccountTask);
        } else {
            GetAuthCookieTask task = new GetAuthCookieTask(curAccount, accountManager);
            task.execute();
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.app = (MWApiApplication)this.getApplicationContext();
    }

    protected void onAuthCookieAcquired(String authCookie) {
        
    }
    protected void onAuthFailure() {
        
    }

}
