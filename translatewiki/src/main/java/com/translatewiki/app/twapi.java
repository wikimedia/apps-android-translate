package com.translatewiki.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.loopj.android.http.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;



/**
 * Created by orsa on 18/7/13.
 *
 * This code is the API wrapper library for working with translatewiki API server.
 */



public class twapi {
    private static twapi ourInstance = new twapi(); // Singleton

    public static twapi getInstance() { // this is the only way to get twapi object outside the library
        return ourInstance;
    }

    private twapi() {
        url = "http://translatewiki.net/w/api.php"; //todo: change to httpS
    }

    private static AsyncHttpClient client = new AsyncHttpClient();
    private String url;
    private String userID;
    private String lgusername;
    private String tmpPass;
    private String lgtoken;
    private Boolean logged;

    //private String curProj;
    //private String curLang;


    public void login(String user, String pass, final IFunction callback) {

        postLogin(user,pass,null, callback);

    }

    public void postLogin(String user, String pass, String token, final IFunction callback) {

        //todo: add input validation

        lgusername = user;
        tmpPass = pass;
        RequestParams params = new RequestParams();
        params.put("format", "json");
        params.put("action", "login");
        params.put("lgname", user);
        params.put("lgpassword", pass);
        if (token!=null)
            params.put("lgtoken",token);

        client.post(url,params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(String s) {
                super.onSuccess(s);

                Log.i("onSuccess", s);

                JSONObject json;
                try {
                    json = new JSONObject(s);
                    String resultStr = json.getJSONObject("login").getString("result");
                    if (resultStr.equals("NeedToken"))
                    {
                        String tok = json.getJSONObject("login").getString("token");
                        postLogin(lgusername,tmpPass,tok,callback);
                    }
                    else
                    {
                        Log.i("onSuccess", json.getJSONObject("login").getString("result"));
                        if (resultStr.equals("Success"))
                        {
                            userID = json.getJSONObject("login").getString("lguserid");
                            lgtoken = json.getJSONObject("login").getString("lgtoken");
                            logged = true;
                        }
                        else
                        {
                            logged = false;
                        }
                       tmpPass = null;
                       callback.execute(resultStr);
                    }

                    Log.i("onSuccess", json.getJSONObject("login").getString("cookieprefix"));
                    Log.i("onSuccess", json.getJSONObject("login").getString("sessionid"));

                } catch (JSONException e) {
                    Log.i("onSuccess", "Login Failed! (1)");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable throwable, String s) {
                super.onFailure(throwable, s);
                Log.i("onFailure", "Login Failed! (2)");
            }

        });


        /* Synchronous way, but independent

        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        httppost.setHeader("Accept", "application/json");
        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
            nameValuePairs.add(new BasicNameValuePair("format", "json"));
            nameValuePairs.add(new BasicNameValuePair("action", "login"));
            nameValuePairs.add(new BasicNameValuePair("lgname", "ademo"));
            nameValuePairs.add(new BasicNameValuePair("lgpassword", "ademo1"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            httppost.setURI(new URI(url));
            // Execute HTTP Post Request
            HttpResponse response;
            response = httpclient.execute(httppost);
            Log.d("API", response.toString());

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } */
    }

    protected Object parseResponse(String responseBody) throws JSONException {
        Object result = null;
        //trim the string to prevent start with blank, and test if the string is valid JSON, because the parser don't do this :(. If Json is not valid this will return null
        responseBody = responseBody.trim();
        if(responseBody.startsWith("{") || responseBody.startsWith("[")) {
            result = new JSONTokener(responseBody).nextValue();
        }
        if (result == null) {
            result = responseBody;
        }
        return result;
    }
}
