package com.neildg.basicopencv.processing;

import android.util.Log;

import com.neildg.basicopencv.io.ImageInputMap;
import com.neildg.basicopencv.io.ImageMatUtils;

import org.opencv.core.Mat;

/**
 * Thread for processing the images
 * Created by NeilDG on 8/28/2017.
 */
public class ProcessingThread extends Thread {
    private final static String TAG = "ProcessingThread";

    public ProcessingThread() {

    }

    @Override
    public void run() {
        Mat[] inputImages = ImageMatUtils.convertImageMapToMat();
        ImageAlignmentOperator alignmentOperator = new ImageAlignmentOperator(inputImages);
        alignmentOperator.perform();

        ImageFusionOperator fusionOperator = new ImageFusionOperator(alignmentOperator.getOutputImages());
        fusionOperator.perform();

    }
}
