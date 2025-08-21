package com.tv.television;


import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;

import android.content.Context;
import android.media.AudioManager;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class LiveTvActivity extends AppCompatActivity {

    PlayerView playerView;
    ExoPlayer player;
    WebView webView;

    // Your streaming links
    String[] urls = {
            "https://deshitv.deshitv24.net/live/myStream/playlist.m3u8",
            "https://www.youtube.com/embed/TdwhCOFh9OA",   // Channel 2
            "http://158.69.124.9:1935/5aabtv/5aabtv/playlist.m3u8",
            "http://95eryw39dwn4-hls-live.wmncdn.net/Ayushu/271ddf829afeece44d8732757fba1a66.sdp/index.m3u8",
            "https://feeds.intoday.in/hltapps/api/master.m3u8",
            "http://playhls.media.nic.in/live/vyas-360p/index.m3u8",
            "http://cdn2.live247stream.com/desiplus/tv/playlist.m3u8",
            "http://fash1043.cloudycdn.services/slive/_definst_/ftv_pg16_adaptive.smil/playlist.m3u8",
            "https://ndtv24x7elemarchana.akamaized.net/hls/live/2003678/ndtv24x7/ndtv24x7master.m3u8",
            "http://cdn27.live247stream.com/primecanada/247/primecanada/stream1/chunks.m3u8",
            "http://6284rn2xr7xv-hls-live.wmncdn.net/shubhsandeshtv1/live123.stream/index.m3u8",
            "http://cdn9.live247stream.com/punjabitvcanada/tv/playlist.m3u8",
            "http://158.69.124.9:1935/sardaritv/sardaritv/playlist.m3u8",
            "https://amg1-i.akamaihd.net/hls/live/784034/Q001/playlist.m3u8?__nn__=5606168722001&hdnea=st=1566232200~exp=1566235800~acl=/hls/live/784034/Q001/*~hmac=4de929fdc0ee53199a11237ee75a695383a82a42623fa92c1376493e5fc91d7b",
            "http://playhls.media.nic.in/live/vyas/index.m3u8",
            "https://jk3lz82elw79-hls-live.5centscdn.com/harPalGeo/955ad3298db330b5ee880c2c9e6f23a0.sdp/playlist.m3u8",
            "https://dl.dropbox.com/s/96ziqh1ehkhzh81/badminton1.m3u8?dl=0",
            "http://imob.dunyanews.tv/live/_definst_/dunyalive_1/chunklist_w1239250459.m3u8",
            "https://content.uplynk.com/channel/3324f2467c414329b3b0cc5cd987b6be.m3u8",
            "http://amdlive-ch01.ctnd.com.edgesuite.net/arirang_1ch/smil:arirang_1ch.smil/playlist.m3u8",
            "https://news.cgtn.com/resource/live/english/cgtn-news.m3u8",
            "https://feeds.intoday.in/hltapps/api/master.m3u8",
            "https://5ad386ff92705.streamlock.net/live_transcoder/ngrp:zindagitv.stream_all/chunklist.m3u8",
            "https://ndtv24x7elemarchana.akamaized.net/hls/live/2003678/ndtv24x7/ndtv24x7master.m3u8",
            "https://liveprodapnortheast.global.ssl.fastly.net/ap1/Channel-APTVqvs-AWS-tokyo-1/Source-APTVqvs-1000-1_live.m3u8",
            "https://d2e1asnsl7br7b.cloudfront.net/7782e205e72f43aeb4a48ec97f66ebbe/index_5.m3u8",
            "https://2-fss-2.streamhoster.com/pl_138/201660-1270634-1/chunklist.m3u8",
            "http://cdn19.live247stream.com/channely/tv/playlist.m3u8",
            "http://imob.dunyanews.tv/live/_definst_/dunyalive_1/chunklist_w1239250459.m3u8?checkedby:iptvcat.com",
            "http://cdn11.live247stream.com/tag/tv/playlist.m3u8?checkedby:iptvcat.com",
            "https://streamer12.vdn.dstreamone.net/saazoawaz/saazoawaz/chunks.m3u8?checkedby:iptvcat.com",
            "https://5ad386ff92705.streamlock.net/live_transcoder/ngrp:zindagitv.stream_all/chunklist.m3u8?checkedby:iptvcat.com",
            "https://skynewsau-live.akamaized.net/hls/live/2002689/skynewsau-extra1/master.m3u8",
            "https://nbcnewshls-i.akamaihd.net/hls/live/1005170/nnn_live1/index.m3u8",
            "https://liveprodeuwest.akamaized.net/eu1/Channel-EUTVqvs-AWS-ireland-1/Source-EUTVqvs-1000-1_live.m3u8",
            "https://ndtv24x7elemarchana.akamaized.net/hls/live/2003678/ndtv24x7/ndtv24x7master.m3u8",
            "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master_3360.m3u8",
            "https://nmxlive.akamaized.net/hls/live/529965/Live_1/index.m3u8",
            "https://2-fss-2.themediacdn.com/pl_118/amlst:201982-1318992/playlist.m3u8",
            "http://cdn11.live247stream.com/tag/tv/playlist.m3u8",
            "https://chostv.mobie.in/choos/sports/beinxtra.m3u8"
    };

    // Must match length of urls[]
    String[] types = {
            "stream", // for m3u8 (ExoPlayer)
            "web",    // for YouTube (WebView iframe)
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
            "stream",
    };
    int currentIndex = 0;

    //volume
    Button touchOverlay;
    TextView volumeIndicator;
    AudioManager audioManager;
    float startY;

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

        //mobile next previw
        Button btnLeft = findViewById(R.id.btnLeft);
        Button btnRight = findViewById(R.id.btnRight);
        Button btnCenter = findViewById(R.id.btnCenter);
        // Left button → Previous channel
        btnLeft.setOnClickListener(v -> {
            currentIndex--;
            if (currentIndex < 0) currentIndex = urls.length - 1;
            loadChannel(currentIndex);
        });

        // Right button → Next channel
        btnRight.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex >= urls.length) currentIndex = 0;
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
        //end mobile preview

        //volume +-
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
        //volume+- end
    }

    private void loadChannel(int index) {

        Toast.makeText(LiveTvActivity.this, "Channel "+index,Toast.LENGTH_LONG).show();

        if (index < 0 || index >= urls.length) return;

        // Stop both before switching
        stopAllPlayers();

        String url = urls[index];
        String type = types[index];

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
                    + "<iframe width='100%' height='100%' src='" + url + "?autoplay=1&mute=0' "
                    + "frameborder='0' allow='autoplay; fullscreen' allowfullscreen></iframe>"
                    + "</body></html>";

            webView.loadData(html, "text/html", "utf-8");
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
                    if (currentIndex >= urls.length) currentIndex = 0;
                    loadChannel(currentIndex);
                    return true;

                case KeyEvent.KEYCODE_DPAD_LEFT: // Previous
                    currentIndex--;
                    if (currentIndex < 0) currentIndex = urls.length - 1;
                    loadChannel(currentIndex);
                    return true;

//----------------------------
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
//                    -------------------


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
}