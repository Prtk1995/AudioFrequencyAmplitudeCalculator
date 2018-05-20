package com.prateek.audiocalculator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.prateek.audiocalculator.audio.calculators.AudioCalculator;
import com.prateek.audiocalculator.audio.core.Callback;
import com.prateek.audiocalculator.audio.core.Recorder;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    private Recorder recorder;
    private AudioCalculator audioCalculator;
    private Handler handler;
    private TextView tvAmplitude, tvFrequency, tvMinFrequency, tvMinAmplitude, tvMaxFrequency, tvMaxAmplitude;
    private TextView tvStart;
    private String LOG_TAG = MainActivity.class.getSimpleName();

    /*Final List*/
    private List<Integer> finalAmplitudesList = new ArrayList<>();
    private List<Integer> finalFrequenciesList = new ArrayList<>();
    private List<Integer> finalAmplitudeAverageList = new ArrayList<>();
    private List<Integer> finalFrequencyAverageList = new ArrayList<>();
    private CountDownTimer countDownTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestAudioPermission();
        recorder = new Recorder(callback);
        audioCalculator = new AudioCalculator();
        handler = new Handler(Looper.getMainLooper());
        tvStart = findViewById(R.id.tv_start_recording);
        tvAmplitude = findViewById(R.id.tv_Amplitude);
        tvFrequency = findViewById(R.id.tv_Frequency);
        tvMinFrequency = findViewById(R.id.min_frequency);
        tvMaxFrequency = findViewById(R.id.max_frequency);
        tvMaxAmplitude = findViewById(R.id.max_amplitude);
        tvMinAmplitude = findViewById(R.id.min_amplitude);
        tvStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }


    private Callback callback = new Callback() {
        @Override
        public void onBufferAvailable(byte[] buffer) {
            audioCalculator.setBytes(buffer);
            int amplitudeReceived = audioCalculator.getAmplitude();
            final int amplitude = (int) ((amplitudeReceived / 32767.0) * 100);
            final double frequency = audioCalculator.getFrequency();
            finalAmplitudesList.add(amplitude);
            finalFrequenciesList.add((int) frequency);
            handler.post(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    tvAmplitude.setText("Amplitude = " + amplitude);
                    tvFrequency.setText("Frequency = " + frequency);
                }
            });
        }
    };


    /**
     * Recording and storing are saved on the main thread
     */
    public void startRecording() {
        if (tvStart.getText().toString().equalsIgnoreCase(getString(R.string.start))) {
            if (isPermisisonGranted()) {
                tvStart.setText(R.string.stop);

                recorder.start();
                countDownTimer = new CountDownTimer(60*1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        stopRecordingAndEvaluateData();
                    }
                };
                countDownTimer.start();
            }else {
                Toast.makeText(this,"Please allow permission to continue",Toast.LENGTH_SHORT).show();
                requestAudioPermission();
            }
        } else if (tvStart.getText().toString().equalsIgnoreCase(getString(R.string.stop))) {
            stopRecordingAndEvaluateData();
            if (countDownTimer != null) {
                countDownTimer.cancel();
                Toast.makeText(this,"Audio stored at = /storage/emulated/0/AudioRecorder/AudioRecorder.wav"
                        , Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This method returns true if both the permisison is granted
     *
     * @return true if permissions are granted
     */
    private boolean isPermisisonGranted() {
        if (checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * This method returns minimum and maimum frequency
     */
    private void getMinAndMaxFrequencies() {

        double min = finalFrequenciesList.get(0);
        double max = finalFrequenciesList.get(0);
        for (int i = 0; i < finalFrequenciesList.size(); i++) {

            if (finalFrequenciesList.get(i) <= min) {
                min = finalFrequenciesList.get(i);
            }

            if (finalFrequenciesList.get(i) >= max) {
                max = finalFrequenciesList.get(i);
            }
        }

        final double finalMax = max;
        final double finalMin = min;

        double ampMin = finalAmplitudesList.get(0);
        double ampMax = finalAmplitudesList.get(0);

        for (int i = 0; i < finalAmplitudesList.size(); i++) {
            if (finalAmplitudesList.get(i) <= ampMin) {
                ampMin = finalAmplitudesList.get(i);
            }

            if (finalAmplitudesList.get(i) >= ampMax) {
                ampMax = finalAmplitudesList.get(i);
            }
        }

        final double amplitudeFinalMax = ampMax;
        final double amplitudeFinalMin = ampMin;
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                tvMaxAmplitude.setText("Maximum amplitude = " + String.valueOf(amplitudeFinalMax));
                tvMinAmplitude.setText("Minimum amplitude = " + String.valueOf(amplitudeFinalMin));
                tvMaxFrequency.setText("Maximum frequency = " + String.valueOf(finalMax));
                tvMinFrequency.setText("Minimum frequency = " + String.valueOf(finalMin));
            }
        });
    }

    /**
     * This method is used to stop recording and update the data
     */
    private void stopRecordingAndEvaluateData() {
        tvStart.setText(getString(R.string.start));
        recorder.stop();
        analyzeData();
        getMinAndMaxFrequencies();
        finalFrequenciesList.clear();
        finalAmplitudesList.clear();
        finalFrequencyAverageList.clear();
        finalAmplitudeAverageList.clear();
    }

    private void analyzeData() {

        int subArraySize = finalAmplitudesList.size() / 72;
        int sum = 0, average;

        if (subArraySize > 0) {
            for (int i = 0; i < 72; i++) {
                sum = 0;
                average = 0;
                for (int j = i * subArraySize; j < (i + 1) * subArraySize; j++) {
                    sum = sum + finalAmplitudesList.get(j);
                }
                average = sum / subArraySize;
                Log.d(LOG_TAG, "Amplitude = " + average);
                finalAmplitudeAverageList.add(average);
            }


            for (int i = 0; i < 72; i++) {
                sum = 0;
                average = 0;
                for (int j = i * subArraySize; j < (i + 1) * subArraySize; j++) {
                    sum = sum + finalFrequenciesList.get(j);
                }
                average = sum / subArraySize;
                Log.d(LOG_TAG, "Amplitude = " + average);
                finalFrequencyAverageList.add(average);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        recorder.stop();
    }
}
