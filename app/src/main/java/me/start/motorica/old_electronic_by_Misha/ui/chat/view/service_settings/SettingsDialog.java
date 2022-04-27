package me.start.motorica.old_electronic_by_Misha.ui.chat.view.service_settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import org.jetbrains.annotations.NotNull;

import me.start.motorica.R;
import me.start.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;

public class SettingsDialog extends AppCompatDialogFragment {
    private ChartActivity mChartActivity;
    private EditText password_et;
    private SettingsDialogListener mSettingsDialogListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder settingsDialog = new AlertDialog.Builder(
                requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog, null);
        if (getActivity() != null) {mChartActivity = (ChartActivity) getActivity();}

        settingsDialog.setView(view);
        settingsDialog.setTitle(R.string.advanced_settings);
        settingsDialog.setPositiveButton(R.string.ok, (dialog, which) -> {
            String password = password_et.getText().toString();
            mSettingsDialogListener.passwordServiceSettings(password);
            mChartActivity.openServiceSettings();
        });

        settingsDialog.setNegativeButton(R.string.cancel, (dialog, which) -> {
        });

        password_et = view.findViewById(R.id.password_et);

        return settingsDialog.create();
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        try {
            mSettingsDialogListener = (SettingsDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context +
            "must implement SettingsDialogListener");
        }
    }

    public interface SettingsDialogListener {
        void passwordServiceSettings(String password);
    }
}
