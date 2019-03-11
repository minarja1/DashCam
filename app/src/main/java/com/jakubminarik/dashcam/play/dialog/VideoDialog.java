package com.jakubminarik.dashcam.play.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.jakubminarik.dashcam.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class VideoDialog extends DialogFragment {

    private static final String ARG_VIDEO_POSITION = "ARG_VIDEO_POSITION";
    private static final String ARG_VIDEO_LISTENER = "ARG_VIDEO_LISTENER";

    private int videoPosition;
    private VideoActionListener listener;

    public static VideoDialog newInstance(int videoPosition, VideoActionListener listener) {
        Bundle args = new Bundle();
        VideoDialog fragment = new VideoDialog();
        args.putInt(ARG_VIDEO_POSITION, videoPosition);
        args.putSerializable(ARG_VIDEO_LISTENER, listener);
        fragment.setArguments(args);
        return fragment;
    }

    @OnClick(R.id.playButton)
    void playVideo() {
        dismiss();
        listener.onPlayClicked(videoPosition);
    }

    @OnClick(R.id.shareButton)
    void shareVideo() {
        dismiss();
        listener.onShareClicked(videoPosition);
    }

    @OnClick(R.id.infoButton)
    void infoClicked() {
        dismiss();
        listener.onInfoClicked(videoPosition);
    }

    @OnClick(R.id.deleteButton)
    void deleteVideo() {
        dismiss();
        listener.onDeleteClicked(videoPosition);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.video_dialog, null);

        videoPosition = getArguments().getInt(ARG_VIDEO_POSITION);
        listener = ((VideoActionListener) getArguments().getSerializable(ARG_VIDEO_LISTENER));


        AlertDialog dialog = builder.setView(view).create();

        ButterKnife.bind(this, view);
        return dialog;
    }
}
