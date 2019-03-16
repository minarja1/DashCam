package com.jakubminarik.dashcam.play;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.jakubminarik.dashcam.BuildConfig;
import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.base.BaseActivityDI;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.base.Constants;
import com.jakubminarik.dashcam.helper.DialogHelper;
import com.jakubminarik.dashcam.helper.ViewHelper;
import com.jakubminarik.dashcam.model.Video;
import com.jakubminarik.dashcam.play.dialog.VideoActionListener;
import com.jakubminarik.dashcam.play.dialog.VideoDialog;
import com.jakubminarik.dashcam.video_detail.VideoDetailActivity;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlayActivity extends BaseActivityDI implements PlayActivityView, DatePickerDialog.OnDateSetListener, VideoActionListener {
    private static final String ARG_CALENDAR = "arg_calendar";
    @Inject
    PlayActivityPresenter presenter;

    @BindView(R.id.playRecyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.emptyView)
    TextView emptyView;
    @BindView(R.id.searchLinearLayout)
    LinearLayout searchLinearLayout;
    @BindView(R.id.searchTextView)
    TextView searchTextView;

    private VideoAdapter adapter;

    private static int VIDEO_DETAIL_ACTIVITY_REQUEST_CODE = 42;

    int videoToReloadPosition;

    Calendar myCalendar = Calendar.getInstance();
    private static final String DIALOG_TAG = "DIALOG_TAG";

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            myCalendar = (Calendar) savedInstanceState.getSerializable(ARG_CALENDAR);
        }

        initList();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        updateActionBar();
    }

    private void updateActionBar() {
        if (presenter.isVideoSelected()) {
            getSupportActionBar().setTitle(String.valueOf(presenter.getSelectedCount()));
            getSupportActionBar().setHomeAsUpIndicator(ContextCompat.getDrawable(getContext(), R.drawable.ic_close_white_24dp));
        } else {
            getSupportActionBar().setTitle(R.string.recorded_videos);
            getSupportActionBar().setHomeAsUpIndicator(ContextCompat.getDrawable(getContext(), R.drawable.ic_arrow_back_white_24dp));
        }
    }

    private void initList() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        adapter = new VideoAdapter(presenter.getVideos());
        recyclerView.setAdapter(adapter);

        if (presenter.isShowingFiltered()) {
            showSearchResult();
        }
    }

    private void onBackgroundClicked(int position) {
        onPlayClicked(position);
    }

    private void onMenuClicked(int position) {
        VideoDialog.newInstance(position, this).show(getFragmentManager(), DIALOG_TAG);
    }

    @OnClick(R.id.cancelSearchButton)
    void cancelSearchButtonClicked() {
        presenter.calcelSearch();
        searchLinearLayout.setVisibility(View.GONE);
        adapter.setVideos(presenter.getVideos());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void reloadList() {
        presenter.loadVideosFromDb();
        adapter.setVideos(presenter.getVideos());
        adapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    @Override
    public void reloadItem(int itemPosition) {
        adapter.notifyItemChanged(itemPosition);
    }

    @Override
    public void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    @Override
    public void showSearchResult() {
        searchLinearLayout.setVisibility(View.VISIBLE);
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
        searchTextView.setText(String.format("%s %s", getResources().getString(R.string.displaying_res), dateFormat.format(myCalendar.getTime())));
        adapter.setVideos(presenter.getFilteredVideos());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        myCalendar.set(Calendar.YEAR, year);
        myCalendar.set(Calendar.MONTH, month);
        myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        presenter.filterVideosByDate(myCalendar);
    }

    @Override
    public void notifyItemRemoved(int position) {
        if (position < adapter.getItemCount()) {
            adapter.notifyItemChanged(position);
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onDeleteClicked(final int position) {
        AlertDialog dialog = DialogHelper.getConfirmDialog(getContext(), R.string.delete_dialog_title, R.string.delete_dialog_message, R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                presenter.deleteVideo(position);
                adapter.notifyItemRemoved(position);
                invalidateOptionsMenu();
            }
        });
        dialog.show();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.red));
        }
    }

    @Override
    public void onInfoClicked(int position) {
        Video video = presenter.getVideos().get(position);

        Intent intent = new Intent(getContext(), VideoDetailActivity.class);
        intent.putExtra(Constants.ARG_VIDEO_ID, video.getId());
        videoToReloadPosition = position;
        startActivityForResult(intent, VIDEO_DETAIL_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onPlayClicked(int position) {
        Video video = presenter.getVideos().get(position);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getPathToFile()));
        intent.setDataAndType(Uri.parse(video.getPathToFile()), "video/mp4");
        startActivity(intent);
    }

    @Override
    public void onShareClicked(int position) {
        Video video = presenter.getVideos().get(position);

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

    private void shareSelectedVideos() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Videos from SmartDashCam");
        intent.setType("video/mp4");

        ArrayList<Uri> uris = new ArrayList<>();

        for (String path : presenter.getSelectedFilePaths()) {
            File file = new File(path);
            if (file.exists()) {
                Uri videoUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", file);
                uris.add(videoUri);
            }
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        startActivity(Intent.createChooser(intent, "Share videos"));
    }

    public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
        private List<Video> videos;

        private VideoAdapter(List<Video> videos) {
            setVideos(videos);
        }

        public void setVideos(List<Video> videos) {
            this.videos = videos;

            if (videos.size() == 0) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        }

        @NonNull
        @Override
        public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_row_item, parent, false);
            return new VideoViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final VideoViewHolder holder, int position) {
            final Video video = videos.get(position);
            holder.nameTextView.setText(video.getName());
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
            holder.dateTextView.setText(dateFormat.format(video.getTimestamp()));

            loadThumbnail(video, holder);

            holder.durationTextView.setText(video.getDurationString(getContext()));

            holder.mapImageView.setVisibility(video.hasMapAvailable() ? View.VISIBLE : View.GONE);

            holder.actionButton.setEnabled(!video.isSelected());
            holder.actionButton.setImageDrawable(ContextCompat.getDrawable(getContext(), video.isSelected() ? R.drawable.ic_check_circle_white_24dp : R.drawable.ic_more_vert_white_24dp));
            holder.itemBackground.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    video.setSelected(!video.isSelected());
                    holder.actionButton.setEnabled(!video.isSelected());
                    invalidateOptionsMenu();
                    if (video.isSelected()) {
                        ViewHelper.imageButtonAnimatedChange(getContext(), holder.actionButton, ContextCompat.getDrawable(getContext(), R.drawable.ic_check_circle_white_24dp), 150);
                    } else {
                        ViewHelper.imageButtonAnimatedChange(getContext(), holder.actionButton, ContextCompat.getDrawable(getContext(), R.drawable.ic_more_vert_white_24dp), 150);
                    }
                    invalidateOptionsMenu();
                    return true;
                }
            });

        }

        private void loadThumbnail(Video video, VideoViewHolder holder) {
            boolean imageSuccesfullyLoaded = false;
            //try loading screenshot
            if (video.getPathToScreenshot() != null && !video.getPathToScreenshot().isEmpty()) {
                File imageFile = new File(video.getPathToScreenshot());
                if (imageFile.exists()) {
                    Glide.with(getContext()).load(imageFile).into(holder.thumbnailImageView);
                    holder.thumbnailImageView.setVisibility(View.VISIBLE);
                    holder.errorImageView.setVisibility(View.GONE);
                    imageSuccesfullyLoaded = true;
                }
            }
            //try loading mapImage
            if (!imageSuccesfullyLoaded) {
                if (video.getPathToMaoImage() != null && !video.getPathToMaoImage().isEmpty()) {
                    File imageFile = new File(video.getPathToMaoImage());
                    if (imageFile.exists()) {
                        Glide.with(getContext()).load(imageFile).into(holder.thumbnailImageView);
                        holder.thumbnailImageView.setVisibility(View.VISIBLE);
                        holder.errorImageView.setVisibility(View.GONE);
                        imageSuccesfullyLoaded = true;
                    }
                }
            }
            //give up
            if (!imageSuccesfullyLoaded) {
                holder.thumbnailImageView.setVisibility(View.GONE);
                holder.errorImageView.setVisibility(View.VISIBLE);
            }

        }

        @Override
        public int getItemCount() {
            return videos.size();
        }

        public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            @BindView(R.id.nameTextView)
            TextView nameTextView;
            @BindView(R.id.dateTextView)
            TextView dateTextView;
            @BindView(R.id.durationTextView)
            TextView durationTextView;
            @BindView(R.id.itemBackground)
            LinearLayout itemBackground;
            @BindView(R.id.thumbnailImageView)
            ImageView thumbnailImageView;
            @BindView(R.id.errorImageView)
            ImageView errorImageView;
            @BindView(R.id.mapImageView)
            ImageView mapImageView;
            @BindView(R.id.actionButton)
            ImageButton actionButton;

            public VideoViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemBackground.setOnClickListener(this);
                actionButton.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (v == itemBackground) {
                    PlayActivity.this.onBackgroundClicked(getAdapterPosition());
                } else if (v == actionButton) {
                    PlayActivity.this.onMenuClicked(getAdapterPosition());
                }
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.menu_deleteAll) {
            AlertDialog dialog;
            if (presenter.isVideoSelected()) {
                dialog = DialogHelper.getConfirmDialog(getContext(), R.string.delete_selected, R.string.delete_selected_message, R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.deleteSelectedVideos();
                    }
                });
            } else {
                dialog = DialogHelper.getConfirmDialog(getContext(), R.string.delete_all, R.string.delete_all_message, R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.deleteAllVideos();
                    }
                });
            }
            dialog.show();

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.red));
            }
        } else if (item.getItemId() == R.id.menu_search) {
            new DatePickerDialog(PlayActivity.this, this, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        } else if (item.getItemId() == R.id.menu_share) {
            shareSelectedVideos();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (presenter.isVideoSelected()) {
            presenter.deselectAllVideos();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_play, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteItem = menu.findItem(R.id.menu_deleteAll);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        MenuItem shareItem = menu.findItem(R.id.menu_share);

        boolean notEmpty = presenter.getVideos() != null && !presenter.getVideos().isEmpty();

        deleteItem.setVisible(notEmpty);
        searchItem.setVisible(notEmpty && !presenter.isVideoSelected());
        shareItem.setVisible(notEmpty && presenter.isVideoSelected());

        updateActionBar();
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_CALENDAR, myCalendar);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //video name might have been changed
        if (requestCode == VIDEO_DETAIL_ACTIVITY_REQUEST_CODE) {
            presenter.reloadVideo(videoToReloadPosition);
        }
    }
}
