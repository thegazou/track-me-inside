package ch.hearc.trackmeinside;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //L'id de la vue actuelle
    private int currentViewId;

    //Ecoute de la région
    private BeaconManager beaconManager;
    private Region region;

    //Dictionnaire de positions des beacons selon leur "Minor"
    private static final Map<Integer, String> POSITION_BY_BEACON;
    //Positionnement actuelle
    private Cercle estimatedPosition;
    //Liste des dernières positions mesurées
    private CircularFifoQueue<Cercle> logMesure;

    //Callibrage
    private static List<Integer> calibrage = new ArrayList<Integer>();
    private float tx_Power;
    private final float CALIBRAGE_UNSET = 1000;
    private boolean isCallibrating;
    public static final String PREFS_NAME = "MyPrefsFile";

    //Accéléromètre
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate;
    private double acc_tot;
    private static final int ACCELERATION_TOTAL_THRESHOLD_INF = 3;
    private static final int ACCELERATION_TOTAL_THRESHOLD_SUP = 5;
    private int idleIteration;

    //Nombre de mesure nécessaire pour faire une estimation de la position de l'utilisateur.
    private static int MESURE_COMPTE_FOR_ESTIMATION=20;

    static {
        Map<Integer, String> positionByBeacon = new HashMap<>();
        positionByBeacon.put(4, "1.5:0"); //Le beacon avec la mineur 1 est à la position 1.5m en x, 0m en y
        positionByBeacon.put(5, "3.8:2.1"); //Le beacon avec la mineur 2 est à la position 3.8m, 2.1m
        positionByBeacon.put(6, "0:2.1"); //Le beacon avec la mineur 3 est à la position 0m, 2.1m
        POSITION_BY_BEACON = Collections.unmodifiableMap(positionByBeacon);
    }

    //Zones d'intérêts
    private List<PositionnalContent> listPositionnalContent;
    private PositionnalContent currentPositionnalContent;

    //Démarrage de la visite
    private boolean is_started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeToMainView();

        //Démarrage de la visite
        is_started = false;

        //Callibrage
        isCallibrating = false;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        tx_Power = settings.getFloat("beaconDefault1M_Power", CALIBRAGE_UNSET);

        if(tx_Power == CALIBRAGE_UNSET){
            changeToCallibrerView();
        }

        //Accéléromètre
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        lastUpdate = System.currentTimeMillis();
        acc_tot = 0;
        idleIteration = 0;

        //Mesures
        logMesure = new CircularFifoQueue<Cercle>(MESURE_COMPTE_FOR_ESTIMATION);

        //Liste d'intérêts
        listPositionnalContent = new ArrayList<PositionnalContent>(2);
        listPositionnalContent.add(new PositionnalContent(0.7f, 0, 2.5f, 0.6f, R.drawable.discobole, R.string.contentDiscobole));
        listPositionnalContent.add(new PositionnalContent(0, 1, 3.8f, 2.1f, R.drawable.joconde, R.string.contentJoconde));
        currentPositionnalContent = null;

        //Ecoute de la région
        region = new Region("ranged region", UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);
        beaconManager = new BeaconManager(this);
        beaconManager.setForegroundScanPeriod(200, 0); //scan pendant 200ms, avec 0ms de délai entre chaque scan
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {

            private int k = 0;
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                k++;

                if (!list.isEmpty()) {

                    if(isCallibrating) {
                        callibrer(list.get(0));

                    }
                    else if(list.size() >= 2 && is_started) //Peut être qu'on peut faire la trilatération avec 2 beacons seulement
                    {
                        if(is_started)
                        {
                            List<Cercle> listCercles = new ArrayList<>();
                            String position = "";
                            Beacon beacon;
                            int taille = (list.size()==2 )? 2:3;
                            for (int i=0;i<taille;i++) {
                                beacon = list.get(i);
                                if (POSITION_BY_BEACON.containsKey(beacon.getMinor())) {
                                    position = POSITION_BY_BEACON.get(beacon.getMinor());
                                } else {
                                    Log.e("WRONG MINOR", "The minor of a beacon match nothing : "+beacon.getMinor() +" - RSSI : " +beacon.getRssi());
                                }
                                // Le beacon et sa distance représente un cercle centré sur sa position et de rayon égal à sa distance au téléphonne.
                                // Un objet cercle est instancié puis ajouté à une liste de cerles
                                try{
                                    listCercles.add(new Cercle(Double.valueOf(position.split(":")[0]), Double.valueOf(position.split(":")[1]), distanceToBeacon(beacon)));
                                }
                                catch(NumberFormatException e){
                                    Log.e("NUMBER_FORMAT_EXCEPTION", "position string : "+position);
                                }

                            }
                            Cercle myPosition = Trilateration.trilaterer2(listCercles);

                            logMesure.add(myPosition);
                            if (logMesure.isAtFullCapacity()) {
                                estimatedPosition = computeAveragePosition();

                                //Affichage de la position estimée toute les 5 itérations dans l'interface utilisateur (pour la démonstration)
                                if(k > 4){
                                    TextView textView = (TextView) findViewById(R.id.position_label);
                                    textView.setText(String.format("x=%1$.3f, y=%2$.3f", estimatedPosition.x, estimatedPosition.y));
                                    k = 0;
                                }

                                for(PositionnalContent posCont : listPositionnalContent){
                                    if(posCont.contains((float)estimatedPosition.x, (float)estimatedPosition.y))
                                    {
                                        if(currentPositionnalContent != posCont){
                                            ImageView img = (ImageView) findViewById(R.id.imageViewContent);
                                            img.setImageResource(posCont.getIdImage());
                                            TextView txt = (TextView) findViewById(R.id.textViewContent);
                                            txt.setText(posCont.getIdMessage());
                                            currentPositionnalContent = posCont;
                                        }
                                    }
                                }
                            }
                        }

                    }

                }
            }
        });

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
    public void onBackPressed(){
        if(getCurrentViewId() != R.layout.activity_main){
            changeToMainView();
        }
        else
        {
            super.onBackPressed();
        }
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
                changeToCallibrerView();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void changeToMainView()
    {
        setContentViewById(R.layout.activity_main);
        final Button b = (Button) findViewById(R.id.button_start);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tx_Power != CALIBRAGE_UNSET )
                {
                    is_started = true;
                    b.setVisibility(View.GONE);
                }
                else
                {
                    changeToCallibrerView();
                }
            }
        });

        if(is_started)
        {
            b.setVisibility(View.GONE);
            if(currentPositionnalContent != null)
            {
                ImageView img = (ImageView) findViewById(R.id.imageViewContent);
                img.setImageResource(currentPositionnalContent.getIdImage());
                TextView txt = (TextView) findViewById(R.id.textViewContent);
                txt.setText(currentPositionnalContent.getIdMessage());
            }
        }
        else
        {
            b.setVisibility(View.VISIBLE);
            TextView txt = (TextView) findViewById(R.id.textViewAccelero);
            txt.setText("");
        }


    }

    public void changeToCallibrerView()
    {
        is_started = false;
        currentPositionnalContent = null;
        setContentViewById(R.layout.callibrer);
        Button b = (Button) findViewById(R.id.Callibrer);
        b.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                isCallibrating = true;
            }
        });
    }


    /**
     *
      * @param beacon
     */
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
            for (int i = 0; i < 2; i++) {
                iterator.next();
            }

            int j = 0;
            for (int i = 0; i < 17; i++) {
                total += iterator.next().doubleValue();
                j++;
            }
            float moyenne = total / calibrage.size()-4;

            tx_Power = moyenne;

            isCallibrating=false;
            calibrage.clear();

            //make changes in cache
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat("beaconDefault1M_Power", tx_Power);
            // Commit the edits!
            editor.commit();

            // 1. Instantiate an AlertDialog.Builder with its constructor
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage(R.string.end_callibrage_text)
                    .setTitle(R.string.end_callibrage_title);

            // 3. Add the OK button
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    changeToMainView();
                }
            });

            // 4. Get the AlertDialog from create()
            AlertDialog dialog = builder.create();

            // 5. Show the dialog
            dialog.show();
        }
    }


    /**
     * RSSI est la force du signal reçu par le téléphone
     * TxPower est la force du signal reçu par le téléphone à 1 mètre du beacon!
     *
     * @param beacon
     * @return distance du téléphone au beacon
     */
    double distanceToBeacon(Beacon beacon) {
        double rssi= (double) beacon.getRssi();
        return 0.42093*Math.pow(rssi/tx_Power, 6.9476)+0.54992;
    }


    /**
     *
     * @return la moyenne des positions calculées
     */
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

                if(acc_tot < ACCELERATION_TOTAL_THRESHOLD_INF )
                {
                    idleIteration++;
                    if(idleIteration >= 4)
                    {
                        //Reprise du ranging (scan) si nécessaire
                        SystemRequirementsChecker.checkWithDefaultDialogs(this);
                        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
                            @Override
                            public void onServiceReady() {
                                beaconManager.startRanging(region);
                            }
                        });

                        ImageView imageView = (ImageView) findViewById(R.id.imageView);
                        if(imageView != null && is_started)
                        {
                            imageView.setBackgroundResource(R.drawable.ok);
                            TextView textView = (TextView) findViewById(R.id.textViewAccelero);
                            textView.setText("calcul de la position en cours ...");
                        }
                    }

                }
                else if (acc_tot > ACCELERATION_TOTAL_THRESHOLD_SUP)
                {
                    idleIteration = 0;

                    //Arrêt du ranging (scan)
                    beaconManager.stopRanging(region);

                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    if(imageView != null && is_started)
                    {
                        imageView.setBackgroundResource(R.drawable.wrong);
                        TextView textView = (TextView) findViewById(R.id.textViewAccelero);
                        textView.setText("Vous bougez trop, calcul de la position stoppé!");
                    }
                }

                acc_tot = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setContentViewById(int id)
    {
        setContentView(id);
        currentViewId = id;
    }

    public int getCurrentViewId()
    {
        return currentViewId;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(getCurrentViewId() == R.layout.activity_main)
        {
            changeToMainView();
        }
        else if(getCurrentViewId() == R.layout.callibrer)
        {
            changeToCallibrerView();
        }
    }
}
