package giangvc.cntt.ntu.footballhub.Activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import giangvc.cntt.ntu.footballhub.Models.Match;
import giangvc.cntt.ntu.footballhub.Models.MatchEvent;
import giangvc.cntt.ntu.footballhub.Models.Player;
import giangvc.cntt.ntu.footballhub.Models.Tournament;
import giangvc.cntt.ntu.footballhub.R;

public class StatisticsActivity extends AppCompatActivity {

    private Spinner spinnerTournaments;
    private TextView tvEmptyState;
    private LinearLayout layoutStatsContent;
    private TextView tvChampion, tvRunnerUp, tvThirdPlace;
    private LinearLayout layoutTopGoalkeepers;
    private LinearLayout layoutTopScorers;

    private FirebaseFirestore db;
    private List<Tournament> tournamentList = new ArrayList<>();
    private List<String> tournamentNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spinnerTournaments = findViewById(R.id.spinnerTournaments);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        layoutStatsContent = findViewById(R.id.layoutStatsContent);
        tvChampion = findViewById(R.id.tvChampion);
        tvRunnerUp = findViewById(R.id.tvRunnerUp);
        tvThirdPlace = findViewById(R.id.tvThirdPlace);
        layoutTopGoalkeepers = findViewById(R.id.layoutTopGoalkeepers);
        layoutTopScorers = findViewById(R.id.layoutTopScorers);

        db = FirebaseFirestore.getInstance();

