package com.neildg.basicopencv.ui.progress_dialog;

/**
 * Created by NeilDG on 4/27/2017.
 */
public interface IProgressImplementor {
    void setup(String title, String message);

    void updateProgress(int progress);

    int getProgress();
    String getMessage();

    void show();
    void hide();
    boolean isShown();
}
