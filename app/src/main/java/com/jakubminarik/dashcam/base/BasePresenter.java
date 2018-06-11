package com.jakubminarik.dashcam.base;

import android.os.Bundle;

public abstract class BasePresenter<V extends BaseView> {
    public abstract void onAttach(V view);

    public abstract void onDetach();

    /**
     * Method called on enter activity/fragment
     *
     * @param bundle A bundle to get arguments from
     */
    public abstract void onEnter(Bundle bundle);

    /**
     * Method called on activity/fragment leave
     *
     * @param bundle A bundle to put arguments to
     */
    public abstract void onExit(Bundle bundle);


    /**
     * Override if you want to perform operations after all views are created.
     */
    public void onStart() {

    }
}
