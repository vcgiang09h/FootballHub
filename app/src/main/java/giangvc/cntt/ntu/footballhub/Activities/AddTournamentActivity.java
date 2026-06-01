package giangvc.cntt.ntu.footballhub.Activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import giangvc.cntt.ntu.footballhub.Models.Tournament;
import giangvc.cntt.ntu.footballhub.R;

/**
 * AddTournamentActivity — CREATE giải đấu mới
 *
 * Cho phép chọn hình thức:
 *  - KNOCKOUT (đá loại trực tiếp): SlideBar chọn số đội 2–24
 *  - ROUND_ROBIN (đá vòng bảng): không giới hạn cứng
 */
public class AddTournamentActivity extends AppCompatActivity {

    private static final String COLLECTION = "Tournaments";

    // ── Format selection views ─────────────────────────────────────────────────
    private View         cardKnockout;
    private View         cardRoundRobin;
    private RadioButton  rbKnockout;
    private RadioButton  rbRoundRobin;
    private View         cardMaxTeams;    // Card SeekBar (ẩn khi Round Robin)
    private SeekBar      seekBarTeams;
    private TextView     tvMaxTeamsValue;
    private MaterialButton btnSelectTeams;
    private TextView     tvSelectedTeamsInfo;

    // ── Form inputs ────────────────────────────────────────────────────────────
    private TextInputEditText etTournamentName;
    private TextInputEditText etStartDate;
    private TextInputEditText etEndDate;
    private TextInputEditText etMatchesPerDay;
    private TextInputEditText etDescription;

    // ── Buttons ────────────────────────────────────────────────────────────────
    private MaterialButton btnSaveTournament;
    private MaterialButton btnCancel;

    private String selectedFormat = Tournament.FORMAT_KNOCKOUT;
    private int    maxTeams       = 8; // SeekBar min=2, progress=6 → value=2+6=8

