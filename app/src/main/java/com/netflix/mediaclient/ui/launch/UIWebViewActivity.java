package com.netflix.mediaclient.ui.launch;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

    // 🔗 Your JSON file hosted on GitHub (contains latestVersion and apkUrl)
    private static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/mdshahinurislamm/television/refs/heads/master/assets/version.json";
    private static final int REQ_INSTALL_UNKNOWN_APPS = 2345;
    private long downloadId = -1;
    private ProgressBar progressBar;
    private AlertDialog updateDialog;
    private String pendingApkUrl;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_tv);

        //current version check start
        checkForUpdate();
        // current version check end

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
//Show an update dialog--------------------------------------

    private void checkForUpdate() {
        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder().url(UPDATE_JSON_URL).build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(UIWebViewActivity.this,
                        "Update check failed", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    JSONObject obj = new JSONObject(response.body().string());
                    int latestVersion = obj.getInt("latestVersion");
                    String apkUrl = obj.getString("apkUrl");
                    String notes = obj.optString("releaseNotes", "New update available");

                    int currentVersion = 2; //BuildConfig.VERSION_CODE;
                    if (latestVersion > currentVersion) {
                        pendingApkUrl = apkUrl;
                        //runOnUiThread(() -> showMandatoryUpdateDialog(apkUrl, notes));
                        runOnUiThread(() -> showUpdatePopup(notes));
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(UIWebViewActivity.this, "App is up to date", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showUpdatePopup(String notes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Required");
        builder.setMessage(notes);
        builder.setCancelable(false);
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                startDownload(pendingApkUrl);
            }
        });
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                finishAffinity(); // close app
            }
        });
        updateDialog = builder.create();
        updateDialog.setCancelable(false);
        updateDialog.setCanceledOnTouchOutside(false);
        updateDialog.show();
    }

    private void showMandatoryUpdateDialog(String apkUrl, String notes) {
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setMax(100);

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(60, 60, 60, 60);
        layout.setGravity(Gravity.CENTER);
        layout.addView(progressBar);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Required");
        builder.setMessage(notes + "\n\nDownloading update...");
        builder.setView(layout);
        builder.setCancelable(false);
        builder.setNegativeButton("Exit app", (dialog, which) -> finishAffinity());

        updateDialog = builder.create();
        updateDialog.setCanceledOnTouchOutside(false);
        updateDialog.show();

//        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
//            @Override public void onClick(DialogInterface dialog, int which) {
//
//            }
//        });
        startDownload(apkUrl);

    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void startDownload(String apkUrl) {
        // Enqueue download with DownloadManager so it handles downloads reliably
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("Downloading update");
        request.setDescription("Please wait...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // let DownloadManager pick location; we'll query it later
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = dm.enqueue(request);

        // listen for completion
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    // Download complete receiver
    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != downloadId) return;

            // Dismiss dialog UI
            if (updateDialog != null && updateDialog.isShowing()) updateDialog.dismiss();

            // Ask DownloadManager for the content URI of the downloaded file
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri apkUri = dm.getUriForDownloadedFile(downloadId); // <-- important: returns content:// URI if available

            if (apkUri == null) {
                // fallback: try to get file:// path (less reliable)
                Toast.makeText(UIWebViewActivity.this,
                        "Download finished but couldn't access file.", Toast.LENGTH_LONG).show();
                return;
            }

            // On Android O+ must have permission to install unknown apps
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    // open settings for this app so user can allow install from unknown sources
                    Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(i, REQ_INSTALL_UNKNOWN_APPS);
                    // Save apkUri somewhere accessible (field) so after RETURN we can proceed.
                    // We'll store downloadId and ask DownloadManager again in onActivityResult.
                    return;
                }
            }

            // Start installation
            startInstallIntent(apkUri);
        }
    };

    private void startInstallIntent(Uri apkUri) {
        try {
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Grant temporary read permission to the installer
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "No installer found to install the update", Toast.LENGTH_LONG).show();
        }
    }

    // If user returns from "allow unknown apps" screen, try installation again
    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_INSTALL_UNKNOWN_APPS) {
            // User may have granted (or denied) permission. Try again to get the downloaded file and install
            if (downloadId == -1) return;
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri apkUri = dm.getUriForDownloadedFile(downloadId);
            if (apkUri != null) {
                // if still not allowed, installer will refuse; otherwise will open installer
                startInstallIntent(apkUri);
            } else {
                Toast.makeText(this, "Cannot find downloaded APK to install.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(onDownloadComplete); } catch (Exception ignored) {}
    }


}