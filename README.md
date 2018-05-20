# AudioFrequencyAmplitudeCalculator

This app helps user to calculate the **Amplitude and Frequency** at run time.
It also stores the recorded audio in the ".wav" format in the secondary storage.

The apps require
***Audio permission*** - for recording purpose  && ***Write external storage permission*** - for storing the recorded file

## Process :- 
1. Recording process is initiated by starting a new thread from Main thread.
2. Then Start reading the bytes[] received by the AudioRecorder Class.
3. We perform some opration and store the byte[] received directly into the raw file.
4. Once audio recoding has been stopped we copy the data fom the raw file to the *.wav* file after applying headeer into it.

Note * Value of frequenct changes very quickly depending upon the sample rate. You can store the values inside a list and perform operations there after.


![amplitude and freq calculator](https://user-images.githubusercontent.com/39397821/40279497-33e5698a-5c61-11e8-92f2-fa729114c192.png)


 :+1: Happy Coding !
