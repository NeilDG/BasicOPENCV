package com.neildg.basicopencv.processing;

import com.neildg.basicopencv.io.FileImageWriter;
import com.neildg.basicopencv.io.ImageFileAttribute;
import com.neildg.basicopencv.platformtools.notifications.NotificationCenter;
import com.neildg.basicopencv.platformtools.notifications.NotificationListener;
import com.neildg.basicopencv.platformtools.notifications.Notifications;
import com.neildg.basicopencv.processing.imagetools.ImageOperator;
import com.neildg.basicopencv.processing.imagetools.MatMemory;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Performs the fusing of multiple aligned images
 * Created by NeilDG on 8/28/2017.
 */

public class ImageFusionOperator {
    private final static String TAG = "ImageOperator";

    private Mat[] inputImages;

    private Mat outputMat;

    public ImageFusionOperator(Mat[] inputImages) {
        this.inputImages = inputImages;
        this.outputMat = new Mat();
    }

    public void perform() {
        Mat initialMat = this.inputImages[0];
        initialMat.convertTo(initialMat, CvType.CV_16UC(initialMat.channels())); //convert to 16_UC

        for(int i = 1; i < this.inputImages.length; i++) {
            Mat maskMat = ImageOperator.produceMask(this.inputImages[i]);
            this.inputImages[i] = this.sharpen(this.inputImages[i]);
            Core.add(initialMat, this.inputImages[i], initialMat, maskMat, CvType.CV_16UC(initialMat.channels()));
        }

        Core.divide(initialMat, Scalar.all(this.inputImages.length), this.outputMat);
        FileImageWriter.getInstance().saveMatrixToImage(this.outputMat, "denoise_image", ImageFileAttribute.FileType.JPEG);
        NotificationCenter.getInstance().postNotification(Notifications.ON_PROCESS_COMPLETED);

        this.outputMat.release();
        MatMemory.releaseAll(this.inputImages, true);
    }

    private Mat sharpen(Mat inputMat) {
        Mat outputMat = new Mat();
        Mat blurMat = new Mat();
        this.outputMat = new Mat();
        Imgproc.blur(inputMat, blurMat, new Size(25,25));

        Core.addWeighted(inputMat, 1.75, blurMat, -0.75, 0, outputMat, CvType.CV_8UC(inputMat.channels()));

        return outputMat;
    }
}
