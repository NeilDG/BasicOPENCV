package com.neildg.basicopencv;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.darsh.multipleimageselect.activities.AlbumSelectActivity;
import com.darsh.multipleimageselect.helpers.Constants;
import com.darsh.multipleimageselect.models.Image;
import com.neildg.basicopencv.io.BitmapURIRepository;
import com.neildg.basicopencv.io.DirectoryStorage;
import com.neildg.basicopencv.io.FileImageReader;
import com.neildg.basicopencv.io.FileImageWriter;
import com.neildg.basicopencv.platformtools.core_application.ApplicationCore;
import com.neildg.basicopencv.platformtools.notifications.NotificationCenter;
import com.neildg.basicopencv.platformtools.notifications.NotificationListener;
import com.neildg.basicopencv.platformtools.notifications.Notifications;
import com.neildg.basicopencv.platformtools.notifications.Parameters;
import com.neildg.basicopencv.processing.ProcessingThread;
import com.neildg.basicopencv.ui.progress_dialog.ProgressDialogHandler;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NotificationListener {
    private final static String TAG = "MainActivity";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully!");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.initializeButtons();

        ApplicationCore.initialize(this);
        ProgressDialogHandler.initialize(this);
        ProgressDialogHandler.getInstance().setDefaultProgressImplementor();

        NotificationCenter.getInstance().addObserver(Notifications.ON_PROCESS_COMPLETED, this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }


        DirectoryStorage.getSharedInstance().createDirectory();
        FileImageWriter.initialize(this);
        FileImageReader.initialize(this);
    }

    @Override
    public void onDestroy() {
        ProgressDialogHandler.destroy();
        FileImageWriter.destroy();
        FileImageReader.destroy();

        NotificationCenter.getInstance().removeObserver(Notifications.ON_PROCESS_COMPLETED, this);
        super.onDestroy();
    }

    private void initializeButtons() {
        Button pickImagesBtn = (Button) this.findViewById(R.id.select_image_btn);
        pickImagesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.startImagePickActivity();
            }
        });
    }

    private void startImagePickActivity() {
        Intent intent = new Intent(MainActivity.this, AlbumSelectActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_LIMIT, 10);
        startActivityForResult(intent, Constants.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            //The array list has the image paths of the selected images
            ArrayList<Image> images = data.getParcelableArrayListExtra(Constants.INTENT_EXTRA_IMAGES);
            ArrayList<Uri> imageURIList = new ArrayList<>();
            for(int i = 0; i < images.size(); i++) {
                imageURIList.add(Uri.fromFile(new File(images.get(i).path)));
                BitmapURIRepository.getInstance().setImageURIList(imageURIList);
            }

            if(imageURIList.size() >= 3) {
                Log.v("LOG_TAG", "Selected Images " + imageURIList.size());
                this.startProcessing();

            }
            else {
                Toast.makeText(this, "You haven't picked enough images. Pick multiple similar images. At least 3.",
                        Toast.LENGTH_LONG).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startProcessing() {
        ProgressDialogHandler.getInstance().showProcessDialog("Processing", "Processing images");
        ProcessingThread processingThread = new ProcessingThread();
        processingThread.start();
    }

    @Override
    public void onNotify(String notificationString, Parameters params) {
        if(notificationString == Notifications.ON_PROCESS_COMPLETED) {
            ProgressDialogHandler.getInstance().hideProcessDialog();
            Intent imageViewIntent = new Intent(MainActivity.this, ImageViewActivity.class);
            this.startActivity(imageViewIntent);
        }
    }
}
