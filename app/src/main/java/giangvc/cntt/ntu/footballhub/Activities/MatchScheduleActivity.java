package giangvc.cntt.ntu.footballhub.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import giangvc.cntt.ntu.footballhub.Adapters.MatchAdapter;
import giangvc.cntt.ntu.footballhub.Models.Match;
import giangvc.cntt.ntu.footballhub.Models.Tournament;
import giangvc.cntt.ntu.footballhub.R;

public class MatchScheduleActivity extends AppCompatActivity {

    private static final String TAG = "MatchScheduleActivity";
    private static final String COLLECTION = "Matches";

    private RecyclerView rvMatches;
    private ExtendedFloatingActionButton fabAddMatch;
    private View layoutEmpty;
    private LinearLayout layoutTournamentSelector;
    private Spinner spinnerGlobalTournament;

    private MatchAdapter adapter;
    private List<Match> matchList;

    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

    private String filterTournamentId = null;
    private boolean isReadOnly = true;
    
    private List<Tournament> globalTournaments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_schedule);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        
        filterTournamentId = getIntent().getStringExtra("TOURNAMENT_ID");
        isReadOnly = (filterTournamentId == null);

        if (getSupportActionBar() != null) {
            if (!isReadOnly) {
                getSupportActionBar().setTitle("Quản lý trận đấu");
            }
        }

        rvMatches = findViewById(R.id.rvMatches);
        fabAddMatch = findViewById(R.id.fabAddMatch);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutTournamentSelector = findViewById(R.id.layoutTournamentSelector);
        spinnerGlobalTournament = findViewById(R.id.spinnerGlobalTournament);

        matchList = new ArrayList<>();
        adapter = new MatchAdapter(matchList);
        rvMatches.setLayoutManager(new LinearLayoutManager(this));
        rvMatches.setAdapter(adapter);

        rvMatches.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) fabAddMatch.shrink();
                else        fabAddMatch.extend();
            }
        });

        adapter.setOnMatchClickListener(match -> {
            if (isReadOnly) {
                Toast.makeText(this, "Vui lòng vào Quản lý Giải đấu để cập nhật thông tin trận đấu.", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, MatchDetailActivity.class);
                intent.putExtra("MATCH_ID", match.getMatchId());
                startActivity(intent);
            }
        });

        if (isReadOnly) {
            fabAddMatch.setVisibility(View.GONE);
            layoutTournamentSelector.setVisibility(View.VISIBLE);
            loadGlobalTournaments();
        } else {
            fabAddMatch.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddMatchActivity.class);
                intent.putExtra("TOURNAMENT_ID", filterTournamentId);
                startActivity(intent);
            });
            listenToMatches();
        }
    }

    private void loadGlobalTournaments() {
        db.collection("Tournaments").get().addOnSuccessListener(snapshots -> {
            globalTournaments = new ArrayList<>();
            List<String> tournamentNames = new ArrayList<>();
            
            // Tùy chọn "Tất cả giải đấu"
            Tournament allT = new Tournament();
            allT.setTournamentId("");
            globalTournaments.add(allT);
            tournamentNames.add("Tất cả giải đấu");
            
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Tournament t = doc.toObject(Tournament.class);
                if (t != null) {
                    globalTournaments.add(t);
                    tournamentNames.add(t.getTournamentName());
                }
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tournamentNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerGlobalTournament.setAdapter(adapter);
            
            spinnerGlobalTournament.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedId = globalTournaments.get(position).getTournamentId();
                    filterTournamentId = selectedId.isEmpty() ? null : selectedId;
                    listenToMatches();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void listenToMatches() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        Query query = db.collection(COLLECTION);
        if (filterTournamentId != null) {
            query = query.whereEqualTo("tournamentId", filterTournamentId);
        }
        
        listenerRegistration = query.addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage());
                        Toast.makeText(this, "Không tải được lịch thi đấu.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots == null) return;

                    matchList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Match match = doc.toObject(Match.class);
                        if (match != null) {
                            matchList.add(match);
                        }
                    }
                    
                    // Sort in Java to avoid requiring a Firestore Composite Index
                    java.util.Collections.sort(matchList, (m1, m2) -> {
                        int w1 = getRoundWeight(m1.getRound());
                        int w2 = getRoundWeight(m2.getRound());
                        if (w1 != w2) {
                            return Integer.compare(w1, w2);
                        }
                        return Long.compare(m1.getMatchOrder(), m2.getMatchOrder());
                    });
                    
                    adapter.notifyDataSetChanged();
                    updateUI();
                });
    }

    private void updateUI() {
        if (matchList.isEmpty()) {
            rvMatches.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvMatches.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private int getRoundWeight(String round) {
        if (round == null) return 99;
        String lowerRound = round.toLowerCase();
        if (lowerRound.contains("sơ loại")) return 1;
        if (lowerRound.contains("vòng 16") || lowerRound.contains("vòng 8")) return 2;
        if (lowerRound.contains("tứ kết")) return 3;
        if (lowerRound.contains("bán kết")) return 4;
        if (lowerRound.contains("tranh hạng 3")) return 5;
        if (lowerRound.contains("chung kết")) return 6;
        return 10; // Các vòng đấu khác
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) listenerRegistration.remove();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        if (!isReadOnly) {
            getMenuInflater().inflate(R.menu.menu_match_schedule, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_edit_tournament) {
            Intent intent = new Intent(this, TournamentDetailActivity.class);
            intent.putExtra(TournamentDetailActivity.EXTRA_TOURNAMENT_ID, filterTournamentId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
