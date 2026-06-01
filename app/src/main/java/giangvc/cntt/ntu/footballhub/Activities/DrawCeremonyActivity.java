package giangvc.cntt.ntu.footballhub.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import giangvc.cntt.ntu.footballhub.Models.Match;
import giangvc.cntt.ntu.footballhub.Models.Tournament;
import giangvc.cntt.ntu.footballhub.R;

public class DrawCeremonyActivity extends AppCompatActivity {

    private static final String TAG = "DrawCeremonyActivity";
    private Spinner spinnerTournaments;
    private LinearLayout layoutSeedsContainer;
    private MaterialButton btnSaveDraw;

    private FirebaseFirestore db;
    private List<Tournament> pendingTournaments = new ArrayList<>();
    private Tournament selectedTournament = null;

    // Map: Seed Index (1 to N) -> Spinner view
    private Map<Integer, Spinner> seedSpinners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_ceremony);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();

        spinnerTournaments = findViewById(R.id.spinnerTournaments);
        layoutSeedsContainer = findViewById(R.id.layoutSeedsContainer);
        btnSaveDraw = findViewById(R.id.btnSaveDraw);

        btnSaveDraw.setOnClickListener(v -> saveDraw());

        loadPendingTournaments();
    }

    private void loadPendingTournaments() {
        db.collection("Tournaments")
                .whereEqualTo("status", Tournament.STATUS_PENDING_DRAW)
                .get()
                .addOnSuccessListener(snapshots -> {
                    pendingTournaments.clear();
                    List<String> names = new ArrayList<>();
                    
                    if (snapshots.isEmpty()) {
                        names.add("Không có giải đấu nào chờ bốc thăm");
                        btnSaveDraw.setEnabled(false);
                    } else {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Tournament t = doc.toObject(Tournament.class);
                            if (t != null) {
                                pendingTournaments.add(t);
                                names.add(t.getTournamentName());
                            }
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTournaments.setAdapter(adapter);

                    spinnerTournaments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (!pendingTournaments.isEmpty()) {
                                selectedTournament = pendingTournaments.get(position);
                                buildSeedUI(selectedTournament);
                            }
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    private void buildSeedUI(Tournament t) {
        layoutSeedsContainer.removeAllViews();
        seedSpinners.clear();

        if (t == null || t.getTeamIds() == null || t.getTeamIds().isEmpty()) return;

        int n = t.getMaxTeams(); // Hoặc t.getTeamIds().size();
        if (t.getTeamIds().size() != n) {
            n = t.getTeamIds().size(); // Đảm bảo an toàn
        }

        List<String> teamOptions = new ArrayList<>();
        teamOptions.add("-- Chọn đội bóng --");
        teamOptions.addAll(t.getTeamNames());

        for (int i = 1; i <= n; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvLabel = new TextView(this);
            tvLabel.setText("Vị trí " + i + " (Đội " + i + "):");
            tvLabel.setTextSize(14);
            tvLabel.setTextColor(Color.BLACK);
            tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Spinner spinner = new Spinner(this);
            spinner.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            spinner.setBackgroundResource(R.drawable.bg_edittext);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            row.addView(tvLabel);
            row.addView(spinner);
            
            layoutSeedsContainer.addView(row);
            seedSpinners.put(i, spinner);
        }
        
        btnSaveDraw.setEnabled(true);
    }

    private void saveDraw() {
        if (selectedTournament == null) return;

        // Validation
        Map<Integer, String> seedToTeamId = new HashMap<>();
        Map<Integer, String> seedToTeamName = new HashMap<>();
        Set<String> selectedTeamIds = new HashSet<>();

        for (Map.Entry<Integer, Spinner> entry : seedSpinners.entrySet()) {
            int seed = entry.getKey();
            int selectedPosition = entry.getValue().getSelectedItemPosition();
            
            if (selectedPosition == 0) {
                Toast.makeText(this, "Vui lòng chọn đội bóng cho tất cả vị trí", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // selectedPosition - 1 vì index 0 là "-- Chọn đội bóng --"
            String teamId = selectedTournament.getTeamIds().get(selectedPosition - 1);
            String teamName = selectedTournament.getTeamNames().get(selectedPosition - 1);
            
            if (selectedTeamIds.contains(teamId)) {
                Toast.makeText(this, "Đội bóng '" + teamName + "' bị trùng ở nhiều vị trí", Toast.LENGTH_SHORT).show();
                return;
            }
            
            selectedTeamIds.add(teamId);
            seedToTeamId.put(seed, teamId);
            seedToTeamName.put(seed, teamName);
        }

        btnSaveDraw.setEnabled(false);
        btnSaveDraw.setText("Đang lưu kết quả...");

        // Update matches
        db.collection("Matches")
                .whereEqualTo("tournamentId", selectedTournament.getTournamentId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = db.batch();
                    
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Match match = doc.toObject(Match.class);
                        if (match == null) continue;

                        boolean updated = false;

                        // Check Team 1
                        if (match.getTeam1Id() != null && match.getTeam1Id().startsWith("SEED_")) {
                            try {
                                int seedNum = Integer.parseInt(match.getTeam1Id().replace("SEED_", ""));
                                if (seedToTeamId.containsKey(seedNum)) {
                                    match.setTeam1Id(seedToTeamId.get(seedNum));
                                    match.setTeam1Name(seedToTeamName.get(seedNum));
                                    updated = true;
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Invalid SEED format: " + match.getTeam1Id());
                            }
                        }

                        // Check Team 2
                        if (match.getTeam2Id() != null && match.getTeam2Id().startsWith("SEED_")) {
                            try {
                                int seedNum = Integer.parseInt(match.getTeam2Id().replace("SEED_", ""));
                                if (seedToTeamId.containsKey(seedNum)) {
                                    match.setTeam2Id(seedToTeamId.get(seedNum));
                                    match.setTeam2Name(seedToTeamName.get(seedNum));
                                    updated = true;
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Invalid SEED format: " + match.getTeam2Id());
                            }
                        }

                        if (updated) {
                            batch.set(doc.getReference(), match);
                        }
                    }

                    // Update tournament status
                    batch.update(db.collection("Tournaments").document(selectedTournament.getTournamentId()),
                            "status", Tournament.STATUS_UPCOMING);

                    batch.commit().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Bốc thăm thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Lỗi khi lưu bốc thăm", Toast.LENGTH_SHORT).show();
                            btnSaveDraw.setEnabled(true);
                            btnSaveDraw.setText("Lưu kết quả bốc thăm");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không thể tải danh sách trận đấu", Toast.LENGTH_SHORT).show();
                    btnSaveDraw.setEnabled(true);
                    btnSaveDraw.setText("Lưu kết quả bốc thăm");
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
