package giangvc.cntt.ntu.footballhub.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import giangvc.cntt.ntu.footballhub.Adapters.TeamAdapter;
import giangvc.cntt.ntu.footballhub.Models.Team;
import giangvc.cntt.ntu.footballhub.R;

/**
 * TeamManagementActivity — v3 (Full CRUD)
 * - Hiển thị danh sách đội bóng real-time từ Firestore
 * - Click item → TeamDetailActivity (Read + Update + Delete)
 * - Long-click item → Dialog xác nhận xóa nhanh
 * - FAB → AddTeamActivity (Create)
 */
public class TeamManagementActivity extends AppCompatActivity {

    private static final String TAG = "TeamMgmt";

    // ── Views ──────────────────────────────────────────────────────────────────
    private RecyclerView                rvTeams;
    private ExtendedFloatingActionButton fabAddTeam;
    private View                        layoutEmpty;
    private TextView                    tvTeamCount;

    // ── Data & Adapter ─────────────────────────────────────────────────────────
    private TeamAdapter teamAdapter;
    private List<Team>  teamList;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore    db;
    private ListenerRegistration listenerRegistration;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_management);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Firebase
        db = FirebaseFirestore.getInstance();

        // Views
        rvTeams     = findViewById(R.id.rvTeams);
        fabAddTeam  = findViewById(R.id.fabAddTeam);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvTeamCount = findViewById(R.id.tvTeamCount);

        // RecyclerView
        teamList    = new ArrayList<>();
        teamAdapter = new TeamAdapter(teamList);
        rvTeams.setLayoutManager(new LinearLayoutManager(this));
        rvTeams.setAdapter(teamAdapter);

        // Thu nhỏ FAB khi scroll xuống, mở rộng khi scroll lên
        rvTeams.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) fabAddTeam.shrink();
                else        fabAddTeam.extend();
            }
        });

        // ── READ: Click item → PlayerManagementActivity ─────────────────────────────
        teamAdapter.setOnTeamClickListener(team -> {
            Intent intent = new Intent(this, PlayerManagementActivity.class);
            intent.putExtra(PlayerManagementActivity.EXTRA_TEAM_ID,   team.getTeamId());
            intent.putExtra(PlayerManagementActivity.EXTRA_TEAM_NAME, team.getTeamName());
            intent.putExtra("TEAM_CLASS", team.getTeamClass());
            intent.putExtra("CAPTAIN_NAME", team.getCaptainName());
            intent.putExtra("CAPTAIN_CLASS", team.getCaptainClass());
            intent.putExtra("CAPTAIN_STUDENT_ID", team.getCaptainStudentId());
            intent.putExtra("CAPTAIN_PHONE", team.getCaptainPhone());
            intent.putExtra("CAPTAIN_EMAIL", team.getCaptainEmail());
            startActivity(intent);
        });

        // ── EDIT/DELETE: Long-click item → TeamDetailActivity ───────────────────
        teamAdapter.setOnTeamLongClickListener(team -> {
            Intent intent = new Intent(this, TeamDetailActivity.class);
            intent.putExtra(TeamDetailActivity.EXTRA_TEAM_ID,    team.getTeamId());
            intent.putExtra(TeamDetailActivity.EXTRA_TEAM_NAME,  team.getTeamName());
            intent.putExtra(TeamDetailActivity.EXTRA_TEAM_CLASS,          team.getTeamClass());
            intent.putExtra(TeamDetailActivity.EXTRA_CAPTAIN_NAME,        team.getCaptainName());
            intent.putExtra(TeamDetailActivity.EXTRA_CAPTAIN_CLASS,       team.getCaptainClass());
            intent.putExtra(TeamDetailActivity.EXTRA_CAPTAIN_STUDENT_ID,  team.getCaptainStudentId());
            intent.putExtra(TeamDetailActivity.EXTRA_CAPTAIN_PHONE,       team.getCaptainPhone());
            intent.putExtra(TeamDetailActivity.EXTRA_CAPTAIN_EMAIL,       team.getCaptainEmail());
            startActivity(intent);
        });

        // ── CREATE: FAB → AddTeamActivity ─────────────────────────────────────
        fabAddTeam.setOnClickListener(v ->
                startActivity(new Intent(this, AddTeamActivity.class))
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenToTeams();
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Real-time listener Firestore */
    private void listenToTeams() {
        listenerRegistration = db.collection("Teams")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage());
                        Toast.makeText(this, "Không tải được danh sách đội.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots == null) return;

                    teamList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Team team = doc.toObject(Team.class);
                        if (team != null) teamList.add(team);
                    }

                    teamAdapter.notifyDataSetChanged();
                    updateUI();
                    Log.d(TAG, "Loaded " + teamList.size() + " teams");
                });
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Dialog xác nhận xóa nhanh khi long-click */
    private void showQuickDeleteDialog(Team team) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa đội bóng")
                .setMessage("Bạn có chắc muốn xóa đội \"" + team.getTeamName() + "\"?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("Teams").document(team.getTeamId())
                            .delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this,
                                            "🗑️ Đã xóa đội \"" + team.getTeamName() + "\"",
                                            Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Cập nhật empty state và đếm số đội */
    private void updateUI() {
        int count = teamList.size();
        if (count == 0) {
            rvTeams.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvTeamCount.setText("Chưa có đội bóng nào");
        } else {
            rvTeams.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvTeamCount.setText(count + " đội đang tham gia");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) listenerRegistration.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
