package net.translatewiki.app;

import android.os.AsyncTask;
import android.util.Log;

//import com.loopj.android.http.AsyncHttpClient;
//import com.loopj.android.http.AsyncHttpResponseHandler;
//import com.loopj.android.http.RequestParams;

import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.MWApi;

import java.io.IOException;

/**
 * Created by orsa on 18/7/13.
 *
 * This code is the API wrapper library for working with translatewiki API server.
 */

public class TWApi extends MWApi{

    private static TWApi ourInstance = new TWApi(); // Singleton


    public static TWApi getInstance() { // this is the only way to get twapi object outside the library
        return ourInstance;
    }

    private TWApi(String apiURL, AbstractHttpClient client) {
        super(apiURL, client);
    }

    private TWApi(){
        super("http://translatewiki.net/w/api.php", new DefaultHttpClient());
    }

    //private static AsyncHttpClient client = new AsyncHttpClient();
    private static String url = "https://translatewiki.net/w/api.php";
    private String userID;
    private String lgusername;
    private String tmpPass;
    private String lgtoken;
    private Boolean logged;

    //private String curProj;
    //private String curLang;


    public void login(String name,String pass, final IFunction callback){
        new AsyncTask<String,Object,String>(){

            @Override
            protected String doInBackground(String... strings) {
                try {
                    return login(strings[0],strings[1]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                callback.execute(result);
            }
        }.execute(name,pass);

    }

    public void logout(final IFunction callback){
        new AsyncTask<Object,Object,Object>(){

            @Override
            protected Object doInBackground(Object... strings) {
                try {
                    logout();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                callback.execute(null);
            }
        }.execute();
    }




//    public void login(String user, String pass, final IFunction callback) {
//        postLogin(user,pass,null, callback);
//    }


    /**
     * login method: sends login POST request containing the user credentials.
     * the function 'callback' will be called for any 200OK server response except 'NeedToken' response.
     * 'NeedToken' response will lead to send a login finalization request containing the supplied token
     */
//    private void postLogin(String user, String pass, String token, final IFunction callback) {
//
//        //todo: add input validation
//
//        lgusername = user;
//        tmpPass = pass;
//
//        //prepare login parameters
//        RequestParams params = new RequestParams();
//        params.put("format", "json");
//        params.put("action", "login");
//        params.put("lgname", user);
//        params.put("lgpassword", pass);
//        if (token!=null)
//            params.put("lgtoken",token); //for the login finalization request
//
//        client.post(url,params, new AsyncHttpResponseHandler() {
//
//            @Override
//            public void onSuccess(String s) {
//                super.onSuccess(s);
//
//                Log.i("onSuccess", s);
//
//                JSONObject json;
//                try {
//                    //parse json response
//                    json = new JSONObject(s);
//                    String resultStr = json.getJSONObject("login").getString("result");
//
//                    if (resultStr.equals("NeedToken")) //good request, need to finalize
//                    {
//                        String tok = json.getJSONObject("login").getString("token");
//                        postLogin(lgusername,tmpPass,tok,callback); // make login finalization request, this time with token.
//                    }
//                    else
//                    {
//                        Log.i("onSuccess", json.getJSONObject("login").getString("result"));
//                        if (resultStr.equals("Success")) //logged in
//                        {
//                            userID = json.getJSONObject("login").getString("lguserid");
//                            lgtoken = json.getJSONObject("login").getString("lgtoken");
//                            logged = true;
//                        }
//                        else //other result - not logged in
//                        {
//                            logged = false;
//                            lgtoken = null;
//                            userID = null;
//                        }
//                       tmpPass = null;
//                       callback.execute(resultStr);
//                    }
//
//                    Log.i("onSuccess", json.getJSONObject("login").getString("cookieprefix"));
//                    Log.i("onSuccess", json.getJSONObject("login").getString("sessionid"));
//
//                } catch (JSONException e) {
//                    Log.i("onSuccess", "Login Failed! (1)");
//                    e.printStackTrace();
//                    //TODO: handle exception
//                }
//            }
//
//            @Override
//            public void onFailure(Throwable throwable, String s) {
//                super.onFailure(throwable, s);
//                Log.i("onFailure", "Login Failed! (2)");
//                //TODO: handle failure
//            }
//
//        });
//
//
//        /* Synchronous way, but independent
//
//        // Create a new HttpClient and Post Header
//        HttpClient httpclient = new DefaultHttpClient();
//        HttpPost httppost = new HttpPost(url);
//        httppost.setHeader("Accept", "application/json");
//        try {
//            // Add your data
//            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
//            nameValuePairs.add(new BasicNameValuePair("format", "json"));
//            nameValuePairs.add(new BasicNameValuePair("action", "login"));
//            nameValuePairs.add(new BasicNameValuePair("lgname", "ademo"));
//            nameValuePairs.add(new BasicNameValuePair("lgpassword", "ademo1"));
//            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//
//            httppost.setURI(new URI(url));
//            // Execute HTTP Post Request
//            HttpResponse response;
//            response = httpclient.execute(httppost);
//            Log.d("API", response.toString());
//
//        } catch (ClientProtocolException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        } */
//    }

    /**
     * logout method: sends logout POST request containing the session cookie
     */
//    public void logout(final IFunction callback) {
//
//        // reset relevant fields
//        logged = false;
//        tmpPass = null;
//        lgtoken = null;
//
//        // prepare logout parameters
//        RequestParams params = new RequestParams();
//        params.put("format", "json");
//        params.put("action", "logout");
//
//        // send request
//        client.post(url,params, new AsyncHttpResponseHandler() {
//            @Override
//            public void onSuccess(String s) {
//                callback.execute(s);
//            }
//        });
//    }

}

