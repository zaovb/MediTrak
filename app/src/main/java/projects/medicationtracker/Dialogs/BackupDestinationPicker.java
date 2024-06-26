package projects.medicationtracker.Dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import projects.medicationtracker.Interfaces.IDialogCloseListener;
import projects.medicationtracker.R;

public class BackupDestinationPicker extends DialogFragment {
    private String exportDir;
    private String exportFile;
    private TextInputLayout fileNameInputLayout;
    private final String fileExtension;

    public BackupDestinationPicker(String fileExtension, String defaultName) {
        this.fileExtension = fileExtension;
        exportFile = defaultName;
    }

    public BackupDestinationPicker(String fileExtension) {
        LocalDateTime now = LocalDateTime.now();

        this.fileExtension = fileExtension;
        exportFile = "meditrak_"
                + "_" + now.getYear()
                + "_" + now.getMonthValue()
                + "_" + now.getDayOfMonth()
                + "_" + now.getHour()
                + "_" + now.getMinute()
                + "_" + now.getSecond();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstances) {
        AlertDialog dialog;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        final String[] directories;
        ArrayAdapter<String> adapter;
        ArrayList<String> dirs = new ArrayList<>();

        builder.setView(inflater.inflate(R.layout.dialog_backup_destination_picker, null));

        builder.setTitle(getString(R.string.export_data));
        builder.setPositiveButton(R.string.export, ((dialogInterface, i) -> {
            onExportClick();
            dismiss();
        }));
        builder.setNegativeButton(R.string.cancel, ((dialogInterface, i) -> dismiss()));

        dialog = builder.create();
        dialog.show();

        MaterialAutoCompleteTextView dirSelector = dialog.findViewById(R.id.export_dir);
        fileNameInputLayout = dialog.findViewById(R.id.export_file_layout);
        TextInputEditText fileName = dialog.findViewById(R.id.export_file);
        ((TextView) dialog.findViewById(R.id.file_extension)).setText("." + fileExtension);

        dirs.add(getString(R.string.downloads));
        dirs.add(getString(R.string.documents));

        directories = new String[] {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath()
        };

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, dirs);

        dirSelector.setAdapter(adapter);
        dirSelector.setText(dirSelector.getAdapter().getItem(0).toString(), false);

        exportDir = directories[0];

        dirSelector.setOnItemClickListener((parent, view, position, id) -> exportDir = directories[position]);

        fileName.setText(exportFile);

        fileName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                Optional<String> fileNameChecker;
                exportFile = editable.toString();

                fileNameChecker = Optional.of(exportFile)
                        .filter(f -> f.contains("."))
                        .map(f -> f.substring(exportFile.lastIndexOf(".") + 1));

                if (exportFile.isEmpty()) {
                    fileNameInputLayout.setError(getString(R.string.err_missing_file_name));
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

                    return;
                } else if (fileNameChecker.isPresent() && !fileNameChecker.get().equals("json")) {
                    fileNameInputLayout.setError(getString(R.string.err_file_must_be_json));
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

                    return;
                }

                fileNameInputLayout.setErrorEnabled(false);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            }
        });

        return dialog;
    }

    private void onExportClick() {
        if (getActivity() instanceof IDialogCloseListener) {
            ((IDialogCloseListener) getActivity()).handleDialogClose(
                IDialogCloseListener.Action.CREATE,
                new String[] { exportDir, exportFile, fileExtension }
            );
        }
    }
}
