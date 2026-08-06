package com.example.billiardassist.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.billiardassist.App;
import com.example.billiardassist.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private List<Integer> musicList;
    private int currentIndex = 0;

    // 🎵 3首歌
    private final List<Integer> MUSIC_RESOURCES = Arrays.asList(
        R.raw.music_01,
        R.raw.music_02,
        R.raw.music_03
    );

    @Override
    public void onCreate() {
        super.onCreate();
        initPlaylist();
    }

    private void initPlaylist() {
        musicList = new ArrayList<>(MUSIC_RESOURCES);
        Collections.shuffle(musicList);
        currentIndex = 0;
        playNext();
    }

    private void playNext() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (musicList == null || musicList.isEmpty()) {
            return;
        }

        if (currentIndex >= musicList.size()) {
            Collections.shuffle(musicList);
            currentIndex = 0;
        }

        try {
            int resId = musicList.get(currentIndex);
            mediaPlayer = MediaPlayer.create(this, resId);

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(false);
                float volume = App.getInstance().getMusicVolume();
                mediaPlayer.setVolume(volume, volume);

                mediaPlayer.setOnCompletionListener(mp -> {
                    currentIndex++;
                    playNext();
                });

                mediaPlayer.start();
                currentIndex++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            currentIndex++;
            playNext();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            playNext();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
