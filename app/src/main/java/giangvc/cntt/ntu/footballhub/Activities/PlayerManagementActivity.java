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

import giangvc.cntt.ntu.footballhub.Adapters.PlayerAdapter;
import giangvc.cntt.ntu.footballhub.Models.Player;
import giangvc.cntt.ntu.footballhub.R;

/**
 * PlayerManagementActivity — Danh sách cầu thủ theo đội (Full CRUD)
 *
 * Nhận teamId + teamName từ Intent.
 * - READ   : Real-time listener Firestore, lọc theo teamId
 * - CREATE : FAB → AddPlayerActivity
 * - UPDATE : Click item → PlayerDetailActivity
 * - DELETE : Long-click item → Dialog xác nhận
 */
public class PlayerManagementActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM_ID   = "TEAM_ID";
    public static final String EXTRA_TEAM_NAME = "TEAM_NAME";

    private static final String TAG = "PlayerMgmt";
    private static final String COLLECTION = "Players";

    // ── Views ──────────────────────────────────────────────────────────────────
    private RecyclerView                rvPlayers;
    private ExtendedFloatingActionButton fabAddPlayer;
    private View                        layoutEmpty;
    private TextView                    tvPlayerCount;
    private TextView                    tvTeamNameHeader;

    // ── Data ───────────────────────────────────────────────────────────────────
    private PlayerAdapter playerAdapter;
    private List<Player>  playerList;
    private String        teamId;
    private String        teamName;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore    db;
    private ListenerRegistration listenerRegistration;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_management);

        // Nhận data từ Intent
        teamId   = getIntent().getStringExtra(EXTRA_TEAM_ID);
        teamName = getIntent().getStringExtra(EXTRA_TEAM_NAME);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();

        // Views
        rvPlayers        = findViewById(R.id.rvPlayers);
        fabAddPlayer     = findViewById(R.id.fabAddPlayer);
        layoutEmpty      = findViewById(R.id.layoutEmpty);
        tvPlayerCount    = findViewById(R.id.tvPlayerCount);
        tvTeamNameHeader = findViewById(R.id.tvTeamNameHeader);

        // Set team name trong header
        if (teamName != null) tvTeamNameHeader.setText(teamName);

        // RecyclerView
        playerList    = new ArrayList<>();
        playerAdapter = new PlayerAdapter(playerList);
        rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        rvPlayers.setAdapter(playerAdapter);

        // Thu nhỏ FAB khi scroll xuống
        rvPlayers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) fabAddPlayer.shrink();
                else        fabAddPlayer.extend();
            }
        });

        // Click → PlayerDetailActivity (Update/Delete)
        playerAdapter.setOnPlayerClickListener(player -> {
            Intent intent = new Intent(this, PlayerDetailActivity.class);
            intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_ID,   player.getPlayerId());
            intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_NAME, player.getPlayerName());
            intent.putExtra(PlayerDetailActivity.EXTRA_TEAM_ID,     player.getTeamId());
            intent.putExtra(PlayerDetailActivity.EXTRA_PLAYER_CLASS, player.getPlayerClass());
            intent.putExtra(PlayerDetailActivity.EXTRA_STUDENT_ID,  player.getStudentId());
            intent.putExtra(PlayerDetailActivity.EXTRA_PHONE,       player.getPhone());
            intent.putExtra(PlayerDetailActivity.EXTRA_EMAIL,       player.getEmail());
            startActivity(intent);
        });

        // Long-click → Dialog xóa nhanh
        playerAdapter.setOnPlayerLongClickListener(this::showQuickDeleteDialog);

        // FAB → AddPlayerActivity (Create)
        fabAddPlayer.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPlayerActivity.class);
            intent.putExtra(AddPlayerActivity.EXTRA_TEAM_ID,   teamId);
            intent.putExtra(AddPlayerActivity.EXTRA_TEAM_NAME, teamName);
            startActivity(intent);
        });

        listenToPlayers();
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Real-time listener — lọc cầu thủ theo teamId */
    private void listenToPlayers() {
        if (teamId == null) return;

        listenerRegistration = db.collection(COLLECTION)
                .whereEqualTo("teamId", teamId)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage());
                        Toast.makeText(this, "Không tải được danh sách cầu thủ.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots == null) return;

                    playerList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Player player = doc.toObject(Player.class);
                        if (player != null) playerList.add(player);
                    }

                    // Sắp xếp theo tên
                    playerList.sort((a, b) -> a.getPlayerName().compareToIgnoreCase(b.getPlayerName()));

                    playerAdapter.notifyDataSetChanged();
                    updateUI();
                    Log.d(TAG, "Loaded " + playerList.size() + " players for team " + teamId);
                });
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Dialog xác nhận xóa nhanh */
    private void showQuickDeleteDialog(Player player) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa cầu thủ")
                .setMessage("Bạn có chắc muốn xóa cầu thủ \"" + player.getPlayerName() + "\"?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection(COLLECTION).document(player.getPlayerId())
                            .delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this,
                                            "🗑️ Đã xóa \"" + player.getPlayerName() + "\"",
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

    /** Cập nhật empty state + đếm cầu thủ */
    private void updateUI() {
        int count = playerList.size();
        if (count == 0) {
            rvPlayers.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvPlayerCount.setText("Chưa có cầu thủ nào");
        } else {
            rvPlayers.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvPlayerCount.setText(count + " cầu thủ");
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
