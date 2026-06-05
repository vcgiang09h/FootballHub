package giangvc.cntt.ntu.footballhub.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import giangvc.cntt.ntu.footballhub.Models.Tournament;
import giangvc.cntt.ntu.footballhub.R;

/**
 * TournamentDetailActivity — READ + UPDATE + DELETE giải đấu
 */
public class TournamentDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TOURNAMENT_ID   = "TOURNAMENT_ID";
    public static final String EXTRA_TOURNAMENT_NAME = "TOURNAMENT_NAME";
    public static final String EXTRA_FORMAT          = "FORMAT";
    public static final String EXTRA_STATUS          = "STATUS";
    public static final String EXTRA_MAX_TEAMS       = "MAX_TEAMS";
    public static final String EXTRA_CURRENT_TEAMS   = "CURRENT_TEAMS";
    public static final String EXTRA_START_DATE      = "START_DATE";
    public static final String EXTRA_END_DATE        = "END_DATE";
    public static final String EXTRA_MATCHES_PER_DAY = "MATCHES_PER_DAY";
    public static final String EXTRA_DESCRIPTION     = "DESCRIPTION";

    private static final String COLLECTION = "Tournaments";

    // ── Header views ───────────────────────────────────────────────────────────
    private TextView tvHeaderName;
    private TextView tvHeaderFormat;
    private TextView tvHeaderStatus;

    // ── Edit form ──────────────────────────────────────────────────────────────
    private TextInputEditText etEditName;
    private TextInputEditText etEditStartDate;
    private TextInputEditText etEditEndDate;
    private TextInputEditText etEditMatchesPerDay;
    private TextInputEditText etEditMaxTeams;
    private TextInputEditText etEditCurrentTeams;
    private TextInputEditText etEditDescription;
    private Spinner           spinnerStatus;

    private MaterialButton btnUpdateTournament;
    private MaterialButton btnDeleteTournament;

    // ── State ──────────────────────────────────────────────────────────────────
    private String tournamentId;
    private String selectedStatus;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore db;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();

        // Nhận data từ Intent
        tournamentId        = getIntent().getStringExtra(EXTRA_TOURNAMENT_ID);

        bindViews();
        setupStatusSpinner(Tournament.STATUS_UPCOMING); // Default, will update later
        
        loadTournamentData();

        btnUpdateTournament.setOnClickListener(v -> updateTournament());
        btnDeleteTournament.setOnClickListener(v -> confirmDelete());
    }

    private void loadTournamentData() {
        if (tournamentId == null) return;
        db.collection(COLLECTION).document(tournamentId).get().addOnSuccessListener(doc -> {
            Tournament t = doc.toObject(Tournament.class);
            if (t != null) {
                setupStatusSpinner(t.getStatus());
                populateViews(t.getTournamentName(), t.getFormat(), t.getStatus(), t.getMaxTeams(),
                        t.getCurrentTeams(), t.getStartDate(), t.getEndDate(), t.getMatchesPerDay(), t.getDescription());
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Không thể tải thông tin giải đấu", Toast.LENGTH_SHORT).show();
        });
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvHeaderName         = findViewById(R.id.tvHeaderName);
        tvHeaderFormat       = findViewById(R.id.tvHeaderFormat);
        tvHeaderStatus       = findViewById(R.id.tvHeaderStatus);

        etEditName           = findViewById(R.id.etEditName);
        etEditStartDate      = findViewById(R.id.etEditStartDate);
        etEditEndDate        = findViewById(R.id.etEditEndDate);
        etEditMatchesPerDay  = findViewById(R.id.etEditMatchesPerDay);
        etEditMaxTeams       = findViewById(R.id.etEditMaxTeams);
        etEditCurrentTeams   = findViewById(R.id.etEditCurrentTeams);
        etEditDescription    = findViewById(R.id.etEditDescription);
        spinnerStatus        = findViewById(R.id.spinnerStatus);

        btnUpdateTournament  = findViewById(R.id.btnUpdateTournament);
        btnDeleteTournament  = findViewById(R.id.btnDeleteTournament);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void setupStatusSpinner(String currentStatus) {
        String[] statusLabels = {"Sắp diễn ra", "Đang diễn ra", "Đã kết thúc"};
        String[] statusValues = {
                Tournament.STATUS_UPCOMING,
                Tournament.STATUS_ONGOING,
                Tournament.STATUS_FINISHED
        };

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, statusLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(spinnerAdapter);

        // Chọn trạng thái hiện tại
        for (int i = 0; i < statusValues.length; i++) {
            if (statusValues[i].equals(currentStatus)) {
                spinnerStatus.setSelection(i);
                break;
            }
        }

        selectedStatus = currentStatus != null ? currentStatus : Tournament.STATUS_UPCOMING;

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = statusValues[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void populateViews(String name, String format, String status,
                                int maxTeams, int currentTeams,
                                String startDate, String endDate,
                                int matchesPerDay, String description) {
        // Header
        tvHeaderName.setText(name);
        tvHeaderFormat.setText(Tournament.FORMAT_KNOCKOUT.equals(format)
                ? "Đá loại trực tiếp" : "Đá vòng bảng");

        // Format label trạng thái
        Tournament tmp = new Tournament();
        tmp.setStatus(status);
        tvHeaderStatus.setText(tmp.getStatusLabel());

        // Form
        etEditName.setText(name);
        etEditStartDate.setText(startDate);
        etEditEndDate.setText(endDate);
        if(matchesPerDay > 0) etEditMatchesPerDay.setText(String.valueOf(matchesPerDay));
        etEditMaxTeams.setText(maxTeams >= 999 ? "Không giới hạn" : String.valueOf(maxTeams));
        etEditCurrentTeams.setText(String.valueOf(currentTeams));
        etEditDescription.setText(description);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void updateTournament() {
        String newName       = getText(etEditName);
        String newStartDate  = getText(etEditStartDate);
        String newEndDate    = getText(etEditEndDate);
        String matchStr      = getText(etEditMatchesPerDay);
        String newDesc       = getText(etEditDescription);
        String currentStr    = getText(etEditCurrentTeams);

        if (TextUtils.isEmpty(newName)) {
            etEditName.setError("Vui lòng nhập tên giải đấu");
            etEditName.requestFocus();
            return;
        }

        int currentTeams = 0;
        try {
            if (!TextUtils.isEmpty(currentStr)) currentTeams = Integer.parseInt(currentStr);
        } catch (NumberFormatException ignored) {}
        
        int matchesPerDay = 0;
        try {
            if (!TextUtils.isEmpty(matchStr)) matchesPerDay = Integer.parseInt(matchStr);
        } catch (NumberFormatException ignored) {}

        Map<String, Object> updates = new HashMap<>();
        updates.put("tournamentName", newName);
        updates.put("startDate",      newStartDate);
        updates.put("endDate",        newEndDate);
        updates.put("matchesPerDay",  matchesPerDay);
        updates.put("description",    newDesc);
        updates.put("status",         selectedStatus);
        updates.put("currentTeams",   currentTeams);

        btnUpdateTournament.setEnabled(false);
        btnUpdateTournament.setText("Đang lưu...");

        db.collection(COLLECTION).document(tournamentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật giải \"" + newName + "\"", Toast.LENGTH_SHORT).show();
                    tvHeaderName.setText(newName);
                    Tournament tmp = new Tournament();
                    tmp.setStatus(selectedStatus);
                    tvHeaderStatus.setText(tmp.getStatusLabel());
                    btnUpdateTournament.setEnabled(true);
                    btnUpdateTournament.setText("Lưu thay đổi");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnUpdateTournament.setEnabled(true);
                    btnUpdateTournament.setText("Lưu thay đổi");
                });
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void confirmDelete() {
        String name = getText(etEditName);
        new AlertDialog.Builder(this)
                .setTitle("Xóa giải đấu")
                .setMessage("Bạn có chắc muốn xóa giải \"" + name + "\"?\nHành động này không thể hoàn tác.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection(COLLECTION).document(tournamentId)
                            .delete()
                            .addOnSuccessListener(a -> {
                                // Xóa các trận đấu thuộc giải đấu này
                                db.collection("Matches").whereEqualTo("tournamentId", tournamentId)
                                        .get().addOnSuccessListener(snapshots -> {
                                            com.google.firebase.firestore.WriteBatch batch = db.batch();
                                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                                                batch.delete(doc.getReference());
                                            }
                                            batch.commit();
                                        });

                                Toast.makeText(this, "Đã xóa giải \"" + name + "\" và các lịch thi đấu liên quan", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
