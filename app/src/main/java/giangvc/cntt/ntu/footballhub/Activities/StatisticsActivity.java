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
    private LinearLayout layoutCardStats;

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
        layoutCardStats = findViewById(R.id.layoutCardStats);

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
                    
                    // 3. Calculate Card Stats
                    calculateCardStats(matches);
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
        String teamId;
        String teamName;
        int count;
        int matchesPlayed;
        double coefficient;
        
        public PlayerStat(String id, String name, String tId, String team, int c) {
            playerId = id; playerName = name; teamId = tId; teamName = team; count = c;
        }
    }

    private java.util.Set<String> getSemiFinalTeams(List<Match> matches) {
        java.util.Set<String> semiFinalTeams = new java.util.HashSet<>();
        for (Match match : matches) {
            if (match.getRound() != null && 
                (match.getRound().contains("Bán kết") || 
                 match.getRound().contains("Chung kết") || 
                 match.getRound().contains("Tranh hạng 3"))) {
                if (match.getTeam1Id() != null && !match.getTeam1Id().isEmpty()) semiFinalTeams.add(match.getTeam1Id());
                if (match.getTeam2Id() != null && !match.getTeam2Id().isEmpty()) semiFinalTeams.add(match.getTeam2Id());
            }
        }
        return semiFinalTeams;
    }

    private void calculateTopScorers(List<Match> matches) {
        Map<String, PlayerStat> scorerMap = new HashMap<>();
        java.util.Set<String> semiFinalTeams = getSemiFinalTeams(matches);
        
        for (Match match : matches) {
            if (match.getEvents() != null) {
                for (MatchEvent event : match.getEvents()) {
                    if (MatchEvent.TYPE_GOAL.equals(event.getType()) && event.getPlayerId() != null && !event.getPlayerId().isEmpty()) {
                        String pId = event.getPlayerId();
                        if (!scorerMap.containsKey(pId)) {
                            scorerMap.put(pId, new PlayerStat(pId, event.getPlayerName(), event.getTeamId(), event.getTeamName(), 0));
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
            if (i == 0) {
                tvName.setText(ps.playerName + " 👟");
            } else {
                tvName.setText(ps.playerName);
            }
            tvName.setTextSize(16);
            if (ps.teamId != null && semiFinalTeams.contains(ps.teamId)) {
                tvName.setTextColor(android.graphics.Color.RED);
            } else {
                tvName.setTextColor(getResources().getColor(android.R.color.black));
            }
            
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
        java.util.Set<String> semiFinalTeams = getSemiFinalTeams(matches);
        
        Map<String, Integer> teamConcededGoals = new HashMap<>();
        Map<String, Integer> teamMatchesPlayed = new HashMap<>();
        Map<String, String> teamNames = new HashMap<>();
        
        for (Match match : matches) {
            String t1 = match.getTeam1Id();
            String t2 = match.getTeam2Id();
            if (t1 != null) teamNames.put(t1, match.getTeam1Name());
            if (t2 != null) teamNames.put(t2, match.getTeam2Name());
            
            if (t1 != null) {
                teamConcededGoals.put(t1, (teamConcededGoals.containsKey(t1) ? teamConcededGoals.get(t1) : 0) + match.getScoreTeam2());
                teamMatchesPlayed.put(t1, (teamMatchesPlayed.containsKey(t1) ? teamMatchesPlayed.get(t1) : 0) + 1);
            }
            if (t2 != null) {
                teamConcededGoals.put(t2, (teamConcededGoals.containsKey(t2) ? teamConcededGoals.get(t2) : 0) + match.getScoreTeam1());
                teamMatchesPlayed.put(t2, (teamMatchesPlayed.containsKey(t2) ? teamMatchesPlayed.get(t2) : 0) + 1);
            }
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
                        if (p.getTeamId() != null && semiFinalTeams.contains(p.getTeamId())) {
                            int conceded = teamConcededGoals.containsKey(p.getTeamId()) ? teamConcededGoals.get(p.getTeamId()) : 0;
                            int matchesPlayed = teamMatchesPlayed.containsKey(p.getTeamId()) ? teamMatchesPlayed.get(p.getTeamId()) : 0;
                            String tName = teamNames.containsKey(p.getTeamId()) ? teamNames.get(p.getTeamId()) : "Không xác định";
                            
                            PlayerStat gk = new PlayerStat(p.getPlayerId(), p.getPlayerName(), p.getTeamId(), tName, conceded);
                            gk.matchesPlayed = matchesPlayed;
                            gk.coefficient = matchesPlayed > 0 ? (double) conceded / matchesPlayed : 0;
                            goalkeepers.add(gk);
                        }
                    }
                    
                    Collections.sort(goalkeepers, (a, b) -> Double.compare(a.coefficient, b.coefficient));
                    
                    if (goalkeepers.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("Chưa có thủ môn nào từ các đội vào bán kết.");
                        layoutTopGoalkeepers.addView(tv);
                        return;
                    }
                    
                    int rank = 1;
                    for (int i = 0; i < Math.min(goalkeepers.size(), 4); i++) {
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
                        if (i == 0) {
                            tvName.setText(gk.playerName + " 🧤");
                        } else {
                            tvName.setText(gk.playerName);
                        }
                        tvName.setTextSize(16);
                        tvName.setTextColor(getResources().getColor(android.R.color.black));
                        
                        TextView tvTeam = new TextView(this);
                        tvTeam.setText(gk.teamName);
                        tvTeam.setTextSize(12);
                        tvTeam.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        
                        nameLayout.addView(tvName);
                        nameLayout.addView(tvTeam);
                        
                        TextView tvConceded = new TextView(this);
                        String displayStr = String.format("%.2f (%d/%d)", gk.coefficient, gk.count, gk.matchesPlayed);
                        tvConceded.setText(displayStr);
                        tvConceded.setTextSize(14);
                        tvConceded.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvConceded.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                        
                        row.addView(tvRank);
                        row.addView(nameLayout);
                        row.addView(tvConceded);
                        
                        layoutTopGoalkeepers.addView(row);
                        rank++;
                    }
                });
    }

    private void calculateCardStats(List<Match> matches) {
        class TeamCardStat {
            String teamName;
            int yellowCards = 0;
            int redCards = 0;
        }
        
        Map<String, TeamCardStat> teamCardMap = new HashMap<>();
        
        for (Match match : matches) {
            if (match.getEvents() != null) {
                for (MatchEvent event : match.getEvents()) {
                    if (MatchEvent.TYPE_YELLOW.equals(event.getType()) || MatchEvent.TYPE_RED.equals(event.getType())) {
                        String tId = event.getTeamId();
                        if (tId != null && !tId.isEmpty()) {
                            if (!teamCardMap.containsKey(tId)) {
                                TeamCardStat stat = new TeamCardStat();
                                stat.teamName = event.getTeamName();
                                teamCardMap.put(tId, stat);
                            }
                            if (MatchEvent.TYPE_YELLOW.equals(event.getType())) {
                                teamCardMap.get(tId).yellowCards++;
                            } else if (MatchEvent.TYPE_RED.equals(event.getType())) {
                                teamCardMap.get(tId).redCards++;
                            }
                        }
                    }
                }
            }
        }
        
        List<TeamCardStat> cardStats = new ArrayList<>(teamCardMap.values());
        Collections.sort(cardStats, (a, b) -> {
            if (a.redCards != b.redCards) {
                return Integer.compare(b.redCards, a.redCards);
            }
            return Integer.compare(b.yellowCards, a.yellowCards);
        });
        
        layoutCardStats.removeAllViews();
        if (cardStats.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Chưa có thẻ phạt nào được ghi nhận.");
            layoutCardStats.addView(tv);
            return;
        }
        
        for (TeamCardStat stat : cardStats) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            
            TextView tvName = new TextView(this);
            tvName.setText(stat.teamName);
            tvName.setTextSize(16);
            tvName.setTextColor(getResources().getColor(android.R.color.black));
            tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            
            TextView tvYellow = new TextView(this);
            tvYellow.setText(stat.yellowCards + " 🟨");
            tvYellow.setTextSize(16);
            tvYellow.setTypeface(null, android.graphics.Typeface.BOLD);
            tvYellow.setTextColor(android.graphics.Color.parseColor("#F9A825"));
            tvYellow.setPadding(0, 0, 16, 0);
            
            TextView tvRed = new TextView(this);
            tvRed.setText(stat.redCards + " 🟥");
            tvRed.setTextSize(16);
            tvRed.setTypeface(null, android.graphics.Typeface.BOLD);
            tvRed.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
            
            row.addView(tvName);
            row.addView(tvYellow);
            row.addView(tvRed);
            
            layoutCardStats.addView(row);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
