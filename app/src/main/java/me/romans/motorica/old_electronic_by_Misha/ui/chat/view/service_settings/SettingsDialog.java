package me.romans.motorica.old_electronic_by_Misha.ui.chat.view.service_settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;

import me.romans.motorica.R;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;

public class SettingsDialog extends AppCompatDialogFragment {
    private ChartActivity mChartActivity;
    private EditText password_et;
    private SettingsDialogListener mSettingsDialogListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder settingsDialog = new AlertDialog.Builder(
                Objects.requireNonNull(getActivity()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog, null);
        if (getActivity() != null) {mChartActivity = (ChartActivity) getActivity();}

        settingsDialog.setView(view);
        settingsDialog.setTitle(R.string.advanced_settings);
        settingsDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = password_et.getText().toString();
                mSettingsDialogListener.passwordServiceSettings(password);

                mChartActivity.openServiceSettings();
            }
        });

        settingsDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        password_et = view.findViewById(R.id.password_et);

        return settingsDialog.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mSettingsDialogListener = (SettingsDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
            "must implement SettingsDialogListener");
        }
    }

    public interface SettingsDialogListener {
        void passwordServiceSettings(String password);
    }
}
