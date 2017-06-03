package org.opencv.samples.tutorial2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.Date;

public class OverlayView extends View implements SensorEventListener {

    public static final String DEBUG_TAG = "OverlayView Log";

    private final Context context;
    private Handler handler;
    private Bundle bundle = new Bundle();
    private int canWid;
    private int canH;
    private Location[] inters;
    private Date date;
    private Calendar start;
    private Paint newpaint;
    private Paint backPaint;
    private Paint newpaint2;

    String accelData = "Accelerometer Data";
    String compassData = "Compass Data";
    String gyroData = "Gyro Data";

    private SensorManager sensors = null;
    private Location lastLocation;

    private float verticalFOV;
    private float horizontalFOV;
    public int which = 1;

    private boolean isAccelAvailable;
    private boolean isCompassAvailable;
    private boolean isGyroAvailable;
    private Sensor accelSensor;
    private Sensor compassSensor;
    private Sensor gyroSensor;
    private Bitmap icon;
    private int width;
    private int height;
    private TextPaint contentPaint;
    LatLng[] inter;
    String[] aqi;
    String[] streets;
    String temp;
    String hum;
    protected float[] gravSensorVals;
    protected float[] magSensorVals;
    static final float ALPHA = 0.17f;



    public OverlayView(Context context) {
        super(context);
        this.context = context;
        this.handler = new Handler();


        sensors = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroSensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        startSensors();

        // get some camera parameters
        Camera camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        verticalFOV = params.getVerticalViewAngle();
        horizontalFOV = params.getHorizontalViewAngle();
        camera.release();

        // paint for text
        contentPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        contentPaint.setTextAlign(Align.LEFT);
        contentPaint.setTextSize(20);
        contentPaint.setColor(Color.WHITE);

        newpaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        newpaint.setTextAlign(Align.CENTER);
        newpaint.setTextSize(130);
        newpaint.setStrokeWidth(20);
        newpaint.setStyle(Paint.Style.FILL_AND_STROKE);
        newpaint.setColor(Color.BLACK);

        newpaint2 = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        newpaint2.setTextAlign(Align.CENTER);
        newpaint2.setTextSize(130);
        newpaint2.setStrokeWidth(15);
        newpaint2.setStyle(Paint.Style.FILL);
        newpaint2.setColor(Color.WHITE);

        backPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        backPaint.setTextAlign(Align.CENTER);
        backPaint.setColor(Color.BLACK);


        icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.bubble);
        height = icon.getHeight();
        width = icon.getWidth();
        date = new Date();
        start = Calendar.getInstance();
        start.setTime(date);
    }

    public void setData(Bundle data)
    {
        bundle =data;
        lastLocation = bundle.getParcelable("location");
    }



    private void startSensors() {
        isAccelAvailable = sensors.registerListener(this, accelSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        isCompassAvailable = sensors.registerListener(this, compassSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        isGyroAvailable = sensors.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }



    @Override
    protected void onDraw(Canvas canvas) {

        if (inter == null || aqi == null){
            Log.d(DEBUG_TAG, "NULL STILL");
            return;
        }
        else {
            inters = new Location[inter.length];
            for (int i = 0; i < inters.length; i++){
                inters[i] = new Location("manual");
                inters[i].setLatitude(inter[i].latitude);
                inters[i].setLongitude(inter[i].longitude);
                inters[i].setAltitude(0);
            }
        }
        for (int i = 0; i < aqi.length; i++){
            if ( aqi[i] == null) return;
        }


        //Log.d(DEBUG_TAG, "onDraw");
        super.onDraw(canvas);
        canWid = canvas.getWidth()/2;
        canH = canvas.getHeight()/2;
        // Draw something fixed (for now) over the camera view


        StringBuilder text = new StringBuilder(accelData).append("\n");
        text.append(compassData).append("\n");
        text.append(gyroData).append("\n");
        for (int i = 0; i < inters.length; i++) {
            float curBearing = lastLocation.bearingTo(inters[i]);
            // compute rotation matrix
            float rotation[] = new float[9];
            float identity[] = new float[9];
            if (magSensorVals != null && gravSensorVals != null) {
                boolean gotRotation = SensorManager.getRotationMatrix(rotation,
                        identity, gravSensorVals, magSensorVals);
                if (gotRotation) {
                    float cameraRotation[] = new float[9];
                    // remap such that the camera is pointing straight down the Y
                    // axis
                    SensorManager.remapCoordinateSystem(rotation,
                            SensorManager.AXIS_X, SensorManager.AXIS_Z,
                            cameraRotation);

                    // orientation vector
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(cameraRotation, orientation);

                    // draw horizon line (a nice sanity check piece) and the target (if it's on the screen)
                    canvas.save();
                    // use roll for screen rotation
                    canvas.rotate((float) (0.0f - Math.toDegrees(orientation[2])));

                    // Translate, but normalize for the FOV of the camera -- basically, pixels per degree, times degrees == pixels
                    float dx = (float) ((canvas.getWidth() / horizontalFOV) * (Math.toDegrees(orientation[0]) - curBearing));
                    float dy = (float) ((canvas.getHeight() / verticalFOV) * Math.toDegrees(orientation[1]));


                    // wait to translate the dx so the horizon doesn't get pushed off
                    canvas.translate(0.0f, 0.0f - dy);

                    canvas.rotate(270f);

                    // now translate the dx
                    canvas.translate(0.0f - dx, 0.0f);

                    // draw our point -- we've rotated and translated this to the right spot already
                    //canvas.drawCircle(canvas.getWidth()/2, canvas.getHeight()/2, 70.0f, targetPaint);
                    int cx = (canWid) >> 1; // same as (...) / 2
                    int cy = (canH) >> 1;
                    if (which == 1) {
                        canvas.drawText("AQI: " + aqi[i], cx, cy, newpaint);
                        canvas.drawText("AQI: " + aqi[i], cx, cy, newpaint2);
                    }
                    else if (which == 2){
                        canvas.drawText("Humidity: "+ hum+"%", cx, cy, newpaint);
                        canvas.drawText("Humidity: "+ hum+"%", cx, cy, newpaint2);
                    }
                    else if (which == 3){
                        canvas.drawText("Temperature: "+temp+" Celsius", cx, cy, newpaint);
                        canvas.drawText("Temperature: "+temp+" Celsius", cx, cy, newpaint2);
                    }
                    String[] parts = streets[i].split(" and ");
                    canvas.drawText(parts[0],cx, cy+newpaint.getTextSize(), newpaint);
                    canvas.drawText(parts[1],cx, cy+newpaint.getTextSize()*2, newpaint);
                    canvas.drawText(parts[0],cx, cy+newpaint.getTextSize(), newpaint2);
                    canvas.drawText(parts[1],cx, cy+newpaint.getTextSize()*2, newpaint2);
                    canvas.restore();
                }
            }
        }

        canvas.save();
        canvas.translate(15.0f, 15.0f);
        StaticLayout textBox = new StaticLayout(text.toString(), contentPaint,
                480, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        textBox.draw(canvas);
        canvas.restore();
    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        Log.d(DEBUG_TAG, "onAccuracyChanged");

    }

    public void onSensorChanged(SensorEvent event) {
        // Log.d(DEBUG_TAG, "onSensorChanged");
        StringBuilder msg = new StringBuilder(event.sensor.getName())
                .append(" ");
        for (float value : event.values) {
            msg.append("[").append(String.format("%.3f", value)).append("]");
        }

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravSensorVals = lowPass(event.values.clone(), gravSensorVals);
                accelData = msg.toString();
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroData = msg.toString();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magSensorVals = lowPass(event.values.clone(), magSensorVals);
                compassData = msg.toString();
                break;
        }
        this.invalidate();
    }

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public void onProviderDisabled(String provider) {
        // ...
    }

    public void onProviderEnabled(String provider) {
        // ...
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // ...
    }

    // this is not an override
    public void onPause() {
        sensors.unregisterListener(this);
    }

    // this is not an override
    public void onResume() {
        startSensors();
    }
}
