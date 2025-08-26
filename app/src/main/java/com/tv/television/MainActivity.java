package com.tv.television;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    Button btnMytv, btnGoogle, btnYouTube, btnMySite;
    FrameLayout fullScreenContainer;
    View customView;
    WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        btnMytv = findViewById(R.id.btnMytv);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnYouTube = findViewById(R.id.btnYouTube);
        btnMySite = findViewById(R.id.btnMySite);
        fullScreenContainer = findViewById(R.id.fullScreenContainer);

        // Configure WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Keep navigation inside WebView
        webView.setWebViewClient(new WebViewClient());

        // Enable fullscreen video support
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Show video fullscreen
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                customView = view;
                customViewCallback = callback;
                fullScreenContainer.addView(view);
                fullScreenContainer.setVisibility(View.VISIBLE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                // Exit fullscreen
                if (customView == null) {
                    return;
                }

                fullScreenContainer.removeView(customView);
                customView = null;
                fullScreenContainer.setVisibility(View.GONE);
                customViewCallback.onCustomViewHidden();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        });

        // Load default
        webView.loadUrl("https://sites.google.com/view/mytelevision");

        // Button clicks
        btnMytv.setOnClickListener(v -> webView.loadUrl("https://sites.google.com/view/mytelevision"));
        btnGoogle.setOnClickListener(v -> webView.loadUrl("https://www.google.com"));
        btnYouTube.setOnClickListener(v -> webView.loadUrl("https://www.youtube.com"));
        btnMySite.setOnClickListener(v -> webView.loadUrl("https://server2.ftpbd.net"));

        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }
    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            onBackPressedn();
        }
    };
    // Handle Back Button like a browser
    public void onBackPressedn() {
        if (customView != null) {
            // Exit fullscreen first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webView.getWebChromeClient().onHideCustomView();
            }
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            //super.onBackPressed();
            //finish();
            finishAffinity(); // closes all activities
        }
    }

    public void btnMySite(View view) {
        Intent int1 = new Intent(MainActivity.this, LiveTvActivity.class);
        startActivity(int1);
    }
}
