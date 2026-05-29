package giangvc.cntt.ntu.footballhub.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import giangvc.cntt.ntu.footballhub.Adapters.TeamAdapter;
import giangvc.cntt.ntu.footballhub.Models.Team;
import giangvc.cntt.ntu.footballhub.R;

/**
 * TeamManagementActivity
 *
 * Displays a real-time list of teams fetched from the Firestore "Teams" collection.
 * The FAB navigates to AddTeamActivity to create new teams.
 */
public class TeamManagementActivity extends AppCompatActivity {

    private static final String TAG = "TeamMgmt";

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView        rvTeams;
    private FloatingActionButton fabAddTeam;

    // ── Data & Adapter ────────────────────────────────────────────────────────
    private TeamAdapter  teamAdapter;
    private List<Team>   teamList;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_management);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Firebase
        db = FirebaseFirestore.getInstance();

        // Views
        rvTeams    = findViewById(R.id.rvTeams);
        fabAddTeam = findViewById(R.id.fabAddTeam);

        // RecyclerView setup
        teamList    = new ArrayList<>();
        teamAdapter = new TeamAdapter(teamList);
        rvTeams.setLayoutManager(new LinearLayoutManager(this));
        rvTeams.setAdapter(teamAdapter);

        // Optional: handle item clicks (placeholder for future detail screen)
        teamAdapter.setOnTeamClickListener(team ->
                Toast.makeText(this, "Selected: " + team.getTeamName(), Toast.LENGTH_SHORT).show()
        );

        // FAB → navigate to AddTeamActivity
        fabAddTeam.setOnClickListener(v ->
                startActivity(new Intent(TeamManagementActivity.this, AddTeamActivity.class))
        );

        // Start real-time listener
        listenToTeams();
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attaches a Firestore snapshot listener on the "Teams" collection.
     * The UI updates automatically whenever data changes in Firestore.
     */
    private void listenToTeams() {
        db.collection("Teams")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage());
                        Toast.makeText(this, "Failed to load teams.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

                    // Rebuild the list from the snapshot
                    teamList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Team team = doc.toObject(Team.class);
                        if (team != null) {
                            teamList.add(team);
                        }
                    }

                    // Notify adapter of full data change
                    teamAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Teams loaded: " + teamList.size());
                });
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
