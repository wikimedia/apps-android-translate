package net.translatewiki.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by orsa on 27/7/13.
 */
public class TWAuthService extends Service {

    /** Reference to the actual authenticator */
    private TWAccountAuthenticator authenticator;

    @Override
    public void onCreate(){
        this.authenticator = new TWAccountAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}
