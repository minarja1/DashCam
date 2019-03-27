package com.jakubminarik.dashcam.video_detail;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.jakubminarik.dashcam.BuildConfig;
import com.jakubminarik.dashcam.DAO.VideoDAO;
import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.base.BaseActivityDI;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.helper.DialogHelper;
import com.jakubminarik.dashcam.model.Video;
import com.jakubminarik.dashcam.video_detail.dialog.EditValueDialog;

import java.io.File;
import java.text.DateFormat;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.AndroidInjection;

public class VideoDetailActivity extends BaseActivityDI implements VideoDetailActivityView {
    @Inject
    VideoDetailActivityPresenter presenter;

    @BindView(R.id.videoImageView)
    ImageView videoImageView;
    @BindView(R.id.mapNotFoundTextView)
    TextView mapNotFoundTextView;
    @BindView(R.id.dateTextView)
    TextView dateTextView;
    @BindView(R.id.durationTextView)
    TextView durationTextView;
    @BindView(R.id.fromTextView)
    TextView fromTextView;
    @BindView(R.id.toTextView)
    TextView toTextView;

    private Video video;

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);
        ButterKnife.bind(this);
        video = presenter.getVideo();

        getSupportActionBar().setTitle(video.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (video.getPathToMaoImage() != null && !video.getPathToMaoImage().isEmpty()) {
            File imageFile = new File(video.getPathToMaoImage());
            if (imageFile.exists()) {
                Glide.with(this).load(imageFile).into(videoImageView);
            } else {
                videoImageView.setVisibility(View.GONE);
                mapNotFoundTextView.setVisibility(View.VISIBLE);
            }
        }

        boolean imageSuccesfullyLoaded = false;
        //try loading mapImage

        if (video.getPathToMaoImage() != null && !video.getPathToMaoImage().isEmpty()) {
            File imageFile = new File(video.getPathToMaoImage());
            if (imageFile.exists()) {
                Glide.with(getContext()).load(imageFile).into(videoImageView);
                videoImageView.setVisibility(View.VISIBLE);
                imageSuccesfullyLoaded = true;
            }
        }

        //try loading screenshot
        if (!imageSuccesfullyLoaded) {
            if (video.getPathToScreenshot() != null && !video.getPathToScreenshot().isEmpty()) {
                File imageFile = new File(video.getPathToScreenshot());
                if (imageFile.exists()) {
                    Glide.with(getContext()).load(imageFile).into(videoImageView);
                    videoImageView.setVisibility(View.VISIBLE);
                    imageSuccesfullyLoaded = true;
                }
            }
        }

        //give up
        if (!imageSuccesfullyLoaded) {
            videoImageView.setVisibility(View.GONE);
            mapNotFoundTextView.setVisibility(View.VISIBLE);
        }

        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
        dateTextView.setText(String.format("%s: %s", getResources().getString(R.string.date), dateFormat.format(video.getTimestamp())));


        durationTextView.setText(String.format("%s: %s", getResources().getString(R.string.trip_duration), video.getDurationString(getContext())));

        if (video.getTripStartAddress() != null && !video.getTripStartAddress().isEmpty()) {
            fromTextView.setText(String.format("%s: %s", getResources().getString(R.string.from), video.getTripStartAddress()));
        }

        if (video.getTripEndAddress() != null && !video.getTripEndAddress().isEmpty()) {
            toTextView.setText(String.format("%s: %s", getResources().getString(R.string.to), video.getTripEndAddress()));
        }
    }


    private void showRenameDialog() {
        EditValueDialog editNameDialog = EditValueDialog.newInstance(video.getName(),
                getResources().getString(R.string.edit_name), getResources().getString(R.string.edit_name));

        editNameDialog.setListener(new EditValueDialog.OnTextSumbittedListener() {
            @Override
            public void onTextSubmitted(String text) {
                video.setName(text);
                video.save();
                getSupportActionBar().setTitle(video.getName());
            }
        });

        if (getFragmentManager() != null) {
            editNameDialog.show(getSupportFragmentManager(), "AddTextDialog");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_video_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_edit) {
            showRenameDialog();
        } else if (item.getItemId() == R.id.menu_share) {
            shareVideo();
        } else if (item.getItemId() == R.id.menu_delete) {
            deleteVideo();
        } else if (item.getItemId() == R.id.menu_play) {
            playVideo();
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareVideo() {
        Intent shareVideoIntent = new Intent(Intent.ACTION_SEND);
        File videoFile = new File(video.getPathToFile());

        if (videoFile.exists()) {
            shareVideoIntent.setType("video/mp4");

            Uri videoUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", videoFile);
            shareVideoIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
            shareVideoIntent.putExtra(Intent.EXTRA_SUBJECT, "Video from SmartDashCam");

            startActivity(Intent.createChooser(shareVideoIntent, "Share video"));
        }
    }

    private void deleteVideo() {
        AlertDialog dialog = DialogHelper.getConfirmDialog(getContext(), R.string.delete_dialog_title, R.string.delete_dialog_message, R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                VideoDAO.deleteWithFiles(video);
                finish();
            }
        });
        dialog.show();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.red));
        }

    }

    private void playVideo() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getPathToFile()));
        intent.setDataAndType(Uri.parse(video.getPathToFile()), "video/mp4");
        startActivity(intent);
    }

}
