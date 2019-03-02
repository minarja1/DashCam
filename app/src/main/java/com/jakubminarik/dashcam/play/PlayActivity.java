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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
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
import com.jakubminarik.dashcam.model.Video;
import com.jakubminarik.dashcam.play.dialog.VideoActionListener;
import com.jakubminarik.dashcam.video_detail.VideoDetailActivity;

import java.io.File;
import java.text.DateFormat;
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
        getSupportActionBar().setTitle(getResources().getString(R.string.videos));
    }

    private void initList() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));

        adapter = new VideoAdapter(presenter.getVideos());
        recyclerView.setAdapter(adapter);

        if (presenter.isShowingFiltered()) {
            showSearchResult();
        }
    }

    private void onVideoSelected(int position) {
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
        startActivity(intent);
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
        public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
            Video video = videos.get(position);
            holder.nameTextView.setText(video.getName());
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
            holder.dateTextView.setText(dateFormat.format(video.getTimestamp()));

            boolean imageSuccesfullyLoaded = false;

            //try loading screenshot
            if (video.getPathToScreenshot() != null && !video.getPathToScreenshot().isEmpty()) {
                File imageFile = new File(video.getPathToScreenshot());
                if (imageFile.exists()) {
                    Glide.with(getContext()).load(imageFile).into(holder.mapThumbnailImageView);
                    holder.mapThumbnailImageView.setVisibility(View.VISIBLE);
                    holder.errorImageView.setVisibility(View.GONE);
                    imageSuccesfullyLoaded = true;
                }
            }
            //try loading mapImage
            if (!imageSuccesfullyLoaded) {
                if (video.getPathToMaoImage() != null && !video.getPathToMaoImage().isEmpty()) {
                    File imageFile = new File(video.getPathToMaoImage());
                    if (imageFile.exists()) {
                        Glide.with(getContext()).load(imageFile).into(holder.mapThumbnailImageView);
                        holder.mapThumbnailImageView.setVisibility(View.VISIBLE);
                        holder.errorImageView.setVisibility(View.GONE);
                        imageSuccesfullyLoaded = true;
                    }
                }
            }
            //give up
            if (!imageSuccesfullyLoaded) {
                holder.mapThumbnailImageView.setVisibility(View.GONE);
                holder.errorImageView.setVisibility(View.VISIBLE);
            }

            holder.durationTextView.setText(video.getDurationString(getContext()));

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
            @BindView(R.id.mapThumbnailImageView)
            ImageView mapThumbnailImageView;
            @BindView(R.id.errorImageView)
            ImageView errorImageView;

            public VideoViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemBackground.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                PlayActivity.this.onVideoSelected(getAdapterPosition());
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.menu_deleteAll) {
            AlertDialog dialog = DialogHelper.getConfirmDialog(getContext(), R.string.delete_all, R.string.delete_all_message, R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    presenter.deleteAllVideos();
                }
            });
            dialog.show();

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.red));
            }

        } else if (item.getItemId() == R.id.menu_search) {
            new DatePickerDialog(PlayActivity.this, this, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_play, menu);

        MenuItem deleteItem = menu.findItem(R.id.menu_deleteAll);
        MenuItem searchItem = menu.findItem(R.id.menu_search);

        boolean notEmpty = presenter.getVideos() != null && !presenter.getVideos().isEmpty();
        deleteItem.setVisible(notEmpty);
        searchItem.setVisible(notEmpty);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_CALENDAR, myCalendar);
    }
}
