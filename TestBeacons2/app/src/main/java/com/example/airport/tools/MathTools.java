package com.example.airport.tools;

/**
 * Created by Mateli on 18.01.2017.
 */

public class MathTools {

    public static boolean isEqual(double a, double b, double epsilon)
    {
        return (Math.abs(a-b) < epsilon);
    }

}
