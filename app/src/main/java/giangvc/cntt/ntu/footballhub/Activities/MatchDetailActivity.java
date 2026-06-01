package giangvc.cntt.ntu.footballhub.Activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import giangvc.cntt.ntu.footballhub.Models.Match;
import giangvc.cntt.ntu.footballhub.Models.MatchEvent;
import giangvc.cntt.ntu.footballhub.Models.Player;
import giangvc.cntt.ntu.footballhub.Models.Tournament;
import giangvc.cntt.ntu.footballhub.R;

public class MatchDetailActivity extends AppCompatActivity {

    private static final String COLLECTION = "Matches";
    private String matchId;
    private Match currentMatch;
    private Tournament currentTournament;

    // View references
    private TextView tvTournamentName, tvDisplayTeam1, tvDisplayTeam2;
    private TextView tvScore1, tvScore2;
    private EditText etPenalty1, etPenalty2;
    private Spinner spinnerTeam1, spinnerTeam2, spinnerStatus;
    private TextInputEditText etDate, etTime, etLocation, etReferee;
    private LinearLayout layoutEventsContainer;
    private LinearLayout layoutTeam1Events, layoutTeam2Events;
    private MaterialButton btnAddGoal, btnAddCard, btnUpdateMatch, btnDeleteMatch;

    private FirebaseFirestore db;
    private String[] statusValues = {Match.STATUS_UPCOMING, Match.STATUS_ONGOING, Match.STATUS_FINISHED};

    // Data lists
    private List<String> tournamentTeamIds = new ArrayList<>();
    private List<String> tournamentTeamNames = new ArrayList<>();
    
    // Players caching: teamId -> List of Players
    private Map<String, List<Player>> teamPlayersCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        matchId = getIntent().getStringExtra("MATCH_ID");

        bindViews();
        setupStatusSpinner();
        
        btnAddGoal.setOnClickListener(v -> addEventRow(MatchEvent.TYPE_GOAL));
        btnAddCard.setOnClickListener(v -> addEventRow(MatchEvent.TYPE_YELLOW));
        btnUpdateMatch.setOnClickListener(v -> updateMatch());
        btnDeleteMatch.setOnClickListener(v -> confirmDelete());

