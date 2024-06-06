package com.stardust.autojs.runtime.api;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;

import com.stardust.autojs.core.eventloop.EventEmitter;
import com.stardust.autojs.core.looper.Loopers;
import com.stardust.autojs.runtime.ScriptBridges;
import com.stardust.autojs.runtime.ScriptRuntime;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Stardust on 2018/2/5.
 */

public class Sensors extends EventEmitter {


    public class SensorEventEmitter extends EventEmitter implements SensorEventListener {

        public SensorEventEmitter(ScriptBridges bridges) {
            super(bridges);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            Object[] args = new Object[event.values.length + 1];
            args[0] = event;
            for (int i = 1; i < args.length; i++) {
                args[i] = event.values[i - 1];
            }
            emit("change", args);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            emit("accuracy_change", accuracy);
        }

        public void unregister() {
            Sensors.this.unregister(this);
        }
    }

    public static class Delay {
        public static final int normal = SensorManager.SENSOR_DELAY_NORMAL;
        public static final int ui = SensorManager.SENSOR_DELAY_UI;
        public static final int game = SensorManager.SENSOR_DELAY_GAME;
        public static final int fastest = SensorManager.SENSOR_DELAY_FASTEST;
    }


    private static final Map<String, Integer> SENSORS =   Map.ofEntries(
            Map.entry("ACCELEROMETER", Sensor.TYPE_ACCELEROMETER),
            Map.entry("MAGNETIC_FIELD", Sensor.TYPE_MAGNETIC_FIELD),
            Map.entry("ORIENTATION", Sensor.TYPE_ORIENTATION),
            Map.entry("GYROSCOPE", Sensor.TYPE_GYROSCOPE),
            Map.entry("LIGHT", Sensor.TYPE_LIGHT),
            Map.entry("TEMPERATURE", Sensor.TYPE_TEMPERATURE),
            Map.entry("PRESSURE", Sensor.TYPE_PRESSURE),
            Map.entry("AMBIENT_TEMPERATURE", Sensor.TYPE_AMBIENT_TEMPERATURE),
            Map.entry("PROXIMITY", Sensor.TYPE_PROXIMITY),
            Map.entry("GRAVITY", Sensor.TYPE_GRAVITY),
            Map.entry("LINEAR_ACCELERATION", Sensor.TYPE_LINEAR_ACCELERATION),
            Map.entry("RELATIVE_HUMIDITY", Sensor.TYPE_RELATIVE_HUMIDITY)
    );

    public boolean ignoresUnsupportedSensor = false;
    public final Delay delay = new Delay();

    private final Set<SensorEventEmitter> mSensorEventEmitters = new HashSet<>();
    private final SensorManager mSensorManager;
    private final ScriptBridges mScriptBridges;
    private final SensorEventEmitter mNoOpSensorEventEmitter;
    private final ScriptRuntime mScriptRuntime;
    private final Loopers.AsyncTask mAsyncTask = new Loopers.AsyncTask("Sensors");


    public Sensors(Context context, ScriptRuntime runtime) {
        super(runtime.bridges);
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mScriptBridges = runtime.bridges;
        mNoOpSensorEventEmitter = new SensorEventEmitter(runtime.bridges);
        mScriptRuntime = runtime;
    }

    public SensorEventEmitter register(String sensorName) {
        return register(sensorName, Delay.normal);
    }

    public SensorEventEmitter register(String sensorName, int delay) {
        if (sensorName == null)
            throw new NullPointerException("sensorName = null");
        Sensor sensor = getSensor(sensorName);
        if (sensor == null) {
            if (ignoresUnsupportedSensor) {
                emit("unsupported_sensor", sensorName);
                return mNoOpSensorEventEmitter;
            } else {
                return null;
            }
        }
        return register(sensor, delay);
    }

    private SensorEventEmitter register(@NonNull Sensor sensor, int delay) {
        mScriptRuntime.loopers.addAsyncTask(mAsyncTask);
        SensorEventEmitter emitter = new SensorEventEmitter(mScriptBridges);
        mSensorManager.registerListener(emitter, sensor, delay);
        synchronized (mSensorEventEmitters) {
            mSensorEventEmitters.add(emitter);
        }
        return emitter;
    }

    public Sensor getSensor(String sensorName) {
        Integer type = SENSORS.get(sensorName.toUpperCase());
        if (type == null)
            type = getSensorTypeByReflect(sensorName);
        if (type == null)
            return null;
        return mSensorManager.getDefaultSensor(type);
    }

    private Integer getSensorTypeByReflect(String sensorName) {
        sensorName = sensorName.toUpperCase();
        try {
            Field field = Sensor.class.getField("TYPE_" + sensorName);
            return (Integer) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    public void unregister(SensorEventEmitter emitter) {
        if (emitter == null)
            return;
        synchronized (mSensorEventEmitters) {
            mSensorEventEmitters.remove(emitter);
            if (mSensorEventEmitters.isEmpty())
                mScriptRuntime.loopers.removeAsyncTask(mAsyncTask);
        }
        mSensorManager.unregisterListener(emitter);
    }

    public void unregisterAll() {
        synchronized (mSensorEventEmitters) {
            for (SensorEventEmitter emitter : mSensorEventEmitters) {
                mSensorManager.unregisterListener(emitter);
            }
            mSensorEventEmitters.clear();
        }
        mScriptRuntime.loopers.removeAsyncTask(mAsyncTask);
    }
}
