package com.jakubminarik.dashcam.play.dialog;

import java.io.Serializable;

public interface VideoActionListener extends Serializable {

    void onDeleteClicked(int position);

    void onInfoClicked(int position);

    void onPlayClicked(int position);

    void onShareClicked(int position);
}
