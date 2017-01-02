package com.example.airport;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.io.Console;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MyApplication extends Application {

    private double RSSI_Default = 1.02; // provient
    private BeaconManager beaconManager;
    private List<Cercle> listCercles;
    private Cercle estimatedPosition;
    //give the position of a beacon by its Major
    private static final Map<Integer, String> POSITION_BY_BEACON;
    private static List<Cercle> logMesure = new ArrayList<>();

    //Nombre de mesure nécessaire pour faire une estimation de la position de l'utilisateur.
    private static int MESURE_COMPTE_FOR_ESTIMATION=20;

    //TODO Constante lié à la force de transmition des beacons déterminé empiriquement.
    private static final double BEACON_EMISION_CONSTANT = 12;

    static {
        Map<Integer, String> positionByBeacon = new HashMap<>();
        //TODO placer et prendre la position des beacons
        positionByBeacon.put(0, "5:5"); //Le beacon avec le major 0 est à la position 5-5;
        positionByBeacon.put(1, "5:5"); //Le beacon avec le major 1 est à la position 5-5;
        positionByBeacon.put(2, "5:5"); //Le beacon avec le major 2 est à la position 5-5;
        positionByBeacon.put(3, "5:5"); //Le beacon avec le major 3 est à la position 5-5;
        positionByBeacon.put(4, "5:5"); //Le beacon avec le major 4 est à la position 5-5;

        POSITION_BY_BEACON = Collections.unmodifiableMap(positionByBeacon);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        beaconManager = new BeaconManager(getApplicationContext());
        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> list) {
                /*Quand on rentre dans la classe le calcule de la position va se faire par trilatération à partir des trois beacons les plus proches.
                  */
                // Vérifie qu'on est bien à portée de 3 beacons et que le plus éloingné est à une distance "résonnable"
                if (list.size() <= 3 && powerToDistance(list.get(2).getMeasuredPower()) > 100) {
                    Log.d("weakest beacon's signal", list.get(2).getProximityUUID().toString() + " has a signal strengh of " + list.get(2).getMeasuredPower());
                    String position = "0:0";
                    for (Beacon beacon:list) {
                        if (POSITION_BY_BEACON.containsKey(beacon.getMajor())) {
                            position = POSITION_BY_BEACON.get(beacon.getMajor());
                        }else{
                            Log.e("wrong Major", "The major of a beacon match nothing");
                        }
                        // Le beacon et sa distance représente un cerle centré sur sa position et de rayon égal à sa distance au téléphonne.
                        // Un objet cercle est instancié puis ajouté à une liste de cerles
                        listCercles.add(new Cercle(Double.valueOf(position.split(":")[0]),Double.valueOf(position.split(":")[1]), powerToDistance(beacon.getMeasuredPower())));
                    }
                    Cercle myPosition = trilateration(listCercles);
                    Log.d("Position brute", String.format("x=%1$.3f, y=%2$.3f",myPosition.x,myPosition.y));
                    logMesure.add(myPosition);
                    if(logMesure.size()==MESURE_COMPTE_FOR_ESTIMATION)
                    {
                        estimatedPosition = computeAveragePosition();
                        Log.d("Position estimée", String.format("x=%1$.3f, y=%2$.3f",estimatedPosition.x,estimatedPosition.y));
                    }
                }else
                    showNotification("You're not in range of 3 beacons", "");
            }
            @Override
            public void onExitedRegion(Region region) {
                // notifier qu'on est plus dans la salle.
            }
        });
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startMonitoring(new Region("monitored region",
                        null, 1, null));
            }
        });
    }
    // trouve le point le plus proche des trois cerles.
    private Cercle trilateration(List<Cercle> listCercles)
    {
        double top = 0;
        double bot = 0;
        for (int i=0; i<3; i++) {
            Cercle c = listCercles.get(i);
            Cercle c2, c3;
            if (i==0) {
                c2 = listCercles.get(1);
                c3 = listCercles.get(2);
            }
            else if (i==1) {
                c2 = listCercles.get(0);
                c3 = listCercles.get(2);
            }
            else {
                c2 = listCercles.get(0);
                c3 = listCercles.get(1);
            }

            double d = c2.x - c3.x;

            double v1 = (c.x * c.x + c.y * c.y) - (c.r * c.r);
            top += d*v1;

            double v2 = c.y * d;
            bot += v2;

        }

        double y = top / (2*bot);
        Cercle c1 = listCercles.get(0);
        Cercle c2 = listCercles.get(1);
        top = c2.r*c2.r+c1.x*c1.x+c1.y*c1.y-c1.r*c1.r-c2.x*c2.x-c2.y*c2.y-2*(c1.y-c2.y)*y;
        bot = c1.x-c2.x;
        double x = top / (2*bot);

        return new Cercle(x,y,0);

    }

    //TODO estimer la fonction qui permet de convertir la force du signal d'un beacon à la distance du beacon au téléphonne.
    public double powerToDistance(int powerRecieved)
    {
        return Math.pow((RSSI_Default-powerRecieved)/20,10);
        //return Math.pow((BEACON_EMISION_CONSTANT*RSSI_Default)/powerRecieved,1/2);
    }

    //Calcule la moyenne des positions calculées
    public Cercle computeAveragePosition()
    {
        double sumX=0;
        double sumY=0;
        for (Cercle position:logMesure) {
            sumX+=position.x;
            sumY+=position.y;
        }
        return new Cercle(sumX/MESURE_COMPTE_FOR_ESTIMATION,sumY/MESURE_COMPTE_FOR_ESTIMATION,5);
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
}
