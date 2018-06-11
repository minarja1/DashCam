package com.jakubminarik.dashcam.record;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.base.BaseActivityDI;
import com.jakubminarik.dashcam.base.BasePresenter;
import javax.inject.Inject;



public class RecordActivity extends BaseActivityDI implements RecordActivityView, View.OnClickListener {
    @Inject
    RecordActivityPresenter presenter;

    @Override
    public void onClick(View v) {

    }

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, RecordFragment.newInstance())
                    .commit();
        }
    }
}
