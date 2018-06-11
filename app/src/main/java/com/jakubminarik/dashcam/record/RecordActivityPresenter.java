package com.jakubminarik.dashcam.record;

import android.os.Bundle;

import com.jakubminarik.dashcam.base.BasePresenter;

import javax.inject.Inject;

public class RecordActivityPresenter extends BasePresenter<RecordActivityView> {
    RecordActivityView view;

    @Inject
    public RecordActivityPresenter() {
    }
    @Override
    public void onAttach(RecordActivityView view) {
        this.view = view;
    }

    @Override
    public void onDetach() {
        this.view = null;
    }

    @Override
    public void onEnter(Bundle bundle) {

    }

    @Override
    public void onExit(Bundle bundle) {

    }
}
