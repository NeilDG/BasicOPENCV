package com.neildg.basicopencv.processing;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

/**
 * Handles image alignment, using first reference image
 * Created by NeilDG on 8/28/2017.
 */

public class ImageAlignmentOperator {
    private final static String TAG = "ImageAlignmentOperator";

    private Mat[] inputImages;
    private Mat[] outputImages;

    private FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.ORB);
    private DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    private DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

    private MatOfKeyPoint refKeypoint;
    private Mat referenceDescriptor;

    private MatOfKeyPoint[] lrKeypointsList;
    private Mat[] lrDescriptorList;
    private MatOfDMatch[] dMatchesList;

    public ImageAlignmentOperator(Mat[] inputImages) {
        this.inputImages = inputImages;

        this.lrKeypointsList = new MatOfKeyPoint[this.inputImages.length - 1];
        this.lrDescriptorList = new Mat[this.inputImages.length - 1];
        this.dMatchesList = new MatOfDMatch[this.inputImages.length - 1];
    }

    public void perform() {
        this.detectFeaturesInReference(0);
        for(int i = 1; i < this.inputImages.length; i++) {
            this.detectFeatures(this.inputImages[i], i - 1);
        }
    }

    /*
     * Detects all keypoints first in a given reference image
     */
    private void detectFeaturesInReference(int index) {
        this.referenceDescriptor = new Mat();
        this.refKeypoint = new MatOfKeyPoint();

        featureDetector.detect(this.inputImages[index], this.refKeypoint);
        descriptorExtractor.compute(this.inputImages[index], this.refKeypoint, this.referenceDescriptor);
    }

    /*
     * Detects all keypoints in all images, except the reference image
     */
    private void detectFeatures(Mat imgMat, int index) {
        Mat lrDescriptor = new Mat();
        MatOfKeyPoint keyPoint = new MatOfKeyPoint();

        featureDetector.detect(imgMat,keyPoint);
        this.descriptorExtractor.compute(imgMat, keyPoint, lrDescriptor);

        this.lrKeypointsList[index] = keyPoint;
        this.lrDescriptorList[index] = lrDescriptor;
    }
}
