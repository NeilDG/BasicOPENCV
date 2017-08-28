package com.neildg.basicopencv.processing;

import org.opencv.core.Mat;

/**
 * Performs the fusing of multiple aligned images
 * Created by NeilDG on 8/28/2017.
 */

public class ImageFusionOperator {
    private final static String TAG = "ImageOperator";

    private Mat[] inputImages;

    public ImageFusionOperator(Mat[] inputImages) {
        this.inputImages = inputImages;
    }

    public void perform() {

    }
}
