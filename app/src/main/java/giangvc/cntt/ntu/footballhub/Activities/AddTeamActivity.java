package giangvc.cntt.ntu.footballhub.Activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import giangvc.cntt.ntu.footballhub.Models.Team;
import giangvc.cntt.ntu.footballhub.R;

/**
 * AddTeamActivity
 *
 * Provides a form to create a new Team document in the Firestore "Teams" collection.
 * A unique document ID is generated via {@code db.collection("Teams").document().getId()}.
 */
public class AddTeamActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputEditText etTeamName;
    private TextInputEditText etCoachName;
    private MaterialButton    btnSaveTeam;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_team);

        // Enable back navigation in the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add New Team");
        }

        // Firebase
        db = FirebaseFirestore.getInstance();

        // Bind views
        etTeamName  = findViewById(R.id.etTeamName);
        etCoachName = findViewById(R.id.etCoachName);
        btnSaveTeam = findViewById(R.id.btnSaveTeam);

        // Save button listener
        btnSaveTeam.setOnClickListener(v -> saveTeam());
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Validates inputs, builds the Team object, and writes it to Firestore. */
    private void saveTeam() {
        String teamName  = etTeamName.getText()  != null ? etTeamName.getText().toString().trim()  : "";
        String coachName = etCoachName.getText() != null ? etCoachName.getText().toString().trim() : "";

        // ── Validation ────────────────────────────────────────────────────────
        if (TextUtils.isEmpty(teamName)) {
            etTeamName.setError("Team name is required");
            etTeamName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(coachName)) {
            etCoachName.setError("Coach name is required");
            etCoachName.requestFocus();
            return;
        }

        // ── Generate a unique Firestore document ID ───────────────────────────
        String teamId = db.collection("Teams").document().getId();

        // ── Build the Team object (start with 0 stats) ────────────────────────
        Team newTeam = new Team(
                teamId,
                teamName,
                coachName,
                "",   // logoUrl — can be updated later
                0,    // matchesPlayed
                0,    // points
                0     // goalDifference
        );

        // Disable button to prevent double-tap
        btnSaveTeam.setEnabled(false);

        // ── Write to Firestore ────────────────────────────────────────────────
        db.collection("Teams")
                .document(teamId)
                .set(newTeam)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Team \"" + teamName + "\" added!", Toast.LENGTH_SHORT).show();
                    finish(); // Return to TeamManagementActivity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSaveTeam.setEnabled(true); // Re-enable on failure
                });
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
