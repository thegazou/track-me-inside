package ch.hearc.trackmeinside;

import android.graphics.RectF;

/**
 * Created by Mateli on 23.01.2017.
 */

public class PositionnalContent extends RectF {

    private int idImage, idMessage;

    /**
     * Create a new rectangle with the specified coordinates. Note: no range
     * checking is performed, so the caller must ensure that left <= right and
     * top <= bottom.
     *
     * + some data IDs
     *
     *
     * @param left      The X coordinate of the left side of the rectangle
     * @param top       The Y coordinate of the top of the rectangle
     * @param right     The X coordinate of the right side of the rectangle
     * @param bottom    The Y coordinate of the bottom of the rectangle
     * @param idImage   The id of the content image related to this area
     * @param idMessage The id of the content message related to this area
     */
    public PositionnalContent(float left, float top, float right, float bottom, int idImage, int idMessage){
        super(left, top, right, bottom);
        this.idImage = idImage;
        this.idMessage = idMessage;
    }

    public int getIdImage() {
        return this.idImage;
    }

    public int getIdMessage() {
        return this.idMessage;
    }
}
