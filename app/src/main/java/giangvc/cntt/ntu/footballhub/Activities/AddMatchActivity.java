package giangvc.cntt.ntu.footballhub.Activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import giangvc.cntt.ntu.footballhub.Models.Match;
import giangvc.cntt.ntu.footballhub.R;

public class AddMatchActivity extends AppCompatActivity {

    private static final String MATCHES_COLLECTION = "Matches";
    private static final String TOURNAMENTS_COLLECTION = "Tournaments";
    private static final String TEAMS_COLLECTION = "Teams";

    private Spinner spinnerTournament;
    private Spinner spinnerTeam1;
    private Spinner spinnerTeam2;
    private TextInputEditText etRound;
    private TextInputEditText etDate;
    private TextInputEditText etTime;
    private TextInputEditText etLocation;
    private MaterialButton btnSaveMatch;

    private FirebaseFirestore db;

    // Danh sách lưu trữ object thực tế từ Firestore
    private List<TournamentItem> tournamentList = new ArrayList<>();
    private List<TeamItem> teamList = new ArrayList<>();
    private String preSelectedTournamentId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_match);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();

        spinnerTournament = findViewById(R.id.spinnerTournament);
        spinnerTeam1 = findViewById(R.id.spinnerTeam1);
        spinnerTeam2 = findViewById(R.id.spinnerTeam2);
        etRound = findViewById(R.id.etRound);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLocation = findViewById(R.id.etLocation);
        btnSaveMatch = findViewById(R.id.btnSaveMatch);

        preSelectedTournamentId = getIntent().getStringExtra("TOURNAMENT_ID");

        loadTournaments();
        loadTeams();

        btnSaveMatch.setOnClickListener(v -> saveMatch());
    }

    private void loadTournaments() {
        db.collection(TOURNAMENTS_COLLECTION).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                tournamentList.clear();
                List<String> tournamentNames = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String id = doc.getId();
                    String name = doc.getString("tournamentName");
                    tournamentList.add(new TournamentItem(id, name));
                    tournamentNames.add(name != null ? name : "Unknown");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tournamentNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTournament.setAdapter(adapter);

                if (preSelectedTournamentId != null) {
                    for (int i = 0; i < tournamentList.size(); i++) {
                        if (tournamentList.get(i).id.equals(preSelectedTournamentId)) {
                            spinnerTournament.setSelection(i);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void loadTeams() {
        db.collection(TEAMS_COLLECTION).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                teamList.clear();
                List<String> teamNames = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String id = doc.getId();
                    String name = doc.getString("teamName");
                    teamList.add(new TeamItem(id, name));
                    teamNames.add(name != null ? name : "Unknown");
                }

                ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamNames);
                adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTeam1.setAdapter(adapter1);

                ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamNames);
                adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTeam2.setAdapter(adapter2);
            }
        });
    }

    private void saveMatch() {
        if (tournamentList.isEmpty() || teamList.isEmpty()) {
            Toast.makeText(this, "Vui lòng chờ tải dữ liệu Giải đấu/Đội bóng", Toast.LENGTH_SHORT).show();
            return;
        }

        int tIndex = spinnerTournament.getSelectedItemPosition();
        int t1Index = spinnerTeam1.getSelectedItemPosition();
        int t2Index = spinnerTeam2.getSelectedItemPosition();

        if (t1Index == t2Index) {
            Toast.makeText(this, "Vui lòng chọn 2 đội khác nhau", Toast.LENGTH_SHORT).show();
            return;
        }

        String round = getText(etRound);
        String date = getText(etDate);
        String time = getText(etTime);
        String location = getText(etLocation);

        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            Toast.makeText(this, "Vui lòng nhập Ngày và Giờ", Toast.LENGTH_SHORT).show();
            return;
        }

        TournamentItem selTournament = tournamentList.get(tIndex);
        TeamItem selTeam1 = teamList.get(t1Index);
        TeamItem selTeam2 = teamList.get(t2Index);

        String matchId = db.collection(MATCHES_COLLECTION).document().getId();

        Match match = new Match(
                matchId,
                selTournament.id,
                selTournament.name,
                selTeam1.id,
                selTeam1.name,
                selTeam2.id,
                selTeam2.name,
                date,
                time,
                location,
                round,
                Match.STATUS_UPCOMING,
                0,
                0
        );

        btnSaveMatch.setEnabled(false);
        db.collection(MATCHES_COLLECTION).document(matchId).set(match)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tạo trận đấu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveMatch.setEnabled(true);
                });
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Lớp model nội bộ nhỏ để map ID <-> Name
    static class TournamentItem {
        String id; String name;
        TournamentItem(String id, String name) { this.id = id; this.name = name; }
    }
    static class TeamItem {
        String id; String name;
        TeamItem(String id, String name) { this.id = id; this.name = name; }
    }
}
