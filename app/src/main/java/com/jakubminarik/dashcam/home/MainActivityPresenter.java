package com.jakubminarik.dashcam.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.jakubminarik.dashcam.about.AboutActivity;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.record.RecordActivity;

import javax.inject.Inject;

public class MainActivityPresenter extends BasePresenter<MainActivityView> {
    MainActivityView view;

    //this constructor allows Dagger to create an instance of this class
    //as explained in tutorial at http://www.vogella.com/tutorials/Dagger/article.html
    @Inject
    public MainActivityPresenter() {
    }

    @Override
    public void onAttach(MainActivityView view) {
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
        Intent intent = new Intent(view.getContext(), AboutActivity.class);
        view.getContext().startActivity(intent);
    }

    public void recordButtonClicked() {
        Intent intent = new Intent(view.getContext(), RecordActivity.class);
        view.getContext().startActivity(intent);
    }


}
