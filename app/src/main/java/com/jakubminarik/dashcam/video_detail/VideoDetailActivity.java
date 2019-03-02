package com.jakubminarik.dashcam.video_detail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.base.BaseActivityDI;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.model.Video;

import java.io.File;
import java.text.DateFormat;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;

public class VideoDetailActivity extends BaseActivityDI implements VideoDetailActivityView {
    @Inject
    VideoDetailActivityPresenter presenter;

    @BindView(R.id.mapImageView)
    ImageView mapImageView;
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
        Video video = presenter.getVideo();

        getSupportActionBar().setTitle(video.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (video.getPathToMaoImage() != null && !video.getPathToMaoImage().isEmpty()) {
            File imageFile = new File(video.getPathToMaoImage());
            if (imageFile.exists()) {
                Glide.with(this).load(imageFile).into(mapImageView);
            } else {
                mapImageView.setVisibility(View.GONE);
                mapNotFoundTextView.setVisibility(View.VISIBLE);
            }
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

    @OnClick(R.id.playVideoButton)
    void onPlayVideoButtonClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(presenter.getVideo().getPathToFile()));
        intent.setDataAndType(Uri.parse(presenter.getVideo().getPathToFile()), "video/mp4");
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
