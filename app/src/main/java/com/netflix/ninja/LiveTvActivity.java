package com.netflix.ninja;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LiveTvActivity extends AppCompatActivity {

    VLCVideoLayout vlcVideoLayout;
    LibVLC libVLC;
    MediaPlayer vlcPlayer;
    WebView webView;

    int currentIndex = 0;

    // volume
    Button touchOverlay;
    TextView volumeIndicator;
    AudioManager audioManager;
    float startY;

    // loadingOverlay
    LinearLayout loadingOverlay;
    TextView loadingText;

    // channels
    ArrayList<String> urls = new ArrayList<>();
    ArrayList<String> types = new ArrayList<>();

    private long loadingStartTime = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_tv);

        vlcVideoLayout = findViewById(R.id.vlc_video_layout);
        webView = findViewById(R.id.webView);

        // Initialize VLC
        ArrayList<String> options = new ArrayList<>();
        options.add("--network-caching=300");
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");
        libVLC = new LibVLC(this, options);

        vlcPlayer = new MediaPlayer(libVLC);
        vlcPlayer.attachViews(vlcVideoLayout, null, false, false);

        // WebView setup
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());

        loadChannel(currentIndex);

        // Channel navigation buttons
        Button btnLeft = findViewById(R.id.btnLeft);
        Button btnRight = findViewById(R.id.btnRight);
        Button btnCenter = findViewById(R.id.btnCenter);

        btnLeft.setOnClickListener(v -> {
            currentIndex--;
            if (currentIndex < 0) currentIndex = urls.size() - 1;
            loadChannel(currentIndex);
        });

        btnRight.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex >= urls.size()) currentIndex = 0;
            loadChannel(currentIndex);
        });

        btnCenter.setOnClickListener(v -> {
            togglePlayPause();
        });

        // volume gestures
        touchOverlay = findViewById(R.id.touchOverlay);
        volumeIndicator = findViewById(R.id.volumeIndicator);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        touchOverlay.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = event.getY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float endY = event.getY();
                    float deltaY = startY - endY;
                    if (Math.abs(deltaY) > 50) {
                        adjustVolume(deltaY > 0);
                        startY = endY;
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    volumeIndicator.setVisibility(View.GONE);
                    return true;
            }
            return false;
        });

        // Loading overlay
        loadingOverlay = findViewById(R.id.loadingLayout);
        loadingText = findViewById(R.id.loadingText);

        // load channels
        if (!loadChannelsFromCache()) {
            loadChannelsFromApi();
        } else {
            loadChannelsFromApi(); // update in background
        }

        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    private void loadChannel(int index) {
        if (index < 0 || index >= urls.size()) return;

        stopAllPlayers();

        String url = urls.get(index);
        String type = types.get(index);

        if ("stream".equals(type)) {
            vlcVideoLayout.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);

            //showLoading("Loading Channel...");
            Media media = new Media(libVLC, Uri.parse(url));
            media.setHWDecoderEnabled(true, false);
            media.addOption(":network-caching=300");
            vlcPlayer.setMedia(media);
            media.release();
            vlcPlayer.play();

        } else if ("web".equals(type)) {
            webView.setVisibility(View.VISIBLE);
            vlcVideoLayout.setVisibility(View.GONE);

            //showLoading("Loading Channel...");
            String html = "<html><body style='margin:0;padding:0;'>"
                    + "<iframe width='100%' height='100%' src='" + url + "?autoplay=1&mute=0' "
                    + "frameborder='0' allow='autoplay; fullscreen' allowfullscreen></iframe>"
                    + "</body></html>";

            webView.loadData(html, "text/html", "utf-8");
        }
    }

    private void stopAllPlayers() {
        if (vlcPlayer.isPlaying()) vlcPlayer.stop();
        if (webView != null) webView.loadUrl("about:blank");
    }

    private void togglePlayPause() {
        if (vlcVideoLayout.getVisibility() == View.VISIBLE) {
            if (vlcPlayer.isPlaying()) {
                vlcPlayer.pause();
            } else {
                vlcPlayer.play();
            }
        } else if (webView.getVisibility() == View.VISIBLE) {
            webView.evaluateJavascript(
                    "var v=document.querySelector('video'); " +
                            "if(v){ if(v.paused){v.play();}else{v.pause();} }",
                    null
            );
        }
    }

    private void adjustVolume(boolean increase) {
        int direction = increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);

        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int percent = (int) ((current * 100f) / max);

        volumeIndicator.setText("Volume: " + percent + "%");
        volumeIndicator.setVisibility(View.VISIBLE);
    }

    private void showLoading(String message) {
        loadingOverlay.setVisibility(View.VISIBLE);
        loadingText.setText(message);
        loadingStartTime = System.currentTimeMillis();
    }

    private void hideLoading() {
        long elapsed = System.currentTimeMillis() - loadingStartTime;
        if (elapsed < 2000) {
            loadingOverlay.postDelayed(() -> loadingOverlay.setVisibility(View.GONE), 2000 - elapsed);
        } else {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    private void loadChannelsFromApi() {
        new Thread(() -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/mdshahinurislamm/television/refs/heads/master/assets/channels.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                String jsonString = result.toString();
                runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(jsonString);
                        parseChannelsJson(response);
                        saveChannelsToCache(jsonString);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!loadChannelsFromCache()) {
                        Toast.makeText(this, "Failed to load channels", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void saveChannelsToCache(String jsonString) {
        SharedPreferences prefs = getSharedPreferences("channels_cache", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("channels_json", jsonString);
        editor.apply();
    }

    private boolean loadChannelsFromCache() {
        SharedPreferences prefs = getSharedPreferences("channels_cache", MODE_PRIVATE);
        String cachedJson = prefs.getString("channels_json", null);

        if (cachedJson != null) {
            try {
                JSONObject response = new JSONObject(cachedJson);
                parseChannelsJson(response);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void parseChannelsJson(JSONObject response) {
        urls.clear();
        types.clear();

        try {
            JSONArray channels = response.getJSONArray("channels");
            for (int i = 0; i < channels.length(); i++) {
                JSONObject obj = channels.getJSONObject(i);
                urls.add(obj.getString("url"));
                types.add(obj.getString("type"));
            }
            loadChannel(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            Intent int1 = new Intent(LiveTvActivity.this, MainActivity.class);
            startActivity(int1);
            finish();
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    currentIndex++;
                    if (currentIndex >= urls.size()) currentIndex = 0;
                    loadChannel(currentIndex);
                    return true;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    currentIndex--;
                    if (currentIndex < 0) currentIndex = urls.size() - 1;
                    loadChannel(currentIndex);
                    return true;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    togglePlayPause();
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopAllPlayers();
        if (vlcPlayer != null) {
            vlcPlayer.detachViews();
            vlcPlayer.release();
        }
        if (libVLC != null) {
            libVLC.release();
        }
    }
}
