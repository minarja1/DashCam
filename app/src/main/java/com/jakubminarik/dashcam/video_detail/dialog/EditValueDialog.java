package com.jakubminarik.dashcam.video_detail.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.helper.ViewHelper;

import java.io.Serializable;

public class EditValueDialog extends DialogFragment {

    public static final String ARG_DIALOG_TEXT = "ARG_DIALOG_TEXT";
    public static final String ARG_DIALOG_TITLE = "ARG_DIALOG_TITLE";
    public static final String ARG_DIALOG_HINT = "ARG_DIALOG_HINT";
    public static final String ARG_DIALOG_LISTENER = "ARG_DIALOG_LISTENER";

    private OnTextSumbittedListener listener;
    private EditText editText;

    public static EditValueDialog newInstance(@Nullable String text, @Nullable String title, @Nullable String hint) {
        Bundle args = new Bundle();
        EditValueDialog fragment = new EditValueDialog();
        args.putString(ARG_DIALOG_TEXT, text);
        args.putString(ARG_DIALOG_TITLE, title);
        args.putString(ARG_DIALOG_HINT, hint);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(OnTextSumbittedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_DIALOG_LISTENER)) {
            listener = (OnTextSumbittedListener) savedInstanceState.getSerializable(ARG_DIALOG_LISTENER);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (listener != null) {
            outState.putSerializable(ARG_DIALOG_LISTENER, listener);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_edit_value, null);
        editText = view.findViewById(R.id.editText);
        String text = getArguments().getString(ARG_DIALOG_TEXT);
        String title = getArguments().getString(ARG_DIALOG_TITLE);
        String hint = getArguments().getString(ARG_DIALOG_HINT);

        if (text != null) {
            editText.setText(text);
        }
        if (hint != null) {
            editText.setHint(hint);
        }
        if (title == null) {
            title = "";
        }

        AlertDialog dialog = builder.setView(view)
                .setTitle(title)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (listener != null) {
                            listener.onTextSubmitted(editText.getText().toString());
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        ViewHelper.showKeyboard(editText);

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.green));
        }

        return dialog;
    }


    public interface OnTextSumbittedListener extends Serializable {
        void onTextSubmitted(String text);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getContext() != null) {
            ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getContext(), R.color.green));
        }
    }

}
