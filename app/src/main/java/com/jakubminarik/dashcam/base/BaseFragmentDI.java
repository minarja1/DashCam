package com.jakubminarik.dashcam.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class BaseFragmentDI extends BaseFragment implements BaseView {

    private BasePresenter presenter;

    public abstract void inject();

    public abstract BasePresenter getPresenter();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inject();
        presenter = getPresenter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (presenter != null) {
            presenter.onAttach(this);

            if (savedInstanceState != null) {
                presenter.onEnter(savedInstanceState);
            } else {
                presenter.onEnter(getArguments());
            }
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (presenter != null) {
            presenter.onStart();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

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
    public void finishActivity() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

}

