package giangvc.cntt.ntu.footballhub.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import giangvc.cntt.ntu.footballhub.R;

/**
 * MainActivity — Dashboard chính sau khi đăng nhập.
 * Chỉ xử lý giao diện, chức năng sẽ bổ sung từng ngày sau.
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    // Header views
    private TextView tvWelcome;

    // Menu cards
    private MaterialCardView cardTeamManagement;
    private MaterialCardView cardTournament;
    private MaterialCardView cardMatchSchedule;
    private MaterialCardView cardStats;
    private MaterialCardView cardDrawCeremony;
    private MaterialCardView cardLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        bindViews();
        setupWelcomeText();
        setupCardListeners();
    }

    // ──────────────────────────────────────────────

    private void bindViews() {
        tvWelcome          = findViewById(R.id.tvWelcome);
        cardTeamManagement = findViewById(R.id.cardTeamManagement);
        cardTournament     = findViewById(R.id.cardTournament);
        cardMatchSchedule  = findViewById(R.id.cardMatchSchedule);
        cardStats          = findViewById(R.id.cardStats);
        cardDrawCeremony   = findViewById(R.id.cardDrawCeremony);
        cardLogout         = findViewById(R.id.cardLogout);
    }

    private void setupWelcomeText() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String email = user.getEmail();
            // Lấy phần trước @ để hiển thị ngắn gọn
            String name = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            tvWelcome.setText("Xin chào, " + name + "!");
        } else {
            tvWelcome.setText("Xin chào!");
        }
    }

    private void setupCardListeners() {
        // Quản lý đội bóng (đã có)
        cardTeamManagement.setOnClickListener(v ->
                startActivity(new Intent(this, TeamManagementActivity.class))
        );

        // Quản lý Giải đấu
        cardTournament.setOnClickListener(v ->
                startActivity(new Intent(this, TournamentManagementActivity.class))
        );

        // Lịch thi đấu & Kết quả
        cardMatchSchedule.setOnClickListener(v ->
                startActivity(new Intent(this, MatchScheduleActivity.class))
        );

        // Thống kê
        cardStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatisticsActivity.class))
        );

        // Bốc thăm
        cardDrawCeremony.setOnClickListener(v ->
                startActivity(new Intent(this, DrawCeremonyActivity.class))
        );

        // Đăng xuất
        cardLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
