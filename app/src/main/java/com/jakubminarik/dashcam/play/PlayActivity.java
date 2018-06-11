package com.jakubminarik.dashcam.play;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.base.BaseActivityDI;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.model.Video;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlayActivity extends BaseActivityDI implements PlayActivityView, DatePickerDialog.OnDateSetListener {
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

        adapter = new VideoAdapter(presenter.getVideos(), new VideoClickListener() {
            @Override
            public void onDeleteClicked(final int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(R.string.delete_dialog_message)
                        .setTitle(R.string.delete_dialog_title);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        presenter.deleteVideo(position, true);
                    }
                });

                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }

            @Override
            public void onInfoClicked(int position) {
//todo
            }

            @Override
            public void onBackgroundClicked(int position) {
                Video video = presenter.getVideos().get(position);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getPathToFile()));
                intent.setDataAndType(Uri.parse(video.getPathToFile()), "video/mp4");
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        if (presenter.isShowingFiltered()) {
            showSearchResult();
        }
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
    }

    @Override
    public void showSearchResult() {
        searchLinearLayout.setVisibility(View.VISIBLE);
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
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

    public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
        private List<Video> videos;
        private final VideoClickListener listener;

        private VideoAdapter(List<Video> videos, VideoClickListener listener) {
            setVideos(videos);
            this.listener = listener;
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
            return new VideoViewHolder(v, listener);
        }

        @Override
        public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
            Video video = videos.get(position);
            holder.nameTextView.setText(video.getName());
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
            holder.dateTextView.setText(dateFormat.format(video.getTimestamp()));
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
            @BindView(R.id.deleteButton)
            ImageButton deleteButton;
            @BindView(R.id.infoButton)
            ImageButton infoButton;
            @BindView(R.id.itemBackground)
            LinearLayout itemBackground;

            private WeakReference<VideoClickListener> listenerRef;

            public VideoViewHolder(View itemView, VideoClickListener listener) {
                super(itemView);
                listenerRef = new WeakReference<>(listener);
                ButterKnife.bind(this, itemView);

                deleteButton.setOnClickListener(this);
                infoButton.setOnClickListener(this);
                itemBackground.setOnClickListener(this);
            }


            @Override
            public void onClick(View v) {
                if (v.getId() == deleteButton.getId()) {
                    listenerRef.get().onDeleteClicked(getAdapterPosition());
                } else if (v.getId() == infoButton.getId()) {
                    listenerRef.get().onInfoClicked(getAdapterPosition());
                } else if (v.getId() == itemBackground.getId()) {
                    listenerRef.get().onBackgroundClicked(getAdapterPosition());
                }
            }
        }


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.menu_deleteAll) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.delete_all_message)
                    .setTitle(R.string.delete_all);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    presenter.deleteAllVideos();
                }
            });

            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

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

        return true;
    }

    public interface VideoClickListener {
        void onDeleteClicked(int position);

        void onInfoClicked(int position);

        void onBackgroundClicked(int position);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_CALENDAR, myCalendar);
    }
}