        loadTournaments();
    }

    private void loadTournaments() {
        db.collection("Tournaments").get().addOnSuccessListener(snapshots -> {
            tournamentList.clear();
            tournamentNames.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Tournament t = doc.toObject(Tournament.class);
                tournamentList.add(t);
                tournamentNames.add(t.getTournamentName());
            }

            if (tournamentNames.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Chưa có giải đấu nào.");
                return;
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tournamentNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTournaments.setAdapter(adapter);

            spinnerTournaments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    calculateStatistics(tournamentList.get(position));
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải giải đấu", Toast.LENGTH_SHORT).show();
        });
    }

    private void calculateStatistics(Tournament tournament) {
        tvEmptyState.setVisibility(View.GONE);
        layoutStatsContent.setVisibility(View.GONE);
        
        db.collection("Matches")
                .whereEqualTo("tournamentId", tournament.getTournamentId())
                .whereEqualTo("status", Match.STATUS_FINISHED)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Match> matches = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        matches.add(doc.toObject(Match.class));
                    }
                    
                    if (matches.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Chưa có trận đấu nào kết thúc trong giải này.");
                        return;
                    }
                    
                    layoutStatsContent.setVisibility(View.VISIBLE);
                    
                    // 0. Calculate Championship
                    calculateChampionship(matches);
                    
                    // 1. Calculate Top Scorers
                    calculateTopScorers(matches);
                    
                    // 2. Calculate Golden Glove
                    calculateGoldenGlove(matches, tournament.getTeamIds());
                });
    }

    private void calculateChampionship(List<Match> matches) {
        tvChampion.setText("🥇 Vô địch: Chưa xác định");
        tvRunnerUp.setText("🥈 Á quân: Chưa xác định");
        tvThirdPlace.setText("🥉 Hạng 3: Chưa xác định");

        for (Match match : matches) {
            if (match.getRound() == null) continue;
            
            if (match.getRound().contains("Chung kết")) {
                if (match.getScoreTeam1() > match.getScoreTeam2()) {
                    tvChampion.setText("🥇 Vô địch: " + match.getTeam1Name());
                    tvRunnerUp.setText("🥈 Á quân: " + match.getTeam2Name());
                } else if (match.getScoreTeam1() < match.getScoreTeam2()) {
                    tvChampion.setText("🥇 Vô địch: " + match.getTeam2Name());
                    tvRunnerUp.setText("🥈 Á quân: " + match.getTeam1Name());
                } else if (match.getPenaltyTeam1() != null && match.getPenaltyTeam2() != null && !match.getPenaltyTeam1().equals(match.getPenaltyTeam2())) {
                    if (match.getPenaltyTeam1() > match.getPenaltyTeam2()) {
                        tvChampion.setText("🥇 Vô địch: " + match.getTeam1Name());
                        tvRunnerUp.setText("🥈 Á quân: " + match.getTeam2Name());
                    } else {
                        tvChampion.setText("🥇 Vô địch: " + match.getTeam2Name());
                        tvRunnerUp.setText("🥈 Á quân: " + match.getTeam1Name());
                    }
                } else {
                    tvChampion.setText("🥇 Vô địch: Chưa phân định (Hòa)");
                    tvRunnerUp.setText("🥈 Á quân: Chưa phân định (Hòa)");
                }
            }
            
            if (match.getRound().contains("Tranh hạng 3")) {
                if (match.getScoreTeam1() > match.getScoreTeam2()) {
                    tvThirdPlace.setText("🥉 Hạng 3: " + match.getTeam1Name());
                } else if (match.getScoreTeam1() < match.getScoreTeam2()) {
                    tvThirdPlace.setText("🥉 Hạng 3: " + match.getTeam2Name());
                } else if (match.getPenaltyTeam1() != null && match.getPenaltyTeam2() != null && !match.getPenaltyTeam1().equals(match.getPenaltyTeam2())) {
                    if (match.getPenaltyTeam1() > match.getPenaltyTeam2()) {
                        tvThirdPlace.setText("🥉 Hạng 3: " + match.getTeam1Name());
                    } else {
                        tvThirdPlace.setText("🥉 Hạng 3: " + match.getTeam2Name());
                    }
                } else {
                    tvThirdPlace.setText("🥉 Hạng 3: Chưa phân định (Hòa)");
                }
            }
        }
    }

    private static class PlayerStat {
        String playerId;
        String playerName;
        String teamName;
        int count;
        public PlayerStat(String id, String name, String team, int c) {
            playerId = id; playerName = name; teamName = team; count = c;
        }
    }

    private void calculateTopScorers(List<Match> matches) {
        Map<String, PlayerStat> scorerMap = new HashMap<>();
        
        for (Match match : matches) {
            if (match.getEvents() != null) {
                for (MatchEvent event : match.getEvents()) {
                    if (MatchEvent.TYPE_GOAL.equals(event.getType()) && event.getPlayerId() != null && !event.getPlayerId().isEmpty()) {
                        String pId = event.getPlayerId();
                        if (!scorerMap.containsKey(pId)) {
                            scorerMap.put(pId, new PlayerStat(pId, event.getPlayerName(), event.getTeamName(), 0));
                        }
                        scorerMap.get(pId).count++;
                    }
                }
            }
        }
        
        List<PlayerStat> scorers = new ArrayList<>(scorerMap.values());
        Collections.sort(scorers, (a, b) -> Integer.compare(b.count, a.count));
        
        layoutTopScorers.removeAllViews();
        if (scorers.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Chưa có bàn thắng nào được ghi.");
            layoutTopScorers.addView(tv);
            return;
        }
        
        int rank = 1;
        for (int i = 0; i < Math.min(scorers.size(), 10); i++) {
            PlayerStat ps = scorers.get(i);
            
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);
            
            TextView tvRank = new TextView(this);
            tvRank.setText(String.valueOf(rank));
            tvRank.setTextSize(16);
            tvRank.setTypeface(null, android.graphics.Typeface.BOLD);
            tvRank.setPadding(0, 0, 16, 0);
            
            LinearLayout nameLayout = new LinearLayout(this);
            nameLayout.setOrientation(LinearLayout.VERTICAL);
            nameLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            
            TextView tvName = new TextView(this);
            tvName.setText(ps.playerName);
            tvName.setTextSize(16);
            tvName.setTextColor(getResources().getColor(android.R.color.black));
            
            TextView tvTeam = new TextView(this);
            tvTeam.setText(ps.teamName);
            tvTeam.setTextSize(12);
            tvTeam.setTextColor(getResources().getColor(android.R.color.darker_gray));
            
            nameLayout.addView(tvName);
            nameLayout.addView(tvTeam);
            
            TextView tvGoals = new TextView(this);
            tvGoals.setText(ps.count + " ⚽");
            tvGoals.setTextSize(16);
            tvGoals.setTypeface(null, android.graphics.Typeface.BOLD);
            tvGoals.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
            
            row.addView(tvRank);
            row.addView(nameLayout);
            row.addView(tvGoals);
            
            layoutTopScorers.addView(row);
            rank++;
        }
    }

    private void calculateGoldenGlove(List<Match> matches, List<String> teamIds) {
        // Count goals conceded per team (Bàn thua)
        Map<String, Integer> teamConcededGoals = new HashMap<>();
        Map<String, String> teamNames = new HashMap<>();
        
        for (Match match : matches) {
            String t1 = match.getTeam1Id();
            String t2 = match.getTeam2Id();
            teamNames.put(t1, match.getTeam1Name());
            teamNames.put(t2, match.getTeam2Name());
            
            if (!teamConcededGoals.containsKey(t1)) teamConcededGoals.put(t1, 0);
            if (!teamConcededGoals.containsKey(t2)) teamConcededGoals.put(t2, 0);
            
            // Team 1 concedes what Team 2 scores
            teamConcededGoals.put(t1, teamConcededGoals.get(t1) + match.getScoreTeam2());
            // Team 2 concedes what Team 1 scores
            teamConcededGoals.put(t2, teamConcededGoals.get(t2) + match.getScoreTeam1());
        }
        
        layoutTopGoalkeepers.removeAllViews();
        if (teamIds == null || teamIds.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Không có dữ liệu đội bóng.");
            layoutTopGoalkeepers.addView(tv);
            return;
        }

        db.collection("Players")
                .whereEqualTo("position", "Thủ môn")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<PlayerStat> goalkeepers = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Player p = doc.toObject(Player.class);
                        if (teamIds.contains(p.getTeamId())) {
                            int conceded = teamConcededGoals.containsKey(p.getTeamId()) ? teamConcededGoals.get(p.getTeamId()) : 0;
                            String tName = teamNames.containsKey(p.getTeamId()) ? teamNames.get(p.getTeamId()) : "Không xác định";
                            goalkeepers.add(new PlayerStat(p.getPlayerId(), p.getPlayerName(), tName, conceded));
                        }
                    }
                    
                    // Sort by fewest goals conceded ASCENDING
                    Collections.sort(goalkeepers, (a, b) -> Integer.compare(a.count, b.count));
                    
                    if (goalkeepers.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("Chưa có thủ môn nào trong giải đấu.");
                        layoutTopGoalkeepers.addView(tv);
                        return;
                    }
                    
                    int rank = 1;
                    for (int i = 0; i < Math.min(goalkeepers.size(), 5); i++) {
                        PlayerStat gk = goalkeepers.get(i);
                        
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(0, 8, 0, 8);
                        
                        TextView tvRank = new TextView(this);
                        tvRank.setText(String.valueOf(rank));
                        tvRank.setTextSize(16);
                        tvRank.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvRank.setPadding(0, 0, 16, 0);
                        
                        LinearLayout nameLayout = new LinearLayout(this);
                        nameLayout.setOrientation(LinearLayout.VERTICAL);
                        nameLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                        
                        TextView tvName = new TextView(this);
                        tvName.setText(gk.playerName);
                        tvName.setTextSize(16);
                        tvName.setTextColor(getResources().getColor(android.R.color.black));
                        
                        TextView tvTeam = new TextView(this);
                        tvTeam.setText(gk.teamName);
                        tvTeam.setTextSize(12);
                        tvTeam.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        
                        nameLayout.addView(tvName);
                        nameLayout.addView(tvTeam);
                        
                        TextView tvConceded = new TextView(this);
                        tvConceded.setText(gk.count + " ⚽");
                        tvConceded.setTextSize(16);
                        tvConceded.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvConceded.setTextColor(android.graphics.Color.parseColor("#D32F2F")); // Red for goals conceded
                        
                        row.addView(tvRank);
                        row.addView(nameLayout);
                        row.addView(tvConceded);
                        
                        layoutTopGoalkeepers.addView(row);
                        rank++;
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
