package com.example.mberkc.mbcassignment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class HomeActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        webview =findViewById(R.id.webView);

        webview.setWebViewClient(new MyWebViewClient());
        webview.setWebChromeClient(new WebChromeClient());
        webview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(true);

        SharedPreferences sharedPref = HomeActivity.this.getPreferences(Context.MODE_PRIVATE);
        String lastVisited = sharedPref.getString(getString(R.string.latest_section), "/casino");
        Log.d("-----------last--------", "onCreate: " + lastVisited);
        webview.loadUrl("https://mobile.comeon.com"+lastVisited);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
            webview.goBack();
            SharedPreferences sharedPref = HomeActivity.this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            if (webview.getOriginalUrl().contains("casino")) {
                editor.putString(getString(R.string.latest_section), "/casino");
            }else if(webview.getOriginalUrl().contains("sportsbook")){
                editor.putString(getString(R.string.latest_section), "/sportsbook");
            }
            editor.commit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            SharedPreferences sharedPref = HomeActivity.this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            if (url.contains("casino")) {
                editor.putString(getString(R.string.latest_section), "/casino");
                editor.commit();
            }else if(url.contains("sportsbook")){
                editor.putString(getString(R.string.latest_section), "/sportsbook");
                editor.commit();
            }else if(url.contains("shop")){
                editor.putInt("shop_visited",
                        sharedPref.getInt("shop_visited", 0)+1);
                editor.commit();
                view.evaluateJavascript("javascript: alert('"+sharedPref.getInt("shop_visited", 0)+" time(s) visited');", null);
            }
            view.loadUrl(url);

            return true;
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
            if (url.contains("about")){
                Intent intent = new Intent(HomeActivity.this, AboutActivity.class);
                webview.goBack();
                startActivity(intent);
            }else if(url.contains("login")){
                SharedPreferences sharedPref = HomeActivity.this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("login_visited",
                        sharedPref.getInt("login_visited", 0)+1);
                editor.commit();
                view.evaluateJavascript("javascript: alert('"+sharedPref.getInt("login_visited", 0)+" time(s) visited');", null);
                Bundle bundle = new Bundle();
                bundle.putInt("LOGIN", 1);
                mFirebaseAnalytics.logEvent("LOGIN", bundle);

                view.evaluateJavascript("javascript: window.dataLayer[2];", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String gtmData) {
                        String[] userGTMProperties = gtmData.replaceAll("[{}\"]","").split(",");
                        for(String userGTMProperty: userGTMProperties) {
                            String[] kv = userGTMProperty.split(":");
                            if(kv[0].equals("pixel_country") || kv[0].equals("pixel_device") || kv[0].equals("pixel_language")) {
                                String key = kv[0];
                                String value = kv[1];
                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                DatabaseReference myRef = database.getReference();
                                myRef.child("analytics").child("gtmData").child(key).setValue(value);
                            }
                        }
                    }
                });
            }
        }

    }

}
