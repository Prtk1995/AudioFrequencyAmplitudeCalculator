package com.prateek.audiocalculator.audio.core;

public interface Callback {
    void onBufferAvailable(byte[] buffer);
}