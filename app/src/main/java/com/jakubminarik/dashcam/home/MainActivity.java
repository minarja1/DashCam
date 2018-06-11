package com.jakubminarik.dashcam.home;

import android.os.Bundle;

import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.base.BaseActivityDI;
import com.jakubminarik.dashcam.base.BasePresenter;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivityDI implements MainActivityView{
    @Inject
    MainActivityPresenter presenter;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @OnClick(R.id.aboutButton)
    public void aboutButtonClicked() {
        presenter.aboutButtonClicked();
    }


    @OnClick(R.id.recordButton)
    public void recordButtonClicked() {
        presenter.recordButtonClicked();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }


}
