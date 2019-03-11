package com.jakubminarik.dashcam.play;

import com.jakubminarik.dashcam.base.BaseView;

public interface PlayActivityView extends BaseView {
    void reloadList();

    void showSearchResult();

    void reloadItem(int itemPosition);
}
