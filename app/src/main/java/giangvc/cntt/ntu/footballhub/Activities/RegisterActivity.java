package giangvc.cntt.ntu.footballhub.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import giangvc.cntt.ntu.footballhub.R;
import giangvc.cntt.ntu.footballhub.Models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Ánh xạ các View từ XML
        initViews();

        // 3. Xử lý sự kiện nút Đăng ký
        btnRegister.setOnClickListener(v -> handleRegistration());

        // 4. Chuyển sang màn hình Đăng nhập nếu đã có tài khoản
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    private void handleRegistration() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Kiểm tra dữ liệu hợp lệ (Validation)
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải chứa ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tiến hành tạo tài khoản trên Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Lưu thông tin vào Firestore chạy ngầm
                            saveUserToFirestore(firebaseUser.getUid(), email);
                        }
                        // Thông báo và chuyển màn hình ngay sau khi tạo tài khoản thành công
                        Toast.makeText(RegisterActivity.this, "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Exception e = task.getException();
                        Log.e("FIREBASE_AUTH", "Register failed", e);
                        String msg = (e != null) ? e.getMessage() : "Unknown error";
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String email) {
        // Mặc định tài khoản tạo từ app di động là Ban tổ chức (admin) để có quyền quản lý giải
        User newUser = new User(uid, email, "admin");

        db.collection("Users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIRESTORE", "User saved successfully: " + uid);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Failed to save user to Firestore", e);
                });
    }
}