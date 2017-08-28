package com.neildg.basicopencv.processing;

import android.util.Log;

import com.neildg.basicopencv.io.FileImageWriter;
import com.neildg.basicopencv.io.ImageFileAttribute;
import com.neildg.basicopencv.processing.imagetools.MatMemory;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

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
            this.matchFeaturesToReference(this.lrDescriptorList[i - 1], i - 1, 999.9f);

            //debug show mat
            Mat matchesShower = new Mat();
            Features2d.drawMatches(this.inputImages[0], this.refKeypoint, this.inputImages[i], this.lrKeypointsList[i - 1], this.dMatchesList[i - 1], matchesShower);
            FileImageWriter.getInstance().saveMatrixToImage(matchesShower, "matches_"+i, ImageFileAttribute.FileType.JPEG);
            matchesShower.release();

            Mat homography = this.identifyHomography(this.lrKeypointsList[i - 1], this.dMatchesList[i - 1]);
            Mat warpedMat = this.performPerspectiveWarping(this.inputImages[i], homography);

            FileImageWriter.getInstance().saveMatrixToImage(warpedMat, "warped_" +i, ImageFileAttribute.FileType.JPEG);
        }

        MatMemory.releaseAll(this.lrKeypointsList, false);
        MatMemory.releaseAll(this.lrDescriptorList, false);
        MatMemory.releaseAll(this.dMatchesList, false);
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


    private void matchFeaturesToReference(Mat comparingDescriptor, int index, float distanceThreshold) {
        MatOfDMatch initialMatch = new MatOfDMatch();
        Log.d(TAG, "Reference descriptor type: "+ CvType.typeToString(this.referenceDescriptor.type()) + " Comparing descriptor type: "+ CvType.typeToString(comparingDescriptor.type()));
        Log.d(TAG, "Reference size: " +this.referenceDescriptor.size().toString()+ " Comparing descriptor size: " +comparingDescriptor.size().toString());
        this.matcher.match(this.referenceDescriptor, comparingDescriptor, initialMatch);


        //given matches from initialmatch, filter only good matches based on given threshold
        DMatch[] dMatchList = initialMatch.toArray();
        List<DMatch> goodMatchesList = new ArrayList<DMatch>();
        for(int i = 0; i < dMatchList.length; i++) {
            //Log.d(TAG, "dMatch distance: " +dMatchList[i].distance);
            if(dMatchList[i].distance < distanceThreshold) {
                goodMatchesList.add(dMatchList[i]);
            }
        }

        initialMatch.release();

        //filter matches to only show good ones
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromArray(goodMatchesList.toArray(new DMatch[goodMatchesList.size()]));

        this.dMatchesList[index] = goodMatches;
    }

    /*
     * Identifies the projective homography of the images, given candidate keypoints extracted from the reference image. This determines the
     * alignment/orientation of the image so that it will be accurately aligned with the reference image.
     */
    private Mat identifyHomography(MatOfKeyPoint candidateKeypoint, MatOfDMatch dMatch) {
        MatOfPoint2f matOfPoint1 = new MatOfPoint2f();
        MatOfPoint2f matOfPoint2 = new MatOfPoint2f();

        KeyPoint[] keyPoints1 = this.refKeypoint.toArray();
        KeyPoint[] keyPoints2 = candidateKeypoint.toArray();

        List<Point> pointList1 = new ArrayList<>();
        List<Point> pointList2 = new ArrayList<>();

        DMatch[] dMatchArray = dMatch.toArray();

        for(int i = 0; i < dMatchArray.length; i++) {
            Log.d(TAG, "DMATCHES" + dMatchArray[i].toString());

            pointList1.add(keyPoints1[dMatchArray[i].queryIdx].pt);
            pointList2.add(keyPoints2[dMatchArray[i].trainIdx].pt);
        }

        matOfPoint1.fromList(pointList1); matOfPoint2.fromList(pointList2);

        //((M0.type() == CV_32F || M0.type() == CV_64F) && M0.rows == 3 && M0.cols == 3)

        //Log.d(TAG, "Homography pre info: matOfPoint1 ROWS: " + matOfPoint1.rows() + " matOfPoint1 COLS: " + matOfPoint1.cols());
        //Log.d(TAG, "Homography pre info: matOfPoint2 ROWS: " + matOfPoint2.rows() + " matOfPoint2 COLS: " + matOfPoint2.cols());

        Mat homography;
        if(matOfPoint1.rows() > 0 && matOfPoint1.cols() > 0 && matOfPoint2.rows() > 0 && matOfPoint2.cols() >0) {
            homography = Calib3d.findHomography(matOfPoint2, matOfPoint1, Calib3d.RANSAC, 1);
        }
        else {
            homography = new Mat(); //just empty
        }

        Log.d(TAG, "Homography info: ROWS: " + homography.rows() + " COLS: " + homography.cols());

        matOfPoint1.release();
        matOfPoint2.release();
        pointList1.clear();
        pointList2.clear();

        return homography;
    }

    private Mat performPerspectiveWarping(Mat inputMat, Mat homography) {
        if(homography.rows() == 3 && homography.cols() == 3) {
            Mat warpedMat = new Mat();
            Imgproc.warpPerspective(inputMat, warpedMat, homography, warpedMat.size(), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE, Scalar.all(0));

            homography.release();
            return warpedMat;
        }
        else {
            //do nothing. not enough features for warping
            Mat warpedMat = new Mat();
            inputMat.copyTo(warpedMat);

            homography.release();

            Log.e(TAG, "No homography was found for warp perspective. Returning original mat.");
            return warpedMat;
        }
    }
}
