package com.jakubminarik.dashcam.helper;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.jakubminarik.dashcam.R;

public class DialogHelper {


    public static android.support.v7.app.AlertDialog getConfirmDialog(Context context, CharSequence message) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);

        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

    public static AlertDialog getConfirmDialog(Context context, @StringRes int title, @StringRes int message, @StringRes int okMessage, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(okMessage, listener);
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

}
