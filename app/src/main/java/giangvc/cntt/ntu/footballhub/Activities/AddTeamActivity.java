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
 * AddTeamActivity — v2
 * Form thêm đội bóng mới với giao diện redesigned.
 * Có nút Hủy và toàn bộ text tiếng Việt.
 */
public class AddTeamActivity extends AppCompatActivity {

    private TextInputEditText etTeamName;
    private TextInputEditText etTeamClass;
    private TextInputEditText etCaptainName;
    private TextInputEditText etCaptainClass;
    private TextInputEditText etCaptainStudentId;
    private TextInputEditText etCaptainPhone;
    private TextInputEditText etCaptainEmail;
    
    private MaterialButton    btnSaveTeam;
    private MaterialButton    btnCancel;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_team);

        db = FirebaseFirestore.getInstance();

        etTeamName         = findViewById(R.id.etTeamName);
        etTeamClass        = findViewById(R.id.etTeamClass);
        etCaptainName      = findViewById(R.id.etCaptainName);
        etCaptainClass     = findViewById(R.id.etCaptainClass);
        etCaptainStudentId = findViewById(R.id.etCaptainStudentId);
        etCaptainPhone     = findViewById(R.id.etCaptainPhone);
        etCaptainEmail     = findViewById(R.id.etCaptainEmail);
        
        btnSaveTeam = findViewById(R.id.btnSaveTeam);
        btnCancel   = findViewById(R.id.btnCancel);

        btnSaveTeam.setOnClickListener(v -> saveTeam());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveTeam() {
        String teamName         = getText(etTeamName);
        String teamClass        = getText(etTeamClass);
        String captainName      = getText(etCaptainName);
        String captainClass     = getText(etCaptainClass);
        String captainStudentId = getText(etCaptainStudentId);
        String captainPhone     = getText(etCaptainPhone);
        String captainEmail     = getText(etCaptainEmail);

        // Validate
        if (TextUtils.isEmpty(teamName)) {
            etTeamName.setError("Vui lòng nhập tên đội");
            etTeamName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(captainName)) {
            etCaptainName.setError("Vui lòng nhập tên đội trưởng");
            etCaptainName.requestFocus();
            return;
        }

        // Generate Firestore document ID
        String teamId = db.collection("Teams").document().getId();

        Team newTeam = new Team(teamId, teamName, teamClass, captainName, captainClass, captainStudentId, captainPhone, captainEmail);

        btnSaveTeam.setEnabled(false);
        btnSaveTeam.setText("Đang lưu...");

        db.collection("Teams")
                .document(teamId)
                .set(newTeam)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã thêm đội \"" + teamName + "\"", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSaveTeam.setEnabled(true);
                    btnSaveTeam.setText("Lưu Đội Bóng");
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
}
