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

import giangvc.cntt.ntu.footballhub.Adapters.TournamentAdapter;
import giangvc.cntt.ntu.footballhub.Models.Tournament;
import giangvc.cntt.ntu.footballhub.R;

/**
 * TournamentManagementActivity — Danh sách giải đấu (Full CRUD)
 *  - READ   : Real-time listener Firestore
 *  - CREATE : FAB → AddTournamentActivity
 *  - UPDATE/DELETE : Click → TournamentDetailActivity
 *  - DELETE nhanh  : Long-click → Dialog
 */
public class TournamentManagementActivity extends AppCompatActivity {

    private static final String TAG        = "TournamentMgmt";
    private static final String COLLECTION = "Tournaments";

    // ── Views ──────────────────────────────────────────────────────────────────
    private RecyclerView                rvTournaments;
    private ExtendedFloatingActionButton fabAddTournament;
    private View                        layoutEmpty;
    private TextView                    tvTournamentCount;

    // ── Data ───────────────────────────────────────────────────────────────────
    private TournamentAdapter  adapter;
    private List<Tournament>   tournamentList;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore    db;
    private ListenerRegistration listenerRegistration;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_management);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();

        rvTournaments     = findViewById(R.id.rvTournaments);
        fabAddTournament  = findViewById(R.id.fabAddTournament);
        layoutEmpty       = findViewById(R.id.layoutEmpty);
        tvTournamentCount = findViewById(R.id.tvTournamentCount);

        tournamentList = new ArrayList<>();
        adapter        = new TournamentAdapter(tournamentList);
        rvTournaments.setLayoutManager(new LinearLayoutManager(this));
        rvTournaments.setAdapter(adapter);

        // FAB shrink/extend khi scroll
        rvTournaments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) fabAddTournament.shrink();
                else        fabAddTournament.extend();
            }
        });

        // Click → Detail (MatchScheduleActivity)
        adapter.setOnTournamentClickListener(tournament -> {
            Intent intent = new Intent(this, MatchScheduleActivity.class);
            intent.putExtra("TOURNAMENT_ID", tournament.getTournamentId());
            startActivity(intent);
        });

        // Long-click → xóa nhanh
        adapter.setOnTournamentLongClickListener(this::showQuickDeleteDialog);

        // FAB → Tạo giải đấu mới
        fabAddTournament.setOnClickListener(v ->
                startActivity(new Intent(this, AddTournamentActivity.class))
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenToTournaments();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void listenToTournaments() {
        listenerRegistration = db.collection(COLLECTION)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage());
                        Toast.makeText(this, "Không tải được giải đấu.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots == null) return;

                    tournamentList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Tournament t = doc.toObject(Tournament.class);
                        if (t != null) tournamentList.add(t);
                    }

                    adapter.notifyDataSetChanged();
                    updateUI();
                    Log.d(TAG, "Loaded " + tournamentList.size() + " tournaments");
                });
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void showQuickDeleteDialog(Tournament tournament) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa giải đấu")
                .setMessage("Bạn có chắc muốn xóa giải \"" + tournament.getTournamentName() + "\"?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Xóa", (dialog, which) ->
                        db.collection(COLLECTION).document(tournament.getTournamentId())
                                .delete()
                                .addOnSuccessListener(a -> {
                                        // Xóa các trận đấu thuộc giải đấu này
                                        db.collection("Matches").whereEqualTo("tournamentId", tournament.getTournamentId())
                                                .get().addOnSuccessListener(snapshots -> {
                                                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                                                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                                        batch.delete(doc.getReference());
                                                    }
                                                    batch.commit();
                                                });
                                                
                                        Toast.makeText(this,
                                                "🗑️ Đã xóa giải \"" + tournament.getTournamentName() + "\" và các lịch thi đấu liên quan",
                                                Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                )
                )
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void updateUI() {
        int count = tournamentList.size();
        if (count == 0) {
            rvTournaments.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvTournamentCount.setText("Chưa có giải đấu nào");
        } else {
            rvTournaments.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvTournamentCount.setText(count + " giải đấu");
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
