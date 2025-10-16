package com.netflix.mediaclient.ui.launch;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.netflix.mediaclient.MainActivity;
import com.netflix.mediaclient.R;

import android.media.AudioManager;
import android.view.MotionEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class UIWebViewActivity extends AppCompatActivity {

    PlayerView playerView;
    ExoPlayer player;
    WebView webView;
    int currentIndex = 0;

    //volume
    Button touchOverlay;
    TextView volumeIndicator;
    AudioManager audioManager;
    float startY;

    //loadingOverlay
    LinearLayout loadingOverlay;
    TextView loadingText;

    //load channel from API
    ArrayList<String> urls = new ArrayList<>();
    ArrayList<String> types = new ArrayList<>();
    ArrayList<String> channel = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_tv);

        playerView = findViewById(R.id.playerView);
        webView = findViewById(R.id.webView);

        // Init ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Init WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());

        loadChannel(currentIndex);

        //mobile next previw-----------------------------------
        Button btnLeft = findViewById(R.id.btnLeft);
        Button btnRight = findViewById(R.id.btnRight);
        Button btnCenter = findViewById(R.id.btnCenter);
        // Left button → Previous channel
        btnLeft.setOnClickListener(v -> {
            currentIndex--;
//            if (currentIndex < 0) currentIndex = urls.length - 1;

            if (currentIndex < 0) currentIndex = urls.size() - 1;
            loadChannel(currentIndex);
        });

        // Right button → Next channel
        btnRight.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex >= urls.size()) currentIndex = 0;
            loadChannel(currentIndex);
        });

        // Middle button → Pause/Play
        btnCenter.setOnClickListener(v -> {
            if (playerView.getVisibility() == PlayerView.VISIBLE) {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
            } else if (webView.getVisibility() == WebView.VISIBLE) {
                webView.evaluateJavascript(
                        "var v=document.querySelector('video'); " +
                                "if(v){ if(v.paused){v.play();}else{v.pause();} }",
                        null
                );
            }
        });
        //end mobile preview---------------------------------------------

        //volume +--------------------------------------------------------
        touchOverlay = findViewById(R.id.touchOverlay);
        volumeIndicator = findViewById(R.id.volumeIndicator);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        touchOverlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float endY = event.getY();
                        float deltaY = startY - endY; // up = positive, down = negative

                        if (Math.abs(deltaY) > 50) { // threshold
                            adjustVolume(deltaY > 0); // true = up, false = down
                            startY = endY; // reset baseline
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        volumeIndicator.setVisibility(View.GONE);
                        return true;
                }
                return false;
            }
        });
        //volume+- end-----------------------------------------------

        //loading animation start------------------------------------
        loadingOverlay = findViewById(R.id.loadingLayout);
        loadingText = findViewById(R.id.loadingText);
        // ExoPlayer listener
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    showLoading("Loading Channel...");
                } else if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    hideLoading();
                }
            }
        });
        // WebView listener
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                showLoading("Loading Channel...");
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                hideLoading();
            }
        });
        //loadinf animation end-----------------------------------------------

        //load channel from API---------------------------
        // init ExoPlayer, WebView etc.

        if (!loadChannelsFromCache()) {
            loadChannelsFromApi(); // If no cache, fetch fresh
        } else {
            loadChannelsFromApi(); // Still fetch latest in background
        }
        //End---------------------------
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    private void loadChannel(int index) {

        if (index < 0 || index >= urls.size()) return;

        // Stop both before switching
        stopAllPlayers();

//        String url = urls[index];
//        String type = types[index];

        String url = urls.get(index);
        String type = types.get(index);
        String channelname = channel.get(index);

        Toast.makeText(UIWebViewActivity.this, channelname+" Channel "+index,Toast.LENGTH_LONG).show();

        if ("stream".equals(type)) {

            // Show ExoPlayer, hide WebView
            playerView.setVisibility(PlayerView.VISIBLE);
            webView.setVisibility(WebView.GONE);

            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
            player.setMediaItem(mediaItem);
            player.prepare();
            player.setPlayWhenReady(true); // autoplay with sound

        } else if ("web".equals(type)) {
            // Show WebView, hide ExoPlayer
            webView.setVisibility(WebView.VISIBLE);
            playerView.setVisibility(PlayerView.GONE);

            // Auto fullscreen + autoplay for YouTube embed
            String html = "<html><body style='margin:0;padding:0;'>"
                    + "<iframe width='100%' height='100%' src='" + url + "' "
                    + "frameborder='0' allow='autoplay; fullscreen' allowfullscreen></iframe>"
                    + "</body></html>";

            webView.loadData(html, "text/html", "utf-8");

//            String html = "<!DOCTYPE html>" +
//                    "<html>" +
//                    "<body style=\"margin:0;padding:0;\">" +
//                    "<iframe width=\"100%\" height=\"100%\" " +
//                    "src=\"https://www.youtube.com/embed/live_stream?channel=" + url + "&autoplay=1&mute=1\" " +
//                    "frameborder=\"0\" allow=\"autoplay; encrypted-media fullscreen\" allowfullscreen></iframe>" +
//                    "</body>" +
//                    "</html>";
//            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        }
    }

    private void stopAllPlayers() {
        // Stop ExoPlayer
        if (player.isPlaying()) {
            player.stop();
        }

        // Stop WebView video
        if (webView != null) {
            webView.loadUrl("about:blank");
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_RIGHT: // Next
                    currentIndex++;
                    if (currentIndex >= urls.size()) currentIndex = 0;
                    loadChannel(currentIndex);
                    return true;

                case KeyEvent.KEYCODE_DPAD_LEFT: // Previous
                    currentIndex--;
                    if (currentIndex < 0) currentIndex = urls.size() - 1;
                    loadChannel(currentIndex);
                    return true;

            //----------------------------Remote OK button
                case KeyEvent.KEYCODE_DPAD_CENTER: // Remote OK button
                case KeyEvent.KEYCODE_ENTER:       // Enter key
                    if (playerView.getVisibility() == PlayerView.VISIBLE) {
                        if (player.isPlaying()) {
                            player.pause(); // Pause if playing
                        } else {
                            player.play(); // Resume if paused
                        }
                    } else if (webView.getVisibility() == WebView.VISIBLE) {
                        // For WebView videos (like YouTube embed)
                        webView.evaluateJavascript(
                                "var v=document.querySelector('video'); " +
                                        "if(v){ if(v.paused){v.play();}else{v.pause();} }",
                                null
                        );
                    }
                    return true;
                //-------------------


            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopAllPlayers();
        player.release();
    }

    // Adjust volume
    private void adjustVolume(boolean increase) {
        int direction = increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);

        // Show indicator
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int percent = (int) ((current * 100f) / max);

        volumeIndicator.setText("Volume: " + percent + "%");
        volumeIndicator.setVisibility(View.VISIBLE);
    }
    // Adjust volume end

    //loading animation start-------------------------------------
//    private void showLoading(String message) {
//        loadingOverlay.setVisibility(View.VISIBLE);
//        loadingText.setText(message);
//    }
//    private void hideLoading() {
//        loadingOverlay.setVisibility(View.GONE);
//    }
        private long loadingStartTime = 0;
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
    //loading animation end

    //load channel from API
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
                        parseChannelsJson(response);   // ✅ your parser
                        saveChannelsToCache(jsonString); // ✅ caching
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


    //Save Channels to Cache-------------------------------------------------
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
                return true; // ✅ Cache loaded successfully
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    private void parseChannelsJson(JSONObject response) {
        urls.clear();
        types.clear();
        channel.clear();

        try {
            JSONArray channels = response.getJSONArray("channels");
            for (int i = 0; i < channels.length(); i++) {
                JSONObject obj = channels.getJSONObject(i);
                urls.add(obj.getString("url"));
                types.add(obj.getString("type"));
                channel.add(obj.getString("channel"));
            }
            loadChannel(0); // ✅ Start first channel
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //onbackpress

private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
    @Override
    public void handleOnBackPressed() {
        onBackPressedn();
    }
};
// Handle Back Button like a browser
public void onBackPressedn() {
    Intent int1 = new Intent(UIWebViewActivity.this, MainActivity.class);
    startActivity(int1);
    finish();
}
}