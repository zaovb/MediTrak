package projects.medicationtracker.Fragments;

import static projects.medicationtracker.Helpers.DBHelper.DATE_FORMAT;
import static projects.medicationtracker.Helpers.DBHelper.TIME_FORMAT;
import static projects.medicationtracker.MainActivity.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import projects.medicationtracker.AddMedication;
import projects.medicationtracker.Helpers.DBHelper;
import projects.medicationtracker.MedicationHistory;
import projects.medicationtracker.MedicationNotes;
import projects.medicationtracker.R;
import projects.medicationtracker.SimpleClasses.Medication;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MyMedicationsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyMedicationsFragment extends Fragment {
    TextView name;
    TextView dosage;
    TextView alias;
    TextView frequency;
    TextView takenSince;
    Button notesButton;
    Button editButton;
    Button historyButton;

    public MyMedicationsFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MyMedicationsFragment.
     */
    public static MyMedicationsFragment newInstance() {
        return new MyMedicationsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Medication med = requireArguments().getParcelable("MediTrakCore/Medication");

        final View rootView = inflater.inflate(R.layout.fragment_my_medications, container, false);

        insertMedicationData(med, rootView);

        return rootView;
    }

    private void insertMedicationData(Medication medication, View v) {
        DBHelper db = new DBHelper(getContext());
        LocalTime[] times = db.getMedicationTimes(medication.getId());
        LocalDateTime[] dateTimes = new LocalDateTime[times.length];
        String dosageVal;

        name = v.findViewById(R.id.myMedCardMedicationName);
        dosage = v.findViewById(R.id.myMedCardDosage);
        alias = v.findViewById(R.id.myMedCardAlias);
        frequency = v.findViewById(R.id.myMedCardFrequency);
        takenSince = v.findViewById(R.id.myMedCardTakenSince);
        notesButton = v.findViewById(R.id.myMedsNotes);
        editButton = v.findViewById(R.id.myMedsEdit);
        historyButton = v.findViewById(R.id.history_button);

        for (int i = 0; i < times.length; i++) {
            dateTimes[i] = LocalDateTime.of(medication.getStartDate().toLocalDate(), times[i]);
        }

        medication.setTimes(dateTimes);

        if (medication.getDosage() == medication.getDosage()) {
            dosageVal = String.format(Locale.getDefault(), "%d", medication.getDosage());
        } else {
            dosageVal = String.valueOf(medication.getDosage());
        }

        name.setText(getString(R.string.med_name, medication.getName()));
        dosage.setText(getString(R.string.dosage, dosageVal, medication.getDosageUnits()));

        String label = medication.generateFrequencyLabel(
                getContext(),
                preferences.getString(DATE_FORMAT),
                preferences.getString(TIME_FORMAT)
        );

        frequency.setText(label);

        if (!medication.getAlias().equals("")) {
            alias.setVisibility(View.VISIBLE);
            alias.setText(getString(R.string.alias_lbl, medication.getAlias()));
        }

        LocalDateTime start = medication.getParent() == null ? medication.getStartDate() : medication.getParent().getStartDate();

        String beginning = DateTimeFormatter.ofPattern(
                preferences.getString(DATE_FORMAT),
                Locale.getDefault()
        ).format(start);

        takenSince.setText(getString(R.string.taken_since, beginning));

        Intent notesIntent = new Intent(getActivity(), MedicationNotes.class);
        notesIntent.putExtra("medId", medication.getId());

        notesButton.setOnClickListener(view ->
        {
            getActivity().finish();
            getActivity().startActivity(notesIntent);
        });

        Intent editMedIntent = new Intent(getActivity(), AddMedication.class);
        editMedIntent.putExtra("medId", medication.getId());

        editButton.setOnClickListener(view ->
        {
            getActivity().finish();
            getActivity().startActivity(editMedIntent);
        });

        Intent intent = new Intent(getActivity(), MedicationHistory.class);

        historyButton.setOnClickListener(view -> {
            intent.putExtra("ID", medication.getId());
            getActivity().finish();
            startActivity(intent);
        });
    }
}
