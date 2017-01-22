package com.example.airport;


import com.example.airport.tools.MathTools;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Mateli on 18.01.2017.
 */



public class TestTrilateration {

    private final double EPSILON = 0.00000001;
    private final double EPSILONb = 0.7;


    @Test
    public void testPerfectIntersection_1_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(0,1,1));
        listCercles.add(new Cercle(1,0,1));
        listCercles.add(new Cercle(-1,0,1));

        Cercle resultCercle = Trilateration.trilaterer(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILON), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILON), is(true));
    }

    @Test
    public void testPerfectIntersection_2_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(3,4,5));
        listCercles.add(new Cercle(1,0,1));
        listCercles.add(new Cercle(-1,0,1));

        Cercle resultCercle = Trilateration.trilaterer(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILON), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILON), is(true));
    }

    @Test
    public void testPerfectIntersection_3_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(0,1.1,1.1));
        listCercles.add(new Cercle(1,0,1));
        listCercles.add(new Cercle(-1,0,1));

        Cercle resultCercle = Trilateration.trilaterer(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILON), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILON), is(true));
    }

    @Test
    public void testPerfectIntersection_4_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(1,2,1));
        listCercles.add(new Cercle(2,1,1));
        listCercles.add(new Cercle(0,1,1));

        Cercle resultCercle = Trilateration.trilaterer(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 1, EPSILON), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 1, EPSILON), is(true));
    }


    @Test
    public void testAreaIntersection1_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(0,1,1.5));
        listCercles.add(new Cercle(1,0,1.5));
        listCercles.add(new Cercle(-1,0,1.5));

        Cercle resultCercle = Trilateration.trilaterer(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILONb), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILONb), is(true));
    }

    @Test
    public void testAreaIntersection2_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(3,4,5));
        listCercles.add(new Cercle(-4,0,3.5));
        listCercles.add(new Cercle(1,-2,2.5));

        Cercle resultCercle = Trilateration.trilaterer(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILONb), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILONb), is(true));
    }

    //nouvelle formule (plus réaliste et consistante)

    @Test
    public void testPerfectIntersection_1b_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(0,4,4));
        listCercles.add(new Cercle(1,0,1));
        listCercles.add(new Cercle(-1,0,1));

        Cercle resultCercle = Trilateration.trilaterer2(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILONb), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILONb), is(true));
    }

    @Test
    public void testPerfectIntersection_2b_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(3,4,5));
        listCercles.add(new Cercle(1,0,1));
        listCercles.add(new Cercle(-1,0,1));

        Cercle resultCercle = Trilateration.trilaterer2(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILONb), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILONb), is(true));
    }

    @Test
    public void testPerfectIntersection_3b_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(0,1.1,1.1));
        listCercles.add(new Cercle(1,0,1));
        listCercles.add(new Cercle(-1,0,1));

        Cercle resultCercle = Trilateration.trilaterer2(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILONb), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILONb), is(true));
    }

    @Test
    public void testPerfectIntersection_4b_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(1,2,1));
        listCercles.add(new Cercle(2,1,1));
        listCercles.add(new Cercle(0,1,1));

        Cercle resultCercle = Trilateration.trilaterer2(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 1, EPSILONb), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 1, EPSILONb), is(true));
    }

    @Test
    public void testPerfectIntersection_5b_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(2.5,2.5, 2.6376327987774468688185497624126));
        listCercles.add(new Cercle(2.5,-4, 4.0874327861368616668406438287857));
        listCercles.add(new Cercle(-4,0,6));

        Cercle resultCercle = Trilateration.trilaterer2(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 2, EPSILONb), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILONb), is(true));
    }

    @Test
    public void testAreaIntersection1b_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(0,1,1.5));
        listCercles.add(new Cercle(1,0,1.5));
        listCercles.add(new Cercle(-1,0,1.5));

        Cercle resultCercle = Trilateration.trilaterer2(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILONb), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILONb), is(true));
    }

    @Test
    public void testAreaIntersection2b_ReturnsTrue() {
        List<Cercle> listCercles = new ArrayList<Cercle>(3);
        listCercles.add(new Cercle(3,4,5));
        listCercles.add(new Cercle(-4,0,3.5));
        listCercles.add(new Cercle(1,-2,2.5));

        Cercle resultCercle = Trilateration.trilaterer2(listCercles);
        System.out.println(resultCercle.x);
        System.out.println(resultCercle.y);
        assertThat(MathTools.isEqual(resultCercle.x, 0, EPSILONb), is(true));
        assertThat(MathTools.isEqual(resultCercle.y, 0, EPSILONb), is(true));
    }
}