    // ── Team Selection ─────────────────────────────────────────────────────────
    private java.util.List<giangvc.cntt.ntu.footballhub.Models.Team> allTeamsList = new java.util.ArrayList<>();
    private String[] allTeamNamesArray;
    private boolean[] selectedTeamsArray;
    private java.util.List<String> selectedTeamIds = new java.util.ArrayList<>();
    private java.util.List<String> selectedTeamNames = new java.util.ArrayList<>();

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore db;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tournament);

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupFormatSelector();
        setupSeekBar();
        
        loadTeamsFromFirestore();

        btnSelectTeams.setOnClickListener(v -> showTeamSelectionDialog());
        btnSaveTournament.setOnClickListener(v -> saveTournament());
        btnCancel.setOnClickListener(v -> finish());
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        cardKnockout      = findViewById(R.id.cardKnockout);
        cardRoundRobin    = findViewById(R.id.cardRoundRobin);
        rbKnockout        = findViewById(R.id.rbKnockout);
        rbRoundRobin      = findViewById(R.id.rbRoundRobin);
        cardMaxTeams      = findViewById(R.id.cardMaxTeams);
        seekBarTeams      = findViewById(R.id.seekBarTeams);
        tvMaxTeamsValue   = findViewById(R.id.tvMaxTeamsValue);
        btnSelectTeams    = findViewById(R.id.btnSelectTeams);
        tvSelectedTeamsInfo = findViewById(R.id.tvSelectedTeamsInfo);

        etTournamentName  = findViewById(R.id.etTournamentName);
        etStartDate       = findViewById(R.id.etStartDate);
        etEndDate         = findViewById(R.id.etEndDate);
        etMatchesPerDay   = findViewById(R.id.etMatchesPerDay);
        etDescription     = findViewById(R.id.etDescription);

        btnSaveTournament = findViewById(R.id.btnSaveTournament);
        btnCancel         = findViewById(R.id.btnCancel);
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Xử lý chọn hình thức thi đấu */
    private void setupFormatSelector() {
        cardKnockout.setOnClickListener(v -> selectFormat(Tournament.FORMAT_KNOCKOUT));
        cardRoundRobin.setOnClickListener(v -> 
            Toast.makeText(this, "Hình thức Đá vòng bảng đang được phát triển!", Toast.LENGTH_SHORT).show()
        );

        // Mặc định: Knockout
        selectFormat(Tournament.FORMAT_KNOCKOUT);
    }

    private void selectFormat(String format) {
        selectedFormat = format;
        boolean isKnockout = Tournament.FORMAT_KNOCKOUT.equals(format);

        // Cập nhật radio buttons
        rbKnockout.setChecked(isKnockout);
        rbRoundRobin.setChecked(false); // Luôn false

        // Cập nhật background của card
        cardKnockout.setBackgroundResource(
                isKnockout ? R.drawable.bg_format_selected : R.drawable.bg_format_unselected);
        // Không cập nhật background của cardRoundRobin vì nó luôn ở trạng thái disabled (màu xám trong XML)

        // Ẩn/hiện SeekBar số đội
        cardMaxTeams.setVisibility(isKnockout ? View.VISIBLE : View.GONE);

        // Nếu Round Robin không cần giới hạn cứng → dùng 999
        if (!isKnockout) maxTeams = 999;
        else              maxTeams = seekBarTeams.getProgress() + 4;
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** SeekBar: range 4–24 đội */
    private void setupSeekBar() {
        // max=20, progress+4 → range [4, 24]
        seekBarTeams.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                maxTeams = progress + 4;
                tvMaxTeamsValue.setText(String.valueOf(maxTeams));
                
                // Reset selection if maxTeams changes
                if (selectedTeamsArray != null) {
                    java.util.Arrays.fill(selectedTeamsArray, false);
                    selectedTeamIds.clear();
                    selectedTeamNames.clear();
                    updateSelectedTeamsUI();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        // Khởi tạo giá trị ban đầu
        maxTeams = seekBarTeams.getProgress() + 4;
        tvMaxTeamsValue.setText(String.valueOf(maxTeams));
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void loadTeamsFromFirestore() {
        db.collection("Teams").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                allTeamsList.clear();
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : task.getResult()) {
                    giangvc.cntt.ntu.footballhub.Models.Team team = doc.toObject(giangvc.cntt.ntu.footballhub.Models.Team.class);
                    team.setTeamId(doc.getId()); // ensure ID is set if not mapped
                    allTeamsList.add(team);
                }
                
                allTeamNamesArray = new String[allTeamsList.size()];
                for (int i = 0; i < allTeamsList.size(); i++) {
                    String name = allTeamsList.get(i).getTeamName();
                    allTeamNamesArray[i] = (name != null) ? name : "Unknown";
                }
                selectedTeamsArray = new boolean[allTeamsList.size()];
            } else {
                Toast.makeText(this, "Không thể tải danh sách đội bóng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTeamSelectionDialog() {
        if (allTeamNamesArray == null || allTeamNamesArray.length == 0) {
            Toast.makeText(this, "Chưa có đội bóng nào trên hệ thống!", Toast.LENGTH_SHORT).show();
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Chọn đội tham gia (" + maxTeams + " đội)");

        builder.setMultiChoiceItems(allTeamNamesArray, selectedTeamsArray, (dialog, which, isChecked) -> {
            selectedTeamsArray[which] = isChecked;
        });

        builder.setPositiveButton("Xong", (dialog, which) -> {
            selectedTeamIds.clear();
            selectedTeamNames.clear();
            for (int i = 0; i < selectedTeamsArray.length; i++) {
                if (selectedTeamsArray[i]) {
                    selectedTeamIds.add(allTeamsList.get(i).getTeamId());
                    selectedTeamNames.add(allTeamNamesArray[i]);
                }
            }
            updateSelectedTeamsUI();
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    
    private void updateSelectedTeamsUI() {
        int count = selectedTeamIds.size();
        tvSelectedTeamsInfo.setText("Đã chọn: " + count + "/" + maxTeams + " đội");
        if (count == maxTeams) {
            tvSelectedTeamsInfo.setTextColor(android.graphics.Color.parseColor("#43A047")); // Green
        } else {
            tvSelectedTeamsInfo.setTextColor(android.graphics.Color.parseColor("#E65100")); // Orange
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Lưu giải đấu lên Firestore */
    private void saveTournament() {
        String name      = getText(etTournamentName);
        String startDate = getText(etStartDate);
        String endDate   = getText(etEndDate);
        String matchStr  = getText(etMatchesPerDay);
        String desc      = getText(etDescription);

        int matchesPerDay = 0;
        if (!TextUtils.isEmpty(matchStr)) {
            try {
                matchesPerDay = Integer.parseInt(matchStr);
            } catch (NumberFormatException ignored) {}
        }

        // Validate
        if (TextUtils.isEmpty(name)) {
            etTournamentName.setError("Vui lòng nhập tên giải đấu");
            etTournamentName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(startDate)) {
            etStartDate.setError("Vui lòng nhập ngày bắt đầu");
            etStartDate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(endDate)) {
            etEndDate.setError("Vui lòng nhập ngày kết thúc");
            etEndDate.requestFocus();
            return;
        }

        // Nếu Knockout mà số đội < 4
        if (Tournament.FORMAT_KNOCKOUT.equals(selectedFormat)) {
            if (maxTeams < 4) {
                Toast.makeText(this, "Số đội phải ít nhất là 4", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedTeamIds.size() != maxTeams) {
                Toast.makeText(this, "Vui lòng chọn đúng " + maxTeams + " đội bóng", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String tournamentId = db.collection(COLLECTION).document().getId();

        Tournament t = new Tournament(
                tournamentId, name, selectedFormat,
                Tournament.STATUS_PENDING_DRAW,
                maxTeams, selectedTeamIds.size(), startDate, endDate, matchesPerDay, desc,
                selectedTeamIds, selectedTeamNames
        );

        btnSaveTournament.setEnabled(false);
        btnSaveTournament.setText("Đang tạo...");

        db.collection(COLLECTION).document(tournamentId)
                .set(t)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "✅ Đã tạo giải \"" + name + "\"",
                            Toast.LENGTH_SHORT).show();
                    
                    if (Tournament.FORMAT_KNOCKOUT.equals(selectedFormat)) {
                        generateBracketAndFinish(t);
                    } else {
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSaveTournament.setEnabled(true);
                    btnSaveTournament.setText("✔  Tạo Giải Đấu");
                });
    }

    // ──────────────────────────────────────────────────────────────────────────

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void generateBracketAndFinish(Tournament t) {
        btnSaveTournament.setText("Đang xếp lịch...");

        int N = t.getMaxTeams();
        int P = (N <= 4) ? 4 : (N <= 8) ? 8 : 16;
        
        class TeamRef {
            String id; String name;
            TeamRef(String id, String name) { this.id = id; this.name = name; }
        }

        java.util.List<TeamRef> teams = new java.util.ArrayList<>();
        for (int i = 1; i <= N; i++) {
            teams.add(new TeamRef("SEED_" + i, "Đội " + i));
        }
        // No shuffle, teams are placed deterministically as Seed 1 to N

        int byes = P - N;
        int totalPairs = P / 2;
        
        java.util.Queue<TeamRef> queue = new java.util.LinkedList<>(teams);
        java.util.List<TeamRef> seeds = new java.util.ArrayList<>();
        java.util.List<TeamRef> winners = new java.util.ArrayList<>();
        
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        int matchCounter = 1;
        
        // Round 1 (Play-offs / Quarters)
        String r1Name = (P == 16) ? "Sơ loại" : (P == 8) ? "Tứ kết" : "Bán kết";
        for (int i = 0; i < totalPairs; i++) {
            if (i < byes) {
                seeds.add(queue.poll());
            } else {
                TeamRef t1 = queue.poll();
                TeamRef t2 = queue.poll();
                String roundTitle = r1Name + " (Trận " + matchCounter + ")";
                if (P == 4) roundTitle = "Bán kết " + (i - byes + 1);
                createMatch(batch, t, roundTitle, t1.id, t1.name, t2.id, t2.name, matchCounter);
                winners.add(new TeamRef("", "Thắng Trận " + matchCounter));
                matchCounter++;
            }
        }
        
        java.util.List<TeamRef> advancing = new java.util.ArrayList<>();
        advancing.addAll(seeds);
        advancing.addAll(winners);
        
        java.util.List<TeamRef> currentRound = new java.util.ArrayList<>();
        int left = 0, right = advancing.size() - 1;
        while(left < right) {
            currentRound.add(advancing.get(left));
            currentRound.add(advancing.get(right));
            left++;
            right--;
        }
        
        // Subsequent rounds
        while (currentRound.size() > 2) {
            int cSize = currentRound.size();
            String rName = (cSize == 8) ? "Tứ kết" : (cSize == 4) ? "Bán kết" : "Vòng " + cSize;
            
            java.util.List<TeamRef> nextRound = new java.util.ArrayList<>();
            for (int i = 0; i < cSize; i += 2) {
                TeamRef t1 = currentRound.get(i);
                TeamRef t2 = currentRound.get(i+1);
                String roundTitle = rName + " (Trận " + matchCounter + ")";
                if (cSize == 4) roundTitle = "Bán kết " + (i/2 + 1) + " (Trận " + matchCounter + ")";
                createMatch(batch, t, roundTitle, t1.id, t1.name, t2.id, t2.name, matchCounter);
                nextRound.add(new TeamRef("", "Thắng Trận " + matchCounter));
                matchCounter++;
            }
            currentRound = nextRound;
        }
        
        // Finals
        TeamRef f1 = currentRound.get(0);
        TeamRef f2 = currentRound.get(1);
        
        String m3T1 = f1.name.replace("Thắng", "Thua");
        String m3T2 = f2.name.replace("Thắng", "Thua");
        
        createMatch(batch, t, "Tranh hạng 3", "", m3T1, "", m3T2, matchCounter++);
        createMatch(batch, t, "Chung kết", f1.id, f1.name, f2.id, f2.name, matchCounter++);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Đã tự động bốc thăm và lên lịch thi đấu!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Lỗi khi tạo lịch thi đấu", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }

    private void createMatch(com.google.firebase.firestore.WriteBatch batch, Tournament t, String round,
                             String team1Id, String team1Name, String team2Id, String team2Name, long matchOrder) {
        String matchId = db.collection("Matches").document().getId();
        giangvc.cntt.ntu.footballhub.Models.Match match = new giangvc.cntt.ntu.footballhub.Models.Match(
                matchId, t.getTournamentId(), t.getTournamentName(),
                team1Id, team1Name, team2Id, team2Name,
                t.getStartDate(), "", "", round, giangvc.cntt.ntu.footballhub.Models.Match.STATUS_UPCOMING, 0, 0,
                "", new java.util.ArrayList<>(), matchOrder
        );
        batch.set(db.collection("Matches").document(matchId), match);
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
