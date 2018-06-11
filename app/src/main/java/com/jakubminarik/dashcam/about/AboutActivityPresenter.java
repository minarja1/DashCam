package com.jakubminarik.dashcam.about;

import android.os.Bundle;
import android.widget.Toast;

import com.jakubminarik.dashcam.base.BasePresenter;

import javax.inject.Inject;

public class AboutActivityPresenter extends BasePresenter<AboutActivityView> {
    private AboutActivityView view;

    @Inject
    public AboutActivityPresenter() {
    }

    @Override
    public void onAttach(AboutActivityView view) {
        this.view = view;
    }

    @Override
    public void onDetach() {
        view = null;
    }

    @Override
    public void onEnter(Bundle bundle) {

    }

    @Override
    public void onExit(Bundle bundle) {

    }


    public void aboutButtonClicked() {
        Toast.makeText(view.getContext(), "about", Toast.LENGTH_SHORT).show();
    }
}
