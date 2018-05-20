package com.prateek.audiocalculator.audio.core;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Recorder {

    /*PARAMETER USED IN AUDIO RECORDER*/
    private int audioSource = MediaRecorder.AudioSource.MIC;
    private int sampleRate = 44100;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder;
    private int bufferSize = 200000;
//    short[] buffer;
    private boolean isRecording = false;


    private Thread recordingThread = null;

    /* For file name */
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = "AudioRecorder.wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";


    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_CHANNELS_INT = 1;



    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private Thread thread;
    private Callback callback;
    private String filePath="";
    private String LOG_TAG = Recorder.class.getSimpleName();
    private FileOutputStream os;
    private byte[] buffer;
    private short[] writeBuffer;
    private short[] data;

    public Recorder() {
    }

    public Recorder(Callback callback) {
        this.callback = callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void start() {
        if (thread != null) return;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

                int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding);

                recorder = new AudioRecord(
                        audioSource,
                        sampleRate,
                        channelConfig,
                        audioEncoding,
                        minBufferSize);

               if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                    Thread.currentThread().interrupt();
                    return;
                } else {
                    Log.i(Recorder.class.getSimpleName(), "Started.");
                }
                buffer = new byte[minBufferSize];
                recorder.startRecording();
                makeInputFileAndInputStream();
                isRecording = true;
                while (thread != null && !thread.isInterrupted()&& recorder!=null &&
                        recorder.read(buffer, 0, minBufferSize) > 0) {
                    callback.onBufferAvailable(buffer);
                    writeAudioDataToFile(buffer);
                }

                recorder.stop();
                recorder.release();
                stopInputStream();

            }
        }, Recorder.class.getName());
        thread.start();
        isRecording = true;
    }



    public void stop() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
            isRecording = false;
            stopInputStream();
        }
    }


    private void makeInputFileAndInputStream(){
        String filename = getTempFilename();
        os = null;

        try {
            //open the output Stream for writing data
            os = new FileOutputStream(filename);//creating a stream to write dat to the send location

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void stopInputStream(){
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // copy the recorded file to original copy & delete the recorded copy
        copyWaveFile(getTempFilename(), getFilename());//passing a file name forom where to read data and where to write data
        deleteTempFile();
    }



    private short[] convertByteArrayToShort(byte[] byteArray){
        int size = byteArray.length;
        short[] shortArray = new short[size];

        for (int index = 0; index < size; index++) {
            shortArray[index] = (short) byteArray[index];
        }
        return shortArray;
    }

    // stores the file into the SDCARD
    private String getFilename() {
        System.out.println("---3---");
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        Log.d(LOG_TAG,"path = "+ file.getAbsolutePath() + "/" + AUDIO_RECORDER_FILE_EXT_WAV);
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_FILE_EXT_WAV);
    }


    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
        Log.d(LOG_TAG, "File Deleted");
    }



    private void writeAudioDataToFile(byte[] buffer) {

        if (null != os) {
                try {
                    byte[] bytes2 = new byte[buffer.length];
                    ByteBuffer.wrap(bytes2).order(ByteOrder.LITTLE_ENDIAN).put(buffer);
                    os.write(bytes2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        System.out.println("---8---");
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = sampleRate;
        int channels = RECORDER_CHANNELS_INT;
        long byteRate = RECORDER_BPP * sampleRate * channels / 8;


        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            byte[] bytes2 = new byte[buffer.length];
            ByteBuffer.wrap(bytes2).order(ByteOrder.LITTLE_ENDIAN).put(buffer);
            //now creating another byte array and writing it in the output stream
            while (in.read(bytes2) != -1) {
                out.write(bytes2);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    // stores the file into the SDCARD
    private String getTempFilename() {
        // Creates the temp file to store buffer
        System.out.println("---4-1--");
        String filepath = Environment.getExternalStorageDirectory().getPath();
        System.out.println("---4-2--");
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        System.out.println("---4-3--");

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);
        System.out.println("---4-4--");

        if (tempFile.exists())
            tempFile.delete();
        System.out.println("---4-5--");
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }




    /**
     * In this method it is writing some information in the header of the file,
     * I think it is used to by the media player to understand that this is an audio file
     * @param out output Stream
     * @param totalAudioLen audio length calculated from the input stream and added some extra bit to it
     * @param totalDataLen total data length calculated from the input stream
     * @param longSampleRate - sample rate used while recording audio
     * @param channels channels
     * @param byteRate byte rate for the audio
     * @throws IOException if unable to perform read write operation
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        System.out.println("---9---");
        byte[] header = new byte[4088];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) RECORDER_CHANNELS_INT;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (RECORDER_CHANNELS_INT * RECORDER_BPP / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 4088);
    }
}
