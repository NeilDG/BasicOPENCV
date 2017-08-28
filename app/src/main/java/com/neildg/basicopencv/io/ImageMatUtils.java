package com.neildg.basicopencv.io;

import org.opencv.core.Mat;

/**
 * Utility functions for easier loading of images to matrices
 * Created by NeilDG on 8/28/2017.
 */

public class ImageMatUtils {
    private static String TAG = "ImageMatUtils";

    public static Mat[] convertImageMapToMat() {
        Mat[] inputImages = new Mat[ImageInputMap.numImages()];

        for(int i = 0 ; i < inputImages.length; i++) {
            inputImages[i] = FileImageReader.getInstance().imReadOpenCV(ImageInputMap.getInputImage(i), ImageFileAttribute.FileType.JPEG);
        }

        return inputImages;
    }
}
