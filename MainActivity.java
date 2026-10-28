package com.example.simplemusic;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText searchBox;
    private LinearLayout resultsLayout;
    private TextView status, nowPlaying;
    private Button playPause;
    private MediaPlayer player;
    private String currentUrl = "";
    private boolean prepared = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(20, 20, 35));
        getWindow().setNavigationBarColor(Color.rgb(20, 20, 35));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(12));
        root.setBackgroundColor(Color.rgb(16, 18, 30));

        TextView title = new TextView(this);
        title.setText("♫ Simple Music");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, 0, 0, dp(16));
        root.addView(title);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Search songs or artists");
        searchBox.setTextColor(Color.WHITE);
        searchBox.setHintTextColor(Color.LTGRAY);
        searchBox.setPadding(dp(12), 0, dp(8), 0);
        searchBox.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(125, 130, 160)));
        searchRow.addView(searchBox, new LinearLayout.LayoutParams(0, dp(52), 1f));

        Button searchButton = new Button(this);
        searchButton.setText("Search");
        searchRow.addView(searchButton, new LinearLayout.LayoutParams(dp(100), dp(52)));
        root.addView(searchRow);

        status = new TextView(this);
        status.setText("Search for a song to get started.");
        status.setTextColor(Color.LTGRAY);
        status.setPadding(0, dp(12), 0, dp(8));
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        resultsLayout = new LinearLayout(this);
        resultsLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(resultsLayout);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(65, 68, 90));
        root.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));

        nowPlaying = new TextView(this);
        nowPlaying.setText("Nothing playing");
        nowPlaying.setTextColor(Color.WHITE);
        nowPlaying.setTextSize(15);
        nowPlaying.setPadding(0, dp(10), 0, dp(4));
        root.addView(nowPlaying);

        playPause = new Button(this);
        playPause.setText("▶ Play");
        playPause.setOnClickListener(v -> togglePlayback());
        root.addView(playPause, new LinearLayout.LayoutParams(-1, dp(48)));

        setContentView(root);
        searchButton.setOnClickListener(v -> searchSongs());
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            searchSongs();
            return true;
        });
    }

    private void searchSongs() {
        String term = searchBox.getText().toString().trim();
        if (term.isEmpty()) {
            searchBox.setError("Type a song or artist name");
            return;
        }
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        status.setText("Searching…");
        resultsLayout.removeAllViews();
        executor.execute(() -> {
            try {
                String q = URLEncoder.encode(term, "UTF-8");
                URL url = new URL("https://itunes.apple.com/search?term=" + q + "&entity=song&limit=30&country=in");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                } finally { conn.disconnect(); }
                JSONObject json = new JSONObject(response.toString());
                JSONArray tracks = json.getJSONArray("results");
                ArrayList<JSONObject> songs = new ArrayList<>();
                for (int i = 0; i < tracks.length(); i++) {
                    JSONObject item = tracks.getJSONObject(i);
                    if (item.has("previewUrl") && !item.isNull("previewUrl")) songs.add(item);
                }
                runOnUiThread(() -> {
                    status.setText(songs.size() + " results • Previews are usually about 30 seconds");
                    if (songs.isEmpty()) {
                        status.setText("No playable previews found. Try another search.");
                        return;
                    }
                    for (JSONObject song : songs) addSongRow(song);
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Couldn't search right now. Check internet and try again."));
            }
        });
    }

    private void addSongRow(JSONObject song) {
        String name = song.optString("trackName", "Unknown song");
        String artist = song.optString("artistName", "Unknown artist");
        String album = song.optString("collectionName", "");
        String preview = song.optString("previewUrl", "");
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(6), dp(8));
        row.setBackgroundColor(Color.rgb(27, 30, 47));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, dp(5), 0, dp(5));
        resultsLayout.addView(row, rowParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView songName = new TextView(this);
        songName.setText(name);
        songName.setTextColor(Color.WHITE);
        songName.setTextSize(16);
        songName.setMaxLines(2);
        TextView artistName = new TextView(this);
        artistName.setText(artist + (album.isEmpty() ? "" : " • " + album));
        artistName.setTextColor(Color.LTGRAY);
        artistName.setTextSize(12);
        artistName.setMaxLines(2);
        labels.addView(songName);
        labels.addView(artistName);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        Button play = new Button(this);
        play.setText("▶");
        row.addView(play, new LinearLayout.LayoutParams(dp(56), dp(48)));
        play.setOnClickListener(v -> playSong(name, artist, preview));
    }

    private void playSong(String name, String artist, String url) {
        if (url == null || url.isEmpty()) {
            status.setText("No preview available for this song.");
            return;
        }
        releasePlayer();
        currentUrl = url;
        prepared = false;
        nowPlaying.setText("Loading: " + name + " — " + artist);
        playPause.setText("…");
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setDataSource(url);
            player.setOnPreparedListener(mp -> {
                prepared = true;
                mp.start();
                nowPlaying.setText("♫ " + name + " — " + artist);
                playPause.setText("⏸ Pause");
            });
            player.setOnCompletionListener(mp -> playPause.setText("▶ Play"));
            player.setOnErrorListener((mp, what, extra) -> {
                status.setText("Playback failed. Try another result.");
                playPause.setText("▶ Play");
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            status.setText("Couldn't play this preview. Try another song.");
            releasePlayer();
        }
    }

    private void togglePlayback() {
        if (player == null) {
            status.setText("Search and select a song first.");
            return;
        }
        if (!prepared) {
            status.setText("Song is still loading…");
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            playPause.setText("▶ Resume");
        } else {
            try {
                player.start();
                playPause.setText("⏸ Pause");
            } catch (Exception e) {
                status.setText("Couldn't resume playback.");
            }
        }
    }

    private void releasePlayer() {
        prepared = false;
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        releasePlayer();
        executor.shutdownNow();
        super.onDestroy();
    }
}
