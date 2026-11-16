package com.lessonbell.watch;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.Build;
import android.media.AudioAttributes;

public final class Vibration {
    private Vibration() {}
    /** Highest requested actuator amplitude, distinct patterns; device may cap by hardware/settings. */
    public static void buzz(Context context, int minutes) {
        Vibrator v;
        if(Build.VERSION.SDK_INT>=31) v=((VibratorManager)context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
        else v=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        if(v==null || !v.hasVibrator())return;
        // 10m: two strong pulses; 5m: three; 2m: five strong pulses.
        int pulses=minutes==10?2:minutes==5?3:5;
        long[] t=new long[pulses*2]; int[] amps=new int[pulses*2];
        for(int i=0;i<pulses;i++) { t[i*2]=i==0?1:170; amps[i*2]=0; t[i*2+1]=330; amps[i*2+1]=255; }
        VibrationEffect effect=v.hasAmplitudeControl()?VibrationEffect.createWaveform(t,amps,-1)
                :VibrationEffect.createWaveform(t,-1);
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build();
        v.vibrate(effect,attributes);
    }
}
