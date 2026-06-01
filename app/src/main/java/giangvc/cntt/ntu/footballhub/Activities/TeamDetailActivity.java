package giangvc.cntt.ntu.footballhub.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

import giangvc.cntt.ntu.footballhub.R;

/**
 * TeamDetailActivity — Full CRUD (Read + Update + Delete)
 *
 * Nhận teamId qua Intent extra "TEAM_ID", load dữ liệu từ Firestore,
 * cho phép chỉnh sửa và xóa đội bóng.
 */
public class TeamDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM_ID     = "TEAM_ID";
    public static final String EXTRA_TEAM_NAME   = "TEAM_NAME";
    public static final String EXTRA_TEAM_CLASS           = "TEAM_CLASS";
    public static final String EXTRA_CAPTAIN_NAME         = "CAPTAIN_NAME";
    public static final String EXTRA_CAPTAIN_CLASS        = "CAPTAIN_CLASS";
    public static final String EXTRA_CAPTAIN_STUDENT_ID   = "CAPTAIN_STUDENT_ID";
    public static final String EXTRA_CAPTAIN_PHONE        = "CAPTAIN_PHONE";
    public static final String EXTRA_CAPTAIN_EMAIL        = "CAPTAIN_EMAIL";

    private static final String TAG = "TeamDetail";

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView          tvDetailInitial;
    private TextView          tvDetailTeamName;
    private TextView          tvDetailCoachName;

    private TextInputEditText etEditTeamName;
    private TextInputEditText etEditTeamClass;
    private TextInputEditText etEditCaptainName;
    private TextInputEditText etEditCaptainClass;
    private TextInputEditText etEditCaptainStudentId;
    private TextInputEditText etEditCaptainPhone;
    private TextInputEditText etEditCaptainEmail;

    private MaterialButton btnUpdateTeam;
    private MaterialButton btnDeleteTeam;
    private MaterialButton btnViewPlayers;

    // ── Data ───────────────────────────────────────────────────────────────────
    private String teamId;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore db;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_detail);

        // Toolbar với nút Back
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();

        // Nhận dữ liệu từ Intent (truyền từ TeamManagementActivity)
        teamId = getIntent().getStringExtra(EXTRA_TEAM_ID);
        String teamName  = getIntent().getStringExtra(EXTRA_TEAM_NAME);
        String teamClass        = getIntent().getStringExtra(EXTRA_TEAM_CLASS);
        String captainName      = getIntent().getStringExtra(EXTRA_CAPTAIN_NAME);
        String captainClass     = getIntent().getStringExtra(EXTRA_CAPTAIN_CLASS);
        String captainStudentId = getIntent().getStringExtra(EXTRA_CAPTAIN_STUDENT_ID);
        String captainPhone     = getIntent().getStringExtra(EXTRA_CAPTAIN_PHONE);
        String captainEmail     = getIntent().getStringExtra(EXTRA_CAPTAIN_EMAIL);

        // Bind views
        tvDetailInitial    = findViewById(R.id.tvDetailInitial);
        tvDetailTeamName   = findViewById(R.id.tvDetailTeamName);
        tvDetailCoachName  = findViewById(R.id.tvDetailCoachName); // keep name but change text

        etEditTeamName          = findViewById(R.id.etEditTeamName);
        etEditTeamClass         = findViewById(R.id.etEditTeamClass);
        etEditCaptainName       = findViewById(R.id.etEditCaptainName);
        etEditCaptainClass      = findViewById(R.id.etEditCaptainClass);
        etEditCaptainStudentId  = findViewById(R.id.etEditCaptainStudentId);
        etEditCaptainPhone      = findViewById(R.id.etEditCaptainPhone);
        etEditCaptainEmail      = findViewById(R.id.etEditCaptainEmail);

        btnUpdateTeam  = findViewById(R.id.btnUpdateTeam);
        btnDeleteTeam  = findViewById(R.id.btnDeleteTeam);
        btnViewPlayers = findViewById(R.id.btnViewPlayers);

        // Populate giao diện
        populateViews(teamName, teamClass, captainName, captainClass, captainStudentId, captainPhone, captainEmail);

        // Button listeners
        btnUpdateTeam.setOnClickListener(v -> updateTeam());
        btnDeleteTeam.setOnClickListener(v -> confirmDelete());

        // Nút xem cầu thủ → PlayerManagementActivity
        final String currentTeamName = teamName;
        btnViewPlayers.setOnClickListener(v -> {
            String name = etEditTeamName.getText() != null
                    ? etEditTeamName.getText().toString().trim() : currentTeamName;
            Intent intent = new Intent(this, PlayerManagementActivity.class);
            intent.putExtra(PlayerManagementActivity.EXTRA_TEAM_ID,   teamId);
            intent.putExtra(PlayerManagementActivity.EXTRA_TEAM_NAME, name);
            startActivity(intent);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Điền dữ liệu lên header và form */
    private void populateViews(String teamName, String teamClass, String captainName, String captainClass, String captainStudentId, String captainPhone, String captainEmail) {
        // Header
        String initial = (teamName != null && !teamName.isEmpty())
                ? String.valueOf(teamName.charAt(0)).toUpperCase() : "?";
        tvDetailInitial.setText(initial);
        tvDetailTeamName.setText(teamName);
        tvDetailCoachName.setText("Đội trưởng: " + captainName);

        // Form inputs
        etEditTeamName.setText(teamName);
        etEditTeamClass.setText(teamClass);
        etEditCaptainName.setText(captainName);
        etEditCaptainClass.setText(captainClass);
        etEditCaptainStudentId.setText(captainStudentId);
        etEditCaptainPhone.setText(captainPhone);
        etEditCaptainEmail.setText(captainEmail);
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** UPDATE — Cập nhật thông tin đội lên Firestore */
    private void updateTeam() {
        String newName             = etEditTeamName.getText()  != null ? etEditTeamName.getText().toString().trim()  : "";
        String newTeamClass        = etEditTeamClass.getText() != null ? etEditTeamClass.getText().toString().trim() : "";
        String newCaptainName      = etEditCaptainName.getText() != null ? etEditCaptainName.getText().toString().trim() : "";
        String newCaptainClass     = etEditCaptainClass.getText() != null ? etEditCaptainClass.getText().toString().trim() : "";
        String newCaptainStudentId = etEditCaptainStudentId.getText() != null ? etEditCaptainStudentId.getText().toString().trim() : "";
        String newCaptainPhone     = etEditCaptainPhone.getText() != null ? etEditCaptainPhone.getText().toString().trim() : "";
        String newCaptainEmail     = etEditCaptainEmail.getText() != null ? etEditCaptainEmail.getText().toString().trim() : "";

        // Validate
        if (TextUtils.isEmpty(newName)) {
            etEditTeamName.setError("Vui lòng nhập tên đội");
            etEditTeamName.requestFocus();
            return;
        }

        // Build update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("teamName",         newName);
        updates.put("teamClass",        newTeamClass);
        updates.put("captainName",      newCaptainName);
        updates.put("captainClass",     newCaptainClass);
        updates.put("captainStudentId", newCaptainStudentId);
        updates.put("captainPhone",     newCaptainPhone);
        updates.put("captainEmail",     newCaptainEmail);

        btnUpdateTeam.setEnabled(false);
        btnUpdateTeam.setText("Đang lưu...");

        db.collection("Teams").document(teamId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Đã cập nhật đội \"" + newName + "\"", Toast.LENGTH_SHORT).show();

                    // Cập nhật lại header
                    String initial = (!newName.isEmpty()) ? String.valueOf(newName.charAt(0)).toUpperCase() : "?";
                    tvDetailInitial.setText(initial);
                    tvDetailTeamName.setText(newName);
                    tvDetailCoachName.setText("Đội trưởng: " + newCaptainName);

                    btnUpdateTeam.setEnabled(true);
                    btnUpdateTeam.setText("💾  Lưu thay đổi");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnUpdateTeam.setEnabled(true);
                    btnUpdateTeam.setText("💾  Lưu thay đổi");
                });
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** DELETE — Hỏi xác nhận rồi xóa khỏi Firestore */
    private void confirmDelete() {
        String teamName = etEditTeamName.getText() != null
                ? etEditTeamName.getText().toString().trim() : "đội này";

        new AlertDialog.Builder(this)
                .setTitle("Xóa đội bóng")
                .setMessage("Bạn có chắc muốn xóa đội \"" + teamName + "\"?\nHành động này không thể hoàn tác.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Xóa", (dialog, which) -> deleteTeam(teamName))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteTeam(String teamName) {
        btnDeleteTeam.setEnabled(false);
        btnDeleteTeam.setText("Đang xóa...");

        db.collection("Teams").document(teamId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "🗑️ Đã xóa đội \"" + teamName + "\"", Toast.LENGTH_SHORT).show();
                    finish(); // Quay lại danh sách
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnDeleteTeam.setEnabled(true);
                    btnDeleteTeam.setText("🗑️  Xóa đội bóng");
                });
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
