package com.neildg.basicopencv;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.neildg.basicopencv.io.FileImageReader;
import com.neildg.basicopencv.io.ImageFileAttribute;
import com.neildg.basicopencv.io.ImageInputMap;

public class ImageViewActivity extends AppCompatActivity {
    private final static String TAG = "ImageViewActivity";

    private enum ImageViewType {
        ORIGINAL,
        DENOISED
    }

    private SubsamplingScaleImageView originalView;
    private SubsamplingScaleImageView denoiseView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

    }

    @Override
    protected void onResume() {
        super.onResume();
        this.loadImageView();
        this.setupRadioButtons();
    }

    private void loadImageView() {
        String imageSource = FileImageReader.getInstance().getDecodedFilePath("denoise_image", ImageFileAttribute.FileType.JPEG);
        this.denoiseView = (SubsamplingScaleImageView) this.findViewById(R.id.denoise_image_view);
        this.denoiseView.setImage(ImageSource.uri(imageSource));

        this.originalView = (SubsamplingScaleImageView) this.findViewById(R.id.original_image_view);
        imageSource = ImageInputMap.getInputImage(0);
        this.originalView.setImage(ImageSource.uri(imageSource));
    }

    private void setupRadioButtons() {
        RadioGroup radioGroup = (RadioGroup) this.findViewById(R.id.image_view_radio_group);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.original_radio_btn) {
                    ImageViewActivity.this.setImageViewType(ImageViewType.ORIGINAL);
                }
                else if(checkedId == R.id.denoise_radio_btn) {
                    ImageViewActivity.this.setImageViewType(ImageViewType.DENOISED);
                }
            }
        });

        RadioButton originalBtn = (RadioButton) radioGroup.findViewById(R.id.interpolate_radio_btn);
        RadioButton denoiseBtn = (RadioButton) radioGroup.findViewById(R.id.denoise_radio_btn);

        if(FileImageReader.getInstance().doesImageExists("denoise_image", ImageFileAttribute.FileType.JPEG)) {
            denoiseBtn.setEnabled(true);
        }
        else {
            denoiseBtn.setEnabled(false);
        }
    }

    private void setImageViewType(ImageViewType imageViewType) {
        if(imageViewType == ImageViewType.DENOISED) {
            this.denoiseView.setVisibility(View.VISIBLE);
            this.originalView.setVisibility(View.INVISIBLE);
        }
        else if(imageViewType == ImageViewType.ORIGINAL) {
            this.denoiseView.setVisibility(View.INVISIBLE);
            this.originalView.setVisibility(View.VISIBLE);
        }
    }
}
