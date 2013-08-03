package org.mediawiki.auth;

import android.accounts.Account;

import org.mediawiki.api.MWApi;

public interface MWApiApplication {
    MWApi getApi();
    Account getCurrentAccount();
    void setCurrentAccount(Account account);
}
