package giangvc.cntt.ntu.footballhub.Activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import giangvc.cntt.ntu.footballhub.Models.Player;
import giangvc.cntt.ntu.footballhub.R;

/**
 * AddPlayerActivity — CREATE cầu thủ mới
 *
 * Nhận teamId + teamName từ Intent, lưu Player mới vào Firestore.
 */
public class AddPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM_ID   = "TEAM_ID";
    public static final String EXTRA_TEAM_NAME = "TEAM_NAME";

    private static final String COLLECTION = "Players";

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextInputEditText etPlayerName;
    private TextInputEditText etPlayerClass;
    private TextInputEditText etStudentId;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private MaterialButton    btnSavePlayer;
    private MaterialButton    btnCancel;
    private TextView          tvTeamNameSubtitle;

    // ── Data ───────────────────────────────────────────────────────────────────
    private String teamId;
    private String teamName;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore db;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_player);

        teamId   = getIntent().getStringExtra(EXTRA_TEAM_ID);
        teamName = getIntent().getStringExtra(EXTRA_TEAM_NAME);

        db = FirebaseFirestore.getInstance();

        // Views
        etPlayerName       = findViewById(R.id.etPlayerName);
        etPlayerClass      = findViewById(R.id.etPlayerClass);
        etStudentId        = findViewById(R.id.etStudentId);
        etPhone            = findViewById(R.id.etPhone);
        etEmail            = findViewById(R.id.etEmail);
        btnSavePlayer      = findViewById(R.id.btnSavePlayer);
        btnCancel          = findViewById(R.id.btnCancel);
        tvTeamNameSubtitle = findViewById(R.id.tvTeamNameSubtitle);

        // Hiển thị tên đội trong subtitle
        if (teamName != null) {
            tvTeamNameSubtitle.setText("Đội: " + teamName);
        }

        btnSavePlayer.setOnClickListener(v -> savePlayer());
        btnCancel.setOnClickListener(v -> finish());
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void savePlayer() {
        String name        = getText(etPlayerName);
        String playerClass = getText(etPlayerClass);
        String studentId   = getText(etStudentId);
        String phone       = getText(etPhone);
        String email       = getText(etEmail);

        // Validate
        if (TextUtils.isEmpty(name)) {
            etPlayerName.setError("Vui lòng nhập tên cầu thủ");
            etPlayerName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(studentId)) {
            etStudentId.setError("Vui lòng nhập MSSV");
            etStudentId.requestFocus();
            return;
        }

        // Generate Firestore document ID
        String playerId = db.collection(COLLECTION).document().getId();

        Player newPlayer = new Player(playerId, teamId, name, playerClass, studentId, phone, email);

        btnSavePlayer.setEnabled(false);
        btnSavePlayer.setText("Đang lưu...");

        db.collection(COLLECTION)
                .document(playerId)
                .set(newPlayer)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "✅ Đã thêm cầu thủ \"" + name + "\"",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSavePlayer.setEnabled(true);
                    btnSavePlayer.setText("✔  Lưu Cầu Thủ");
                });
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
