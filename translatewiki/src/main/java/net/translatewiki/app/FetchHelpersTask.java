package net.translatewiki.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;
import org.mediawiki.auth.Utils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Handles the task of getting helpers (suggestions and documentation).
 */
public class FetchHelpersTask extends AsyncTask<Void, Void, Void> {

    public MessageAdapter m;
    private Activity context;

    // CTOR
    public FetchHelpersTask(Activity context, MessageAdapter m) {
        this.m = m;
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        MWApi api = ((MainActivity)context).getApp().getApi();
        ApiResult result;
        try { // send API request
            result = api.action("translationaids")
                    .param("title", m.getmTitle())
                    .param("group", m.getmGrupe())                // project
                    .param("prop", "mt|ttmserver|documentation")  // info to get
                    .post();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        m.clearSuggestions();
        int count = 0;

        // extracts suggestions
        ArrayList<ApiResult> packedSuggestions = result.getNodes("/api/helpers/mt/suggestion");
        Log.d("TWN", "Actual result is" + Utils.getStringFromDOM(result.getDocument()));

        String s;
        // insert suggestions with not longer than "MAX_LENGTH_FOR_SUGGESTION",
        // (too long suggestion is empirically bad suggestion)
        // but no more than "MAX_NO_SUGGESTIONS"
        for(ApiResult packedSuggestion: packedSuggestions)
        {
            if (count<MainActivity.MAX_NO_SUGGESTIONS){
                s = packedSuggestion.getString("@target");
                if (s.length()>MainActivity.MAX_LENGTH_FOR_SUGGESTION)
                    continue;

                Log.d("TWN", "suggestion is" + s);
                if(m.addSuggestion(s)) // this call also makes sure no to add duplicates
                    count++;
            } else break;
        }

        // insert suggestions with not longer than "MAX_LENGTH_FOR_SUGGESTION",
        // with a quality of at least "MIN_QUALITY_FOR_SUGGESTION",
        // but no more than "MAX_NO_SUGGESTIONS"
        ArrayList<ApiResult> packedTtms = result.getNodes("/api/helpers/ttmserver/suggestion");
        for(ApiResult packedSuggestion: packedTtms)
        {
            if (count<MainActivity.MAX_NO_SUGGESTIONS){
                if (packedSuggestion.getNumber("@quality") < MainActivity.MIN_QUALITY_FOR_SUGGESTION)
                    continue;
                s = packedSuggestion.getString("@target");
                if (s.length()>MainActivity.MAX_LENGTH_FOR_SUGGESTION)
                    continue;
                Log.d("TWN", "suggestion is" + s);
                if (m.addSuggestion(s)) // this call also makes sure no to add duplicates
                    count++;
            } else break;
        }

        // extract documentation
        String doc = result.getNode("/api/helpers/documentation").getString("@html");
        // documentation is a helper which is shown by WebView,
        // since it is written in 'wiki' code, contains patterns etc.

        doc = doc.split("<div class=\"mw-identical-title mw-collapsible mw-collapsed")[0];
        // some of the documentations include a collapsible tail which let the user see
        // similar usage examples. we are not interested in this tail, for now.

        Log.d("TWN", "doc is: " + doc);
        m.setDocumentation(doc);
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        super.onPostExecute(v);
        ((MainActivity)context).postFetchHelpers(m);
    }
}

