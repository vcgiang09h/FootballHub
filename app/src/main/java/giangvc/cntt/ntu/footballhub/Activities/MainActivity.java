package giangvc.cntt.ntu.footballhub.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import giangvc.cntt.ntu.footballhub.R;

/**
 * MainActivity — Dashboard hiện ra sau khi đăng nhập thành công.
 * Cung cấp điều hướng đến các module của ứng dụng.
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Hiển thị email người dùng đang đăng nhập
        FirebaseUser currentUser = mAuth.getCurrentUser();
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        if (currentUser != null && currentUser.getEmail() != null) {
            tvWelcome.setText("Xin chào, " + currentUser.getEmail() + "!");
        }

        // Card: Team Management → mở TeamManagementActivity
        MaterialCardView cardTeamManagement = findViewById(R.id.cardTeamManagement);
        cardTeamManagement.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, TeamManagementActivity.class))
        );

        // Card: Logout → đăng xuất và quay về LoginActivity
        MaterialCardView cardLogout = findViewById(R.id.cardLogout);
        cardLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
