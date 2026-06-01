package giangvc.cntt.ntu.footballhub.Activities;

import android.os.Bundle;
import android.text.TextUtils;
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
    private Spinner spinnerTeam1, spinnerTeam2, spinnerStatus;
    private TextInputEditText etDate, etTime, etLocation, etReferee, etScore1, etScore2;
    private LinearLayout layoutEventsContainer;
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
        
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLocation = findViewById(R.id.etLocation);
        etReferee = findViewById(R.id.etReferee);
        etScore1 = findViewById(R.id.etScore1);
        etScore2 = findViewById(R.id.etScore2);
        
        layoutEventsContainer = findViewById(R.id.layoutEventsContainer);
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
        
        etScore1.setText(String.valueOf(currentMatch.getScoreTeam1()));
        etScore2.setText(String.valueOf(currentMatch.getScoreTeam2()));

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
                    for (Player p : players) playerNames.add(p.getPlayerName() + " (" + p.getPlayerClass() + ")");
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
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        if (existingEvent != null) etMinute.setText(String.valueOf(existingEvent.getMinute()));
        
        btnRemove.setOnClickListener(v -> layoutEventsContainer.removeView(view));
        
        // Save references in the view tag for data extraction later
        view.setTag(new String[]{t1Id, t2Id, t1Name, t2Name});
        layoutEventsContainer.addView(view);
    }
    
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
            int playerPos = spinPlayer.getSelectedItemPosition();
            List<Player> players = teamPlayersCache.get(teamId);
            if (players != null && playerPos >= 0 && playerPos < players.size()) {
                Player p = players.get(playerPos);
                playerId = p.getPlayerId();
                playerName = p.getPlayerName();
            }
            
            int minute = 0;
            try { minute = Integer.parseInt(etMinute.getText().toString()); } catch (Exception ignored) {}
            
            String eventId = db.collection("Events").document().getId();
            events.add(new MatchEvent(eventId, type, teamId, teamName, playerId, playerName, minute));
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

        String s1 = etScore1.getText() != null ? etScore1.getText().toString().trim() : "0";
        String s2 = etScore2.getText() != null ? etScore2.getText().toString().trim() : "0";
        String status = statusValues[spinnerStatus.getSelectedItemPosition()];
        
        int score1 = 0, score2 = 0;
        try {
            if (!TextUtils.isEmpty(s1)) score1 = Integer.parseInt(s1);
            if (!TextUtils.isEmpty(s2)) score2 = Integer.parseInt(s2);
        } catch (NumberFormatException ignored) {}

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
        updates.put("status", status);
        updates.put("events", extractEvents());

        btnUpdateMatch.setEnabled(false);
        db.collection(COLLECTION).document(matchId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã lưu thông tin trận đấu!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnUpdateMatch.setEnabled(true);
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