        loadMatchData();
    }

    private void bindViews() {
        tvTournamentName = findViewById(R.id.tvTournamentName);
        tvDisplayTeam1 = findViewById(R.id.tvDisplayTeam1);
        tvDisplayTeam2 = findViewById(R.id.tvDisplayTeam2);
        spinnerTeam1 = findViewById(R.id.spinnerTeam1);
        spinnerTeam2 = findViewById(R.id.spinnerTeam2);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        
        // Score displays (now TextView, auto-updated)
        tvScore1 = findViewById(R.id.etScore1);
        tvScore2 = findViewById(R.id.etScore2);
        
        etPenalty1 = findViewById(R.id.etPenalty1);
        etPenalty2 = findViewById(R.id.etPenalty2);
        
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLocation = findViewById(R.id.etLocation);
        etReferee = findViewById(R.id.etReferee);
        
        layoutEventsContainer = findViewById(R.id.layoutEventsContainer);
        layoutTeam1Events = findViewById(R.id.layoutTeam1Events);
        layoutTeam2Events = findViewById(R.id.layoutTeam2Events);
        
        btnAddGoal = findViewById(R.id.btnAddGoal);
        btnAddCard = findViewById(R.id.btnAddCard);
        btnUpdateMatch = findViewById(R.id.btnUpdateMatch);
        btnDeleteMatch = findViewById(R.id.btnDeleteMatch);
    }

    private void setupStatusSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void loadMatchData() {
        if (matchId == null) return;
        db.collection(COLLECTION).document(matchId).get().addOnSuccessListener(doc -> {
            currentMatch = doc.toObject(Match.class);
            if (currentMatch != null) {
                populateMatchData();
                loadTournamentData(); // Load tournament teams after match is loaded
            }
        });
    }

    private void populateMatchData() {
        tvTournamentName.setText(currentMatch.getTournamentName());
        tvDisplayTeam1.setText(currentMatch.getTeam1Name());
        tvDisplayTeam2.setText(currentMatch.getTeam2Name());
        
        etDate.setText(currentMatch.getMatchDate());
        etTime.setText(currentMatch.getMatchTime());
        etLocation.setText(currentMatch.getLocation());
        etReferee.setText(currentMatch.getReferee());
        
        tvScore1.setText(String.valueOf(currentMatch.getScoreTeam1()));
        tvScore2.setText(String.valueOf(currentMatch.getScoreTeam2()));
        
        if (currentMatch.getPenaltyTeam1() != null) {
            etPenalty1.setText(String.valueOf(currentMatch.getPenaltyTeam1()));
        } else {
            etPenalty1.setText("");
        }
        
        if (currentMatch.getPenaltyTeam2() != null) {
            etPenalty2.setText(String.valueOf(currentMatch.getPenaltyTeam2()));
        } else {
            etPenalty2.setText("");
        }

        for (int i = 0; i < statusValues.length; i++) {
            if (statusValues[i].equals(currentMatch.getStatus())) {
                spinnerStatus.setSelection(i);
                break;
            }
        }
        
        // Restore events
        if (currentMatch.getEvents() != null) {
            layoutEventsContainer.removeAllViews();
            for (MatchEvent event : currentMatch.getEvents()) {
                restoreEventRow(event);
            }
        }
        
        // Update event summary display
        refreshEventSummary();
    }

    private void loadTournamentData() {
        db.collection("Tournaments").document(currentMatch.getTournamentId()).get().addOnSuccessListener(doc -> {
            currentTournament = doc.toObject(Tournament.class);
            if (currentTournament != null && currentTournament.getTeamIds() != null) {
                tournamentTeamIds.clear();
                tournamentTeamNames.clear();
                
                // Add current placeholder teams if they are not real teams
                if (!currentTournament.getTeamIds().contains(currentMatch.getTeam1Id())) {
                    tournamentTeamIds.add(currentMatch.getTeam1Id());
                    tournamentTeamNames.add(currentMatch.getTeam1Name());
                }
                if (!currentTournament.getTeamIds().contains(currentMatch.getTeam2Id())) {
                    tournamentTeamIds.add(currentMatch.getTeam2Id());
                    tournamentTeamNames.add(currentMatch.getTeam2Name());
                }
                
                tournamentTeamIds.addAll(currentTournament.getTeamIds());
                tournamentTeamNames.addAll(currentTournament.getTeamNames());
                
                setupTeamSpinners();
            }
        });
    }

    private void setupTeamSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tournamentTeamNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        spinnerTeam1.setAdapter(adapter);
        spinnerTeam2.setAdapter(adapter);
        
        // Select current teams
        int pos1 = tournamentTeamIds.indexOf(currentMatch.getTeam1Id());
        int pos2 = tournamentTeamIds.indexOf(currentMatch.getTeam2Id());
        if (pos1 >= 0) spinnerTeam1.setSelection(pos1);
        if (pos2 >= 0) spinnerTeam2.setSelection(pos2);
        
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTeamDisplays();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerTeam1.setOnItemSelectedListener(listener);
        spinnerTeam2.setOnItemSelectedListener(listener);
        
        // Pre-fetch players for all teams in this tournament to make UI responsive
        for (String tId : currentTournament.getTeamIds()) {
            if (!tId.isEmpty()) fetchPlayersForTeam(tId);
        }
    }

    private void updateTeamDisplays() {
        int pos1 = spinnerTeam1.getSelectedItemPosition();
        int pos2 = spinnerTeam2.getSelectedItemPosition();
        if (pos1 >= 0 && pos2 >= 0 && !tournamentTeamNames.isEmpty()) {
            tvDisplayTeam1.setText(tournamentTeamNames.get(pos1));
            tvDisplayTeam2.setText(tournamentTeamNames.get(pos2));
        }
    }

    private void fetchPlayersForTeam(String teamId) {
        if (teamPlayersCache.containsKey(teamId)) return;
        db.collection("Players").whereEqualTo("teamId", teamId).get().addOnSuccessListener(task -> {
            List<Player> players = new ArrayList<>();
            for (QueryDocumentSnapshot doc : task) {
                Player p = doc.toObject(Player.class);
                p.setPlayerId(doc.getId());
                players.add(p);
            }
            teamPlayersCache.put(teamId, players);
        });
    }

    // ── Events ────────────────────────────────────────────────────────────────
    
    private void addEventRow(String defaultType) {
        createEventRow(null, defaultType);
    }
    
    private void restoreEventRow(MatchEvent event) {
        createEventRow(event, event.getType());
    }
    
    private void createEventRow(MatchEvent existingEvent, String defaultType) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_match_event, layoutEventsContainer, false);
        
        Spinner spinTeam = view.findViewById(R.id.spinnerTeam);
        Spinner spinType = view.findViewById(R.id.spinnerType);
        Spinner spinPlayer = view.findViewById(R.id.spinnerPlayer);
        EditText etMinute = view.findViewById(R.id.etMinute);
        ImageButton btnRemove = view.findViewById(R.id.btnRemoveEvent);
        TextView tvEventSummary = view.findViewById(R.id.tvEventSummary);
        
        // Setup Types
        String[] types = {MatchEvent.TYPE_GOAL, MatchEvent.TYPE_YELLOW, MatchEvent.TYPE_RED};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinType.setAdapter(typeAdapter);
        if (existingEvent != null) {
            for (int i = 0; i < types.length; i++) {
                if (types[i].equals(existingEvent.getType())) {
                    spinType.setSelection(i); break;
                }
            }
        } else {
            for (int i = 0; i < types.length; i++) {
                if (types[i].equals(defaultType)) {
                    spinType.setSelection(i); break;
                }
            }
        }
        
        // Setup Teams (only the 2 teams playing)
        int pos1 = spinnerTeam1.getSelectedItemPosition();
        int pos2 = spinnerTeam2.getSelectedItemPosition();
        String t1Name = pos1 >= 0 ? tournamentTeamNames.get(pos1) : currentMatch.getTeam1Name();
        String t2Name = pos2 >= 0 ? tournamentTeamNames.get(pos2) : currentMatch.getTeam2Name();
        String t1Id = pos1 >= 0 ? tournamentTeamIds.get(pos1) : currentMatch.getTeam1Id();
        String t2Id = pos2 >= 0 ? tournamentTeamIds.get(pos2) : currentMatch.getTeam2Id();
        
        String[] teamNames = {t1Name, t2Name};
        String[] teamIds = {t1Id, t2Id};
        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamNames);
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinTeam.setAdapter(teamAdapter);
        
        if (existingEvent != null && existingEvent.getTeamId().equals(t2Id)) {
            spinTeam.setSelection(1);
        }
        
        // When Team changes, reload Player spinner
        spinTeam.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                String selectedTeamId = teamIds[pos];
                List<Player> players = teamPlayersCache.get(selectedTeamId);
                List<String> playerNames = new ArrayList<>();
                if (players != null) {
                    for (Player p : players) {
                        String display = p.getPlayerName();
                        if (p.getJerseyNumber() > 0) {
                            display = "#" + p.getJerseyNumber() + " " + display;
                        }
                        display += " (" + p.getPlayerClass() + ")";
                        playerNames.add(display);
                    }
                } else {
                    playerNames.add("Chưa tải xong...");
                }
                
                ArrayAdapter<String> playerAdapter = new ArrayAdapter<>(MatchDetailActivity.this, android.R.layout.simple_spinner_item, playerNames);
                playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinPlayer.setAdapter(playerAdapter);
                
                // Select existing player if any
                if (existingEvent != null && existingEvent.getTeamId().equals(selectedTeamId) && players != null) {
                    for (int i = 0; i < players.size(); i++) {
                        if (players.get(i).getPlayerId().equals(existingEvent.getPlayerId())) {
                            spinPlayer.setSelection(i); break;
                        }
                    }
                }
                
                // Auto-update score & summary
                recalculateScore();
                refreshEventSummary();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // When Type changes → auto-update score (e.g. if switched from goal to card)
        spinType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                recalculateScore();
                refreshEventSummary();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // When Player changes → update summary
        spinPlayer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                refreshEventSummary();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        if (existingEvent != null) etMinute.setText(String.valueOf(existingEvent.getMinute()));
        
        btnRemove.setOnClickListener(v -> {
            layoutEventsContainer.removeView(view);
            recalculateScore();
            refreshEventSummary();
        });
        
        // Save references in the view tag for data extraction later
        view.setTag(new String[]{t1Id, t2Id, t1Name, t2Name});
        layoutEventsContainer.addView(view);
        
        // Recalculate after adding
        recalculateScore();
        refreshEventSummary();
    }
    
    // ── Auto-Score Calculation ──────────────────────────────────────────────────
    
    /** Đếm số bàn thắng mỗi đội từ danh sách sự kiện và cập nhật tỷ số tự động */
    private void recalculateScore() {
        int score1 = 0, score2 = 0;
        
        int pos1 = spinnerTeam1.getSelectedItemPosition();
        int pos2 = spinnerTeam2.getSelectedItemPosition();
        if (pos1 < 0 || pos2 < 0 || tournamentTeamIds.isEmpty()) return;
        
        String t1Id = tournamentTeamIds.get(pos1);
        String t2Id = tournamentTeamIds.get(pos2);
        
        for (int i = 0; i < layoutEventsContainer.getChildCount(); i++) {
            View view = layoutEventsContainer.getChildAt(i);
            Spinner spinType = view.findViewById(R.id.spinnerType);
            Spinner spinTeam = view.findViewById(R.id.spinnerTeam);
            
            if (spinType == null || spinTeam == null) continue;
            
            String type = spinType.getSelectedItem() != null ? spinType.getSelectedItem().toString() : "";
            
            // Chỉ đếm bàn thắng
            if (MatchEvent.TYPE_GOAL.equals(type)) {
                String[] teamData = (String[]) view.getTag();
                if (teamData == null) continue;
                int teamPos = spinTeam.getSelectedItemPosition();
                if (teamPos < 0 || teamPos >= 2) continue;
                String selectedTeamId = teamData[teamPos];
                
                if (selectedTeamId.equals(t1Id)) score1++;
                else if (selectedTeamId.equals(t2Id)) score2++;
            }
        }
        
        tvScore1.setText(String.valueOf(score1));
        tvScore2.setText(String.valueOf(score2));
    }
    
    // ── Event Summary Display ──────────────────────────────────────────────────
    
    /** Hiển thị tóm tắt sự kiện bên cạnh tỷ số: tên cầu thủ, số áo, phút */
    private void refreshEventSummary() {
        layoutTeam1Events.removeAllViews();
        layoutTeam2Events.removeAllViews();
        
        int pos1 = spinnerTeam1.getSelectedItemPosition();
        int pos2 = spinnerTeam2.getSelectedItemPosition();
        if (pos1 < 0 || pos2 < 0 || tournamentTeamIds.isEmpty()) return;
        
        String t1Id = tournamentTeamIds.get(pos1);
        String t2Id = tournamentTeamIds.get(pos2);
        
        for (int i = 0; i < layoutEventsContainer.getChildCount(); i++) {
            View view = layoutEventsContainer.getChildAt(i);
            Spinner spinType = view.findViewById(R.id.spinnerType);
            Spinner spinTeam = view.findViewById(R.id.spinnerTeam);
            Spinner spinPlayer = view.findViewById(R.id.spinnerPlayer);
            EditText etMinute = view.findViewById(R.id.etMinute);
            
            if (spinType == null || spinTeam == null || spinPlayer == null) continue;
            
            String type = spinType.getSelectedItem() != null ? spinType.getSelectedItem().toString() : "";
            String[] teamData = (String[]) view.getTag();
            if (teamData == null) continue;
            int teamPos = spinTeam.getSelectedItemPosition();
            if (teamPos < 0 || teamPos >= 2) continue;
            String selectedTeamId = teamData[teamPos];
            
            // Lấy thông tin cầu thủ
            String playerName = "???";
            int jerseyNumber = 0;
            int playerPos = spinPlayer.getSelectedItemPosition();
            List<Player> players = teamPlayersCache.get(selectedTeamId);
            if (players != null && playerPos >= 0 && playerPos < players.size()) {
                Player p = players.get(playerPos);
                playerName = p.getPlayerName();
                jerseyNumber = p.getJerseyNumber();
            }
            
            // Lấy phút
            String minuteStr = etMinute.getText().toString().trim();
            
            // Tạo icon theo loại sự kiện
            String icon;
            int textColor;
            switch (type) {
                case MatchEvent.TYPE_GOAL:
                    icon = "⚽";
                    textColor = Color.parseColor("#2E7D32"); // xanh lá
                    break;
                case MatchEvent.TYPE_YELLOW:
                    icon = "🟡";
                    textColor = Color.parseColor("#F9A825"); // vàng đậm
                    break;
                case MatchEvent.TYPE_RED:
                    icon = "🔴";
                    textColor = Color.parseColor("#C62828"); // đỏ
                    break;
                default:
                    icon = "📌";
                    textColor = Color.parseColor("#757575");
                    break;
            }
            
            // Format: ⚽ #7 Nguyễn Văn A (45')
            StringBuilder sb = new StringBuilder();
            sb.append(icon).append(" ");
            if (jerseyNumber > 0) sb.append("#").append(jerseyNumber).append(" ");
            sb.append(playerName);
            if (!minuteStr.isEmpty()) sb.append(" (").append(minuteStr).append("')");
            
            // Tạo TextView cho sự kiện
            TextView tvEvent = new TextView(this);
            tvEvent.setText(sb.toString());
            tvEvent.setTextSize(12);
            tvEvent.setTextColor(textColor);
            tvEvent.setTypeface(null, Typeface.BOLD);
            tvEvent.setPadding(0, 2, 0, 2);
            
            // Thêm vào cột tương ứng
            if (selectedTeamId.equals(t1Id)) {
                tvEvent.setGravity(Gravity.END);
                layoutTeam1Events.addView(tvEvent);
            } else if (selectedTeamId.equals(t2Id)) {
                tvEvent.setGravity(Gravity.START);
                layoutTeam2Events.addView(tvEvent);
            }
        }
    }
    
    // ── Extract Events for Saving ──────────────────────────────────────────────
    
    private List<MatchEvent> extractEvents() {
        List<MatchEvent> events = new ArrayList<>();
        for (int i = 0; i < layoutEventsContainer.getChildCount(); i++) {
            View view = layoutEventsContainer.getChildAt(i);
            Spinner spinTeam = view.findViewById(R.id.spinnerTeam);
            Spinner spinType = view.findViewById(R.id.spinnerType);
            Spinner spinPlayer = view.findViewById(R.id.spinnerPlayer);
            EditText etMinute = view.findViewById(R.id.etMinute);
            
            String[] teamData = (String[]) view.getTag();
            int teamPos = spinTeam.getSelectedItemPosition();
            if (teamPos < 0 || teamData == null) continue;
            
            String teamId = teamData[teamPos];
            String teamName = teamData[teamPos + 2];
            String type = spinType.getSelectedItem().toString();
            
            String playerId = "";
            String playerName = "Unknown";
            int jerseyNumber = 0;
            int playerPos = spinPlayer.getSelectedItemPosition();
            List<Player> players = teamPlayersCache.get(teamId);
            if (players != null && playerPos >= 0 && playerPos < players.size()) {
                Player p = players.get(playerPos);
                playerId = p.getPlayerId();
                playerName = p.getPlayerName();
                jerseyNumber = p.getJerseyNumber();
            }
            
            int minute = 0;
            try { minute = Integer.parseInt(etMinute.getText().toString()); } catch (Exception ignored) {}
            
            String eventId = db.collection("Events").document().getId();
            events.add(new MatchEvent(eventId, type, teamId, teamName, playerId, playerName, jerseyNumber, minute));
        }
        return events;
    }

    // ── Save/Update ───────────────────────────────────────────────────────────
    
    private void updateMatch() {
        if (currentMatch == null || tournamentTeamIds.isEmpty()) return;
        
        int pos1 = spinnerTeam1.getSelectedItemPosition();
        int pos2 = spinnerTeam2.getSelectedItemPosition();
        if (pos1 >= 0 && pos2 >= 0 && pos1 == pos2 && tournamentTeamIds.size() > 1) {
            Toast.makeText(this, "Hai đội thi đấu không được trùng nhau!", Toast.LENGTH_SHORT).show();
            return;
        }

        String t1Id = tournamentTeamIds.get(pos1);
        String t1Name = tournamentTeamNames.get(pos1);
        String t2Id = tournamentTeamIds.get(pos2);
        String t2Name = tournamentTeamNames.get(pos2);

        // Tỷ số được tính tự động từ số bàn thắng
        int score1 = 0, score2 = 0;
        try { score1 = Integer.parseInt(tvScore1.getText().toString()); } catch (NumberFormatException ignored) {}
        try { score2 = Integer.parseInt(tvScore2.getText().toString()); } catch (NumberFormatException ignored) {}

        String status = statusValues[spinnerStatus.getSelectedItemPosition()];

        Map<String, Object> updates = new HashMap<>();
        updates.put("team1Id", t1Id);
        updates.put("team1Name", t1Name);
        updates.put("team2Id", t2Id);
        updates.put("team2Name", t2Name);
        updates.put("matchDate", getText(etDate));
        updates.put("matchTime", getText(etTime));
        updates.put("location", getText(etLocation));
        updates.put("referee", getText(etReferee));
        updates.put("scoreTeam1", score1);
        updates.put("scoreTeam2", score2);
        
        Integer pen1 = null;
        Integer pen2 = null;
        try { pen1 = Integer.parseInt(etPenalty1.getText().toString()); } catch (NumberFormatException ignored) {}
        try { pen2 = Integer.parseInt(etPenalty2.getText().toString()); } catch (NumberFormatException ignored) {}
        updates.put("penaltyTeam1", pen1);
        updates.put("penaltyTeam2", pen2);
        
        updates.put("status", status);
        updates.put("events", extractEvents());

        btnUpdateMatch.setEnabled(false);
        
        // Final variables for lambda
        final int finalScore1 = score1;
        final int finalScore2 = score2;
        final String finalT1Id = t1Id;
        final String finalT1Name = t1Name;
        final String finalT2Id = t2Id;
        final String finalT2Name = t2Name;
        
        final Integer finalPen1 = pen1;
        final Integer finalPen2 = pen2;
        
        db.collection(COLLECTION).document(matchId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (Match.STATUS_FINISHED.equals(status) && currentMatch != null && currentMatch.getMatchOrder() > 0) {
                        // Determine winner based on main score, then penalties
                        String winnerId = null;
                        String winnerName = null;
                        String loserId = null;
                        String loserName = null;
                        
                        if (finalScore1 > finalScore2) {
                            winnerId = finalT1Id; winnerName = finalT1Name;
                            loserId = finalT2Id; loserName = finalT2Name;
                        } else if (finalScore1 < finalScore2) {
                            winnerId = finalT2Id; winnerName = finalT2Name;
                            loserId = finalT1Id; loserName = finalT1Name;
                        } else if (finalPen1 != null && finalPen2 != null && !finalPen1.equals(finalPen2)) {
                            // Penalty tie break
                            if (finalPen1 > finalPen2) {
                                winnerId = finalT1Id; winnerName = finalT1Name;
                                loserId = finalT2Id; loserName = finalT2Name;
                            } else {
                                winnerId = finalT2Id; winnerName = finalT2Name;
                                loserId = finalT1Id; loserName = finalT1Name;
                            }
                        }
                        
                        if (winnerId != null) {
                            handleKnockoutAdvancement(winnerId, winnerName, loserId, loserName);
                        } else {
                            Toast.makeText(this, "Đã lưu thông tin trận đấu!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Đã lưu thông tin trận đấu!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnUpdateMatch.setEnabled(true);
                });
    }

    private void handleKnockoutAdvancement(String winnerId, String winnerName, String loserId, String loserName) {
        long matchOrder = currentMatch.getMatchOrder();
        String winPlaceholder = "Thắng Trận " + matchOrder;
        String losePlaceholder = "Thua Trận " + matchOrder;

        db.collection(COLLECTION)
                .whereEqualTo("tournamentId", currentMatch.getTournamentId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    boolean hasUpdates = false;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Match nextMatch = doc.toObject(Match.class);
                        Map<String, Object> nextUpdates = new HashMap<>();
                        
                        if (winPlaceholder.equals(nextMatch.getTeam1Name())) {
                            nextUpdates.put("team1Id", winnerId);
                            nextUpdates.put("team1Name", winnerName);
                        } else if (winPlaceholder.equals(nextMatch.getTeam2Name())) {
                            nextUpdates.put("team2Id", winnerId);
                            nextUpdates.put("team2Name", winnerName);
                        }
                        
                        if (losePlaceholder.equals(nextMatch.getTeam1Name())) {
                            nextUpdates.put("team1Id", loserId);
                            nextUpdates.put("team1Name", loserName);
                        } else if (losePlaceholder.equals(nextMatch.getTeam2Name())) {
                            nextUpdates.put("team2Id", loserId);
                            nextUpdates.put("team2Name", loserName);
                        }
                        
                        if (!nextUpdates.isEmpty()) {
                            batch.update(doc.getReference(), nextUpdates);
                            hasUpdates = true;
                        }
                    }
                    if (hasUpdates) {
                        batch.commit().addOnCompleteListener(task -> {
                            Toast.makeText(this, "Đã lưu & Cập nhật đội đi tiếp!", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    } else {
                        Toast.makeText(this, "Đã lưu thông tin trận đấu!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Đã lưu, nhưng không cập nhật được nhánh đấu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa trận đấu")
                .setMessage("Bạn có chắc muốn xóa trận đấu này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection(COLLECTION).document(matchId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Hủy", null).show();
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
