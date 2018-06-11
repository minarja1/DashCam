package com.jakubminarik.dashcam.base;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import butterknife.ButterKnife;
import dagger.android.AndroidInjection;

public abstract class BaseActivityDI extends BaseActivity implements BaseView {
    private BasePresenter presenter;

    public abstract BasePresenter getPresenter();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        presenter = getPresenter();
        if (presenter != null) {
            getPresenter().onAttach(this);
            if (savedInstanceState != null) {
                presenter.onEnter(savedInstanceState);
            } else {
                presenter.onEnter(getIntent().getExtras());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            getPresenter().onDetach();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (presenter != null) {
            presenter.onExit(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void finishActivity() {
        finish();
    }
}
