package com.example.airport;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.Utils;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private BeaconManager beaconManager;
    private Region region;

    //private double RSSI_Default = -44; // provient de l'application estimote. [dBm]
    private List<Cercle> listCercles = new ArrayList<>();
    private Cercle estimatedPosition;
    //give the position of a beacon by its Major
    private static final Map<Integer, String> POSITION_BY_BEACON;
    private CircularFifoQueue<Cercle> logMesure;

    //Callibrage
    private static List<Integer> calibrage = new ArrayList<Integer>();
    private float tx_Power;
    private boolean isCallibrating;
    public static final String PREFS_NAME = "MyPrefsFile";


    //Accéléromètre
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate;
    private double acc_tot;
    private static final int ACCELERATION_TOTAL_THRESHOLD = 7;

    //Nombre de mesure nécessaire pour faire une estimation de la position de l'utilisateur.
    private static int MESURE_COMPTE_FOR_ESTIMATION=20;

    static {
        Map<Integer, String> positionByBeacon = new HashMap<>();
        //TODO placer et prendre la position des beacons
        positionByBeacon.put(1, "0:2"); //Le beacon avec la mineur 1 est à la position 5-5;
        positionByBeacon.put(2, "1.2:0"); //Le beacon avec la mineur 2 est à la position 5-5;
        positionByBeacon.put(3, "2.9:0"); //Le beacon avec la mineur 3 est à la position 5-5;
        positionByBeacon.put(4, "4:2"); //Le beacon avec la mineur 4 est à la position 5-5;
        positionByBeacon.put(5, "2.9:3.5"); //Le beacon avec la mineur 5 est à la position 5-5;
        positionByBeacon.put(6, "1.2:3.5"); //Le beacon avec la mineur 6 est à la position 5-5;

        POSITION_BY_BEACON = Collections.unmodifiableMap(positionByBeacon);
    }

    //Zones d'intérêts
    private Map<RectF, String> mapInterests;
    private RectF currentInterestZone;

    public void toast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Callibrage
        isCallibrating = false;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        tx_Power = settings.getFloat("beaconDefault1M_Power", 1000);

        if(tx_Power == 1000){
            changeToCalibrerView();
        }


        //Accéléromètre
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        lastUpdate = System.currentTimeMillis();
        acc_tot = 0;


        //Mesures
        logMesure = new CircularFifoQueue<Cercle>(MESURE_COMPTE_FOR_ESTIMATION);

        //Liste d'intérêts
        mapInterests = new HashMap<RectF,String>(2);
        mapInterests.put(new RectF(0,0,1,1), "1st Zone");
        mapInterests.put(new RectF(1.5f,2,4,3), "2cd Zone");
        currentInterestZone = null;



        beaconManager = new BeaconManager(this);

        beaconManager.setForegroundScanPeriod(200, 0);

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {

            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {

                if (!list.isEmpty()) {

                    Beacon test = list.get(0);

                    if(isCallibrating)
                        callibrer(test);
                    else if(list.size() >= 3) //Peut être qu'on peut faire la trilatération avec 2 beacons seulement
                    {
                        String position = "";
                        for (Beacon beacon : list) {
                            if (POSITION_BY_BEACON.containsKey(beacon.getMinor())) {
                                position = POSITION_BY_BEACON.get(beacon.getMinor());
                            } else {
                                Log.e("wrong Major", "The major of a beacon match nothing");
                            }
                            // Le beacon et sa distance représente un cerle centré sur sa position et de rayon égal à sa distance au téléphonne.
                            // Un objet cercle est instancié puis ajouté à une liste de cerles
                            listCercles.add(new Cercle(Double.valueOf(position.split(":")[0]), Double.valueOf(position.split(":")[1]), distanceToBeacon(beacon)));
                        }
                        Cercle myPosition = Trilateration.trilaterer2(listCercles);
                        Log.d("Position brute", String.format("x=%1$.3f, y=%2$.3f", myPosition.x, myPosition.y));
                        logMesure.add(myPosition);
                        if (logMesure.isAtFullCapacity()) {
                            estimatedPosition = computeAveragePosition();
                            Log.d("Position estimée", String.format("x=%1$.3f, y=%2$.3f", estimatedPosition.x, estimatedPosition.y));

                            for(RectF rect : mapInterests.keySet()){
                                if(rect.contains((float)estimatedPosition.x, (float)estimatedPosition.y))
                                {
                                    if(currentInterestZone != rect){
                                        showNotification("New Zone", mapInterests.get(rect));
                                        currentInterestZone = rect;
                                    }
                                }
                            }
                        }


                    }
                    else
                    {
                        showNotification("You're not in range of 3 beacons", "");
                    }

                    //Log.d("DEBUG:Name", String.valueOf(test.getProximityUUID()));
                    //Log.d("DEBUG:force du signal", String.valueOf(test.getRssi()));
                    //Log.d("DEBUG:distance", String.valueOf(distanceToBeacon(test)));
                    /*if (list.size() >= 10) {
                        if (distanceToBeacon(list.get(2)) > -1000) {
                            Log.d("weakest beacon's signal", list.get(2).getProximityUUID().toString() + " has a signal strengh of " + distanceToBeacon(list.get(2)));
                            String position = "0:0";

                        }
                    } else {
                        //
                    }*/
                }
            }
        });
        region = new Region("ranged region", null/*mettre uuid*/, null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);


        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(region);
            }
        });


        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {

        beaconManager.stopRanging(region);
        senSensorManager.unregisterListener(this);

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.callibrage:
                changeToCalibrerView();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void changeToCalibrerView()
    {
        setContentView(R.layout.callibrer);

        Button b = (Button) findViewById(R.id.Callibrer);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCallibrating = true;
            }
        });
    }




    // trouve le point le plus proche des trois cerles.
    /*private Cercle trilateration(List<Cercle> listCercles)
    {


    }*/

    //TODO estimer la fonction qui permet de convertir la force du signal d'un beacon à la distance du beacon au téléphonne.

    public void callibrer(Beacon beacon){

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBarCallibrer);

        if(calibrage.size() < 20){
            calibrage.add(new Integer(beacon.getRssi()));
            progressBar.setProgress(calibrage.size() * 5);

        }
        else
        {
            Collections.sort(calibrage);
            float total = 0;
            Iterator<Integer> iterator = calibrage.iterator();
            for (int i = 0; i < 1; i++) {
                iterator.next();
            }

            int j = 0;
            for (int i = 0; i < 18; i++) {
                total += iterator.next().doubleValue();
                j++;
            }
            Log.d("DEBUG:cool", String.valueOf(calibrage.size()));
            Log.d("DEBUG:cool2", ""+j);
            float moyenne = total / calibrage.size()-2;
            Log.d("DEBUG:coolMoy", String.valueOf(moyenne));

            tx_Power = moyenne;

            isCallibrating=false;
            calibrage.clear();

            //make changes in cache
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat("beaconDefault1M_Power", tx_Power);

            // Commit the edits!
            editor.commit();


            MainActivity.this.toast("Callibré");

        }
    }


    double distanceToBeacon(Beacon beacon) {
    /*
     * RSSI = TxPower - 10 * n * lg(d)
     * n = 2 (in free space)
     *
     * d = 10 ^ ((TxPower - RSSI) / (10 * n))
     * RSSI est la force du signal reçu par le téléphone
     * TxPower est la force du signal reçu par le téléphone à 1 mètre du beacon!
     *
     */


        double rssi= (double) beacon.getRssi();


        //Log.d("DEBUG:dist2", String.valueOf(Utils.computeAccuracy(beacon)));
        return 0.42093*Math.pow(rssi/tx_Power, 6.9476)+0.54992;//Math.pow(10d, (txPower - rssi) / (10 * 2));
    }


    //Calcule la moyenne des positions calculées
    public Cercle computeAveragePosition()
    {
        double sumX=0;
        double sumY=0;
        double size = logMesure.size();
        for (Cercle position:logMesure) {
            sumX+=position.x;
            sumY+=position.y;
        }
        return new Cercle(sumX/size,sumY/size,5);
    }

    public void showNotification(String title, String message) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[]{notifyIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            double acc = Math.sqrt(x*x + y*y + z*z);
            acc_tot += acc;

            long curTime = System.currentTimeMillis();
            long diffTime = curTime - lastUpdate;

            if ((diffTime) > 500) {
                lastUpdate = curTime;

                if(acc_tot < ACCELERATION_TOTAL_THRESHOLD )
                {
                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    if(imageView != null)
                    {
                        imageView.setBackgroundResource(R.drawable.ok);
                    }
                }
                else if (acc_tot > ACCELERATION_TOTAL_THRESHOLD + 3)
                {
                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    if(imageView != null)
                    {
                        imageView.setBackgroundResource(R.drawable.wrong);
                    }
                }

                Log.d("DEBUG:acc totale ", ""+acc_tot);


                acc_tot = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
