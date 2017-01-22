package ch.hearc.trackmeinside;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.junit.Before;
import org.junit.Test;


import java.util.Queue;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Mateli on 21.01.2017.
 */

public class TestCircularFIFOqueue {

    private Queue<Integer> fifoInteger;
    private Queue<Cercle> fifoCercle;

    @Before
    public void initCircularFIFOqueue(){

    }

    @Test
    public void test_fifoInteger() {
        fifoInteger = new CircularFifoQueue<Integer>(2);
        fifoInteger.add(0);
        fifoInteger.add(1);
        fifoInteger.add(2);
        fifoInteger.add(3);

        assertThat(fifoInteger.size() == 2, is(true));

        assertThat(fifoInteger.contains(2), is(true));
        assertThat(fifoInteger.contains(3), is(true));

        assertThat(fifoInteger.contains(1), is(false));
        assertThat(fifoInteger.contains(0), is(false));

        System.out.println(fifoInteger);
    }

    @Test
    public void test_fifoCercle() {
        fifoCercle = new CircularFifoQueue<Cercle>(5);
        assertThat(fifoCercle.size() == 0, is(true));

        fifoCercle.add(new Cercle(1,1,1));
        assertThat(fifoCercle.size() == 1, is(true));

        fifoCercle.add(new Cercle(2,2,2));
        assertThat(fifoCercle.size() == 2, is(true));

        fifoCercle.add(new Cercle(3,3,3));
        assertThat(fifoCercle.size() == 3, is(true));

        fifoCercle.add(new Cercle(4,4,4));
        assertThat(fifoCercle.size() == 4, is(true));

        fifoCercle.add(new Cercle(5,5,5));
        assertThat(fifoCercle.size() == 5, is(true));

        fifoCercle.add(new Cercle(6,6,6));
        assertThat(fifoCercle.size() == 5, is(true));

        fifoCercle.remove();
        assertThat(fifoCercle.size() == 4, is(true));
        System.out.println(fifoCercle);

    }



}
