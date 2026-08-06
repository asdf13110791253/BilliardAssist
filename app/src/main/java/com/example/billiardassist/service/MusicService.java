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
    private List<Integer> playedList;  // 已播放列表
    private int currentIndex = 0;

    // 🎵 在这里添加你的音乐文件（R.raw.xxx）
    private final List<Integer> MUSIC_RESOURCES = Arrays.asList(
        R.raw.music_01,
        R.raw.music_02,
        R.raw.music_03,
        R.raw.music_04,
        R.raw.music_05
        // 继续添加更多音乐...
    );

    @Override
    public void onCreate() {
        super.onCreate();
        initPlaylist();
    }

    private void initPlaylist() {
        // 创建播放列表副本
        musicList = new ArrayList<>(MUSIC_RESOURCES);
        playedList = new ArrayList<>();

        // 随机打乱
        Collections.shuffle(musicList);
        currentIndex = 0;

        // 开始播放
        playNext();
    }

    private void playNext() {
        // 释放旧播放器
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // 检查列表是否为空
        if (musicList == null || musicList.isEmpty()) {
            return;
        }

        // 如果所有歌曲都已播放完，重新洗牌
        if (currentIndex >= musicList.size()) {
            // 重置已播放列表
            playedList.clear();
            // 重新洗牌
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

                // 播放完成自动播下一首
                mediaPlayer.setOnCompletionListener(mp -> {
                    // 记录已播放
                    playedList.add(resId);
                    currentIndex++;
                    playNext();
                });

                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 出错时跳下一首
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
