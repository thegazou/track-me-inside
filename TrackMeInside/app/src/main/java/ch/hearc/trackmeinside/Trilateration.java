package ch.hearc.trackmeinside;

import java.util.List;

/**
 * Created by Mateli on 18.01.2017.
 */

public class Trilateration {

    /**
     *
     * @param listCercles (size == 3)
     * @return
     */
    public static Cercle trilaterer(List<Cercle> listCercles){
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

    /**
     *
     * @param listCercles (min size = 1, pas de maxsize)
     * @return
     */
    public static Cercle trilaterer2(List<Cercle> listCercles){

        double x = 0;
        double y = 0;
        double normalizeCoefficient = 0.0;
        int size = listCercles.size();
        //if(listCercles.size() != 3)
            //return null;
        //take revert values, because lower distance then bigger weight
        for (int i = 0; i < size; i++)
            normalizeCoefficient += 1.0 / (listCercles.get(i).r * listCercles.get(i).r);
        System.out.println("Normalized coef : "+normalizeCoefficient);

        double[] tabWeight = new double[size];
        //vector <double> weight( mBeaconMeas.size(), 0.0 );

        for (int i = 0; i < size; i++)
        {

            // calculate probability of being at beacons x,y coordinates
            tabWeight[ i ] = 1.0 / (listCercles.get(i).r * listCercles.get(i).r *
                    normalizeCoefficient );
            //System.out.println("weight "+ i+" : "+tabWeight[i]);



            double beaconX = listCercles.get(i).x;//mBeaconMeas[ i ].getBeaconPtr()->getX();
            double beaconY = listCercles.get(i).y;//mBeaconMeas[ i ].getBeaconPtr()->getY();
            //System.out.println("beaconX "+ i+" : "+beaconX);
            //System.out.println("beaconY "+ i+" : "+beaconY);

            //find final coordinates according to probability
            x += tabWeight[ i ] * beaconX;
            y += tabWeight[ i ] * beaconY;
        }

        return new Cercle(x,y,0);
    }
}
