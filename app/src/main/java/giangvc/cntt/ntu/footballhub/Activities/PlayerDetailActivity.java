package giangvc.cntt.ntu.footballhub.Activities;

import android.os.Bundle;
import android.text.TextUtils;
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

import giangvc.cntt.ntu.footballhub.R;

/**
 * PlayerDetailActivity — READ + UPDATE + DELETE cầu thủ
 *
 * Nhận dữ liệu cầu thủ qua Intent extras, hiển thị form edit,
 * cho phép cập nhật hoặc xóa khỏi Firestore.
 */
public class PlayerDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_ID   = "PLAYER_ID";
    public static final String EXTRA_PLAYER_NAME = "PLAYER_NAME";
    public static final String EXTRA_TEAM_ID     = "TEAM_ID";
    public static final String EXTRA_PLAYER_CLASS = "PLAYER_CLASS";
    public static final String EXTRA_STUDENT_ID   = "STUDENT_ID";
    public static final String EXTRA_PHONE        = "PHONE";
    public static final String EXTRA_EMAIL        = "EMAIL";
    public static final String EXTRA_JERSEY_NUMBER = "JERSEY_NUMBER";
    public static final String EXTRA_POSITION      = "POSITION";

    private static final String COLLECTION = "Players";

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView          tvDetailPlayerInitial;
    private TextView          tvDetailPlayerName;
    private TextView          tvDetailPlayerClass;
    private TextView          tvDetailStudentId;

    private TextInputEditText etEditPlayerName;
    private TextInputEditText etEditPlayerClass;
    private TextInputEditText etEditStudentId;
    private TextInputEditText etEditPhone;
    private TextInputEditText etEditEmail;
    private TextInputEditText etEditJerseyNumber;
    private Spinner           spinnerEditPosition;

    private MaterialButton btnUpdatePlayer;
    private MaterialButton btnDeletePlayer;

    // ── Data ───────────────────────────────────────────────────────────────────
    private String playerId;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore db;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_detail);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();

        // Nhận dữ liệu từ Intent
        playerId         = getIntent().getStringExtra(EXTRA_PLAYER_ID);
        String playerName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
        String position   = getIntent().getStringExtra(EXTRA_POSITION);
        String playerClass= getIntent().getStringExtra(EXTRA_PLAYER_CLASS);
        String studentId  = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        String phone      = getIntent().getStringExtra(EXTRA_PHONE);
        String email      = getIntent().getStringExtra(EXTRA_EMAIL);
        int jerseyNumber  = getIntent().getIntExtra(EXTRA_JERSEY_NUMBER, 0);

        // Bind views
        tvDetailPlayerInitial = findViewById(R.id.tvDetailPlayerInitial);
        tvDetailPlayerName    = findViewById(R.id.tvDetailPlayerName);
        tvDetailPlayerClass   = findViewById(R.id.tvDetailPlayerClass);
        tvDetailStudentId     = findViewById(R.id.tvDetailStudentId);

        etEditPlayerName  = findViewById(R.id.etEditPlayerName);
        etEditPlayerClass = findViewById(R.id.etEditPlayerClass);
        etEditStudentId   = findViewById(R.id.etEditStudentId);
        etEditPhone       = findViewById(R.id.etEditPhone);
        etEditEmail       = findViewById(R.id.etEditEmail);
        etEditJerseyNumber = findViewById(R.id.etEditJerseyNumber);
        spinnerEditPosition = findViewById(R.id.spinnerEditPosition);

        // Setup Spinner
        String[] positions = {"Thủ môn", "Hậu vệ", "Tiền vệ", "Tiền đạo"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, positions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditPosition.setAdapter(adapter);

        btnUpdatePlayer = findViewById(R.id.btnUpdatePlayer);
        btnDeletePlayer = findViewById(R.id.btnDeletePlayer);

        // Populate
        populateViews(playerName, playerClass, studentId, phone, email, jerseyNumber, position, positions);

        btnUpdatePlayer.setOnClickListener(v -> updatePlayer());
        btnDeletePlayer.setOnClickListener(v -> confirmDelete());
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void populateViews(String playerName, String playerClass, String studentId, String phone, String email, int jerseyNumber, String position, String[] positions) {
        // Header
        String initial = (playerName != null && !playerName.isEmpty()) ? String.valueOf(playerName.charAt(0)).toUpperCase() : "?";
        tvDetailPlayerInitial.setText(initial);
        tvDetailPlayerName.setText(playerName);
        tvDetailPlayerClass.setText("Chi đoàn: " + playerClass);
        tvDetailStudentId.setText("MSSV: " + studentId);

        // Form inputs
        etEditPlayerName.setText(playerName);
        etEditPlayerClass.setText(playerClass);
        etEditStudentId.setText(studentId);
        etEditPhone.setText(phone);
        etEditEmail.setText(email);
        if (jerseyNumber > 0) etEditJerseyNumber.setText(String.valueOf(jerseyNumber));
        
        if (position != null) {
            for (int i = 0; i < positions.length; i++) {
                if (positions[i].equals(position)) {
                    spinnerEditPosition.setSelection(i);
                    break;
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** UPDATE — Cập nhật thông tin cầu thủ lên Firestore */
    private void updatePlayer() {
        String newName        = getText(etEditPlayerName);
        String newPlayerClass = getText(etEditPlayerClass);
        String newStudentId   = getText(etEditStudentId);
        String newPhone       = getText(etEditPhone);
        String newEmail       = getText(etEditEmail);
        String jerseyStr      = getText(etEditJerseyNumber);
        int newJerseyNumber   = 0;
        try { if (!jerseyStr.isEmpty()) newJerseyNumber = Integer.parseInt(jerseyStr); } catch (NumberFormatException ignored) {}

        // Validate
        if (TextUtils.isEmpty(newName)) {
            etEditPlayerName.setError("Vui lòng nhập tên cầu thủ");
            etEditPlayerName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(newStudentId)) {
            etEditStudentId.setError("Vui lòng nhập MSSV");
            etEditStudentId.requestFocus();
            return;
        }

        String newPosition    = spinnerEditPosition.getSelectedItem() != null ? spinnerEditPosition.getSelectedItem().toString() : "";

        Map<String, Object> updates = new HashMap<>();
        updates.put("playerName",  newName);
        updates.put("playerClass", newPlayerClass);
        updates.put("studentId",   newStudentId);
        updates.put("phone",       newPhone);
        updates.put("email",       newEmail);
        updates.put("jerseyNumber", newJerseyNumber);
        updates.put("position",    newPosition);

        btnUpdatePlayer.setEnabled(false);
        btnUpdatePlayer.setText("Đang lưu...");

        db.collection(COLLECTION).document(playerId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Đã cập nhật cầu thủ \"" + newName + "\"",
                            Toast.LENGTH_SHORT).show();

                    // Cập nhật header
                    String initial = (!newName.isEmpty()) ? String.valueOf(newName.charAt(0)).toUpperCase() : "?";
                    tvDetailPlayerInitial.setText(initial);
                    tvDetailPlayerName.setText(newName);
                    tvDetailPlayerClass.setText("Chi đoàn: " + newPlayerClass);
                    tvDetailStudentId.setText("MSSV: " + newStudentId);

                    btnUpdatePlayer.setEnabled(true);
                    btnUpdatePlayer.setText("Lưu thay đổi");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnUpdatePlayer.setEnabled(true);
                    btnUpdatePlayer.setText("Lưu thay đổi");
                });
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** DELETE — Hỏi xác nhận rồi xóa cầu thủ khỏi Firestore */
    private void confirmDelete() {
        String name = getText(etEditPlayerName);

        new AlertDialog.Builder(this)
                .setTitle("Xóa cầu thủ")
                .setMessage("Bạn có chắc muốn xóa cầu thủ \"" + name + "\"?\nHành động này không thể hoàn tác.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Xóa", (dialog, which) -> deletePlayer(name))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deletePlayer(String name) {
        btnDeletePlayer.setEnabled(false);
        btnDeletePlayer.setText("Đang xóa...");

        db.collection(COLLECTION).document(playerId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Đã xóa cầu thủ \"" + name + "\"",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnDeletePlayer.setEnabled(true);
                    btnDeletePlayer.setText("Xóa cầu thủ");
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
