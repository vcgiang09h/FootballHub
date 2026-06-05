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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import giangvc.cntt.ntu.footballhub.Adapters.MatchAdapter;
import giangvc.cntt.ntu.footballhub.Models.Match;
import giangvc.cntt.ntu.footballhub.Models.Tournament;
import giangvc.cntt.ntu.footballhub.R;

public class MatchScheduleActivity extends AppCompatActivity {

    private static final String TAG = "MatchScheduleActivity";
    private static final String COLLECTION = "Matches";
    private static final String GROQ_API_KEY = giangvc.cntt.ntu.footballhub.BuildConfig.GROQ_API_KEY;

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
            // Admin: bật nút dự đoán (nhấn giữ card trận sắp diễn ra)
            adapter.setOnPredictClickListener(match ->
                    generatePrediction(match));

            fabAddMatch.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddMatchActivity.class);
                intent.putExtra("TOURNAMENT_ID", filterTournamentId);
                startActivity(intent);
            });
            listenToMatches();
        }
    }

    /** Tạo hoặc xóa dự đoán AI cho trận sắp diễn ra */
    private void generatePrediction(Match match) {
        // Toggle logic: nếu đã có dự đoán -> xóa đi (ẩn khỏi UI)
        if (match.getAiPrediction() != null && !match.getAiPrediction().isEmpty()) {
            Map<String, Object> update = new HashMap<>();
            update.put("aiPrediction", null); // hoặc ""
            db.collection(COLLECTION).document(match.getMatchId())
                    .update(update)
                    .addOnSuccessListener(v -> Toast.makeText(this, "Đã ẩn dự đoán AI", Toast.LENGTH_SHORT).show());
            return;
        }

        Toast.makeText(this, "Đang phân tích và tạo dự đoán...", Toast.LENGTH_SHORT).show();

        String t1Id = match.getTeam1Id();
        String t2Id = match.getTeam2Id();
        String t1Name = match.getTeam1Name();
        String t2Name = match.getTeam2Name();

        // Query tất cả trận đã đấu có liên quan đến 2 đội (trong giải hiện tại)
        db.collection(COLLECTION)
                .whereEqualTo("tournamentId", match.getTournamentId())
                .whereEqualTo("status", Match.STATUS_FINISHED)
                .get()
                .addOnSuccessListener(snapshots -> {
                    StringBuilder historyT1 = new StringBuilder();
                    StringBuilder historyT2 = new StringBuilder();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Match m = doc.toObject(Match.class);
                        if (m == null) continue;

                        boolean involvesT1 = t1Id.equals(m.getTeam1Id()) || t1Id.equals(m.getTeam2Id());
                        boolean involvesT2 = t2Id.equals(m.getTeam1Id()) || t2Id.equals(m.getTeam2Id());

                        if (involvesT1) {
                            String opp = t1Id.equals(m.getTeam1Id()) ? m.getTeam2Name() : m.getTeam1Name();
                            int s1 = t1Id.equals(m.getTeam1Id()) ? m.getScoreTeam1() : m.getScoreTeam2();
                            int s2 = t1Id.equals(m.getTeam1Id()) ? m.getScoreTeam2() : m.getScoreTeam1();
                            historyT1.append("  - ").append(t1Name).append(" ").append(s1).append("-").append(s2)
                                    .append(" ").append(opp).append(" (").append(m.getRound()).append(")\n");
                        }
                        if (involvesT2) {
                            String opp = t2Id.equals(m.getTeam1Id()) ? m.getTeam2Name() : m.getTeam1Name();
                            int s1 = t2Id.equals(m.getTeam1Id()) ? m.getScoreTeam1() : m.getScoreTeam2();
                            int s2 = t2Id.equals(m.getTeam1Id()) ? m.getScoreTeam2() : m.getScoreTeam1();
                            historyT2.append("  - ").append(t2Name).append(" ").append(s1).append("-").append(s2)
                                    .append(" ").append(opp).append(" (").append(m.getRound()).append(")\n");
                        }
                    }

                    // Nếu cả 2 đội đều chưa thi đấu trận nào (không có lịch sử) -> Bỏ qua AI, lưu mặc định để hiện "???"
                    if (historyT1.length() == 0 && historyT2.length() == 0) {
                        String noDataPrediction = "WIN:-1\nDRAW:-1\nLOSE:-1\nANALYSIS:Chưa có đủ dữ liệu lịch sử thi đấu của 2 đội để AI có thể phân tích và đưa ra dự đoán hợp lý.";
                        Map<String, Object> update = new HashMap<>();
                        update.put("aiPrediction", noDataPrediction);
                        db.collection(COLLECTION).document(match.getMatchId())
                                .update(update)
                                .addOnSuccessListener(v -> Toast.makeText(this, "Đã lưu (Không đủ dữ liệu để dự đoán)", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    String prompt = "Bạn là chuyên gia phân tích bóng đá sinh viên. "
                            + "Dựa vào lịch sử thi đấu dưới đây, hãy ước tính tỷ lệ kết quả của trận:\n"
                            + match.getTeam1Name() + " vs " + match.getTeam2Name() + "\n"
                            + "Giải: " + match.getTournamentName() + " | Vòng: " + match.getRound() + "\n\n"
                            + "Lịch sử " + t1Name + ":\n" + (historyT1.length() > 0 ? historyT1 : "  Chưa có dữ liệu\n")
                            + "Lịch sử " + t2Name + ":\n" + (historyT2.length() > 0 ? historyT2 : "  Chưa có dữ liệu\n")
                            + "\nYÊU CẦU QUAN TRỌNG - Trả về ĐÚNG định dạng sau, không thêm bất cứ thứ gì khác:\n"
                            + "WIN:[số 0-100]\n"
                            + "DRAW:[số 0-100]\n"
                            + "LOSE:[số 0-100]\n"
                            + "ANALYSIS:[phân tích ngắn gọn 2-4 câu, dùng emoji, tiếng Việt]\n\n"
                            + "Lưu ý: WIN là tỷ lệ " + t1Name + " thắng. "
                            + "TUYỆT ĐỐI KHÔNG dự đoán tỷ số cụ thể. "
                            + "3 con số WIN+DRAW+LOSE phải cộng lại bằng 100.";

                    // Gọi Groq API trong background thread
                    new Thread(() -> {
                        try {
                            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/json");
                            conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
                            conn.setDoOutput(true);
                            conn.setConnectTimeout(15000);
                            conn.setReadTimeout(30000);

                            JSONObject message = new JSONObject();
                            message.put("role", "user");
                            message.put("content", prompt);
                            JSONArray messages = new JSONArray();
                            messages.put(message);
                            JSONObject body = new JSONObject();
                            body.put("model", "llama-3.1-8b-instant");
                            body.put("messages", messages);
                            body.put("max_tokens", 512);

                            OutputStream os = conn.getOutputStream();
                            os.write(body.toString().getBytes("UTF-8"));
                            os.flush();
                            os.close();

                            int code = conn.getResponseCode();
                            InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                            Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                            String resp = scanner.hasNext() ? scanner.next() : "";

                            if (code == 200) {
                                String prediction = new JSONObject(resp)
                                        .getJSONArray("choices")
                                        .getJSONObject(0)
                                        .getJSONObject("message")
                                        .getString("content")
                                        .replace("**", "").replace("*", "");

                                // Lưu vào Firestore
                                Map<String, Object> update = new HashMap<>();
                                update.put("aiPrediction", prediction);
                                db.collection(COLLECTION).document(match.getMatchId())
                                        .update(update)
                                        .addOnSuccessListener(v ->
                                                runOnUiThread(() -> Toast.makeText(this,
                                                        "Đã tạo dự đoán!", Toast.LENGTH_SHORT).show()))
                                        .addOnFailureListener(e ->
                                                runOnUiThread(() -> Toast.makeText(this,
                                                        "Lỗi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
                            } else {
                                runOnUiThread(() -> Toast.makeText(this,
                                        "Lỗi AI HTTP " + code, Toast.LENGTH_SHORT).show());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Prediction error", e);
                            runOnUiThread(() -> Toast.makeText(this,
                                    "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Không tải được lịch sử: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
    protected void onDestroy() {
        super.onDestroy();
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
