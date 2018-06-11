package com.jakubminarik.dashcam.about;

import android.os.Bundle;

import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.base.BaseActivityDI;
import com.jakubminarik.dashcam.base.BasePresenter;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;

public class AboutActivity extends BaseActivityDI implements AboutActivityView {
    @Inject
    AboutActivityPresenter presenter;

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.aboutButton)
    public void aboutButtonClicked() {
        presenter.aboutButtonClicked();
    }
}
