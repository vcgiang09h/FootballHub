package giangvc.cntt.ntu.footballhub.Adapters;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import giangvc.cntt.ntu.footballhub.Models.Match;
import giangvc.cntt.ntu.footballhub.R;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    private List<Match> matchList;
    private OnMatchClickListener listener;
    private OnPredictClickListener predictListener;
    private boolean showPredictButton = false;

    public interface OnMatchClickListener {
        void onMatchClick(Match match);
    }

    public interface OnPredictClickListener {
        void onPredictClick(Match match);
    }

    public MatchAdapter(List<Match> matchList) {
        this.matchList = matchList;
    }

    public void setOnMatchClickListener(OnMatchClickListener listener) {
        this.listener = listener;
    }

    /** Gọi từ Activity admin để bật nút dự đoán (nhấn giữ card) */
    public void setOnPredictClickListener(OnPredictClickListener listener) {
        this.predictListener = listener;
        this.showPredictButton = true;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        holder.bind(matchList.get(position));
    }

    @Override
    public int getItemCount() {
        return matchList != null ? matchList.size() : 0;
    }

    // ── Parse structured AI response ────────────────────────────────────────
    /** Trích xuất số nguyên từ dòng có dạng "WIN:65" hoặc "WIN: 65%" */
    private static int parsePct(String raw, String key) {
        try {
            Pattern p = Pattern.compile(key + "\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(raw);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return -1;
    }

    /** Trích xuất phần phân tích sau "ANALYSIS:" */
    private static String parseAnalysis(String raw) {
        try {
            int idx = raw.indexOf("ANALYSIS:");
            if (idx >= 0) return raw.substring(idx + 9).trim();
            idx = raw.indexOf("PHÂN TÍCH:");
            if (idx >= 0) return raw.substring(idx + 10).trim();
        } catch (Exception ignored) {}
        return raw.trim(); // fallback: trả toàn bộ
    }

    /** Set layout_weight và text cho 1 đoạn thanh ngang */
    private static void setBarWeight(TextView tv, int weight, String label) {
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) tv.getLayoutParams();
        p.weight = Math.max(weight, 1); // weight min 1 để không biến mất
        tv.setLayoutParams(p);
        tv.setText(weight >= 10 ? label : ""); // ẩn text nếu đoạn quá nhỏ
    }

    // ── ViewHolder ───────────────────────────────────────────────────────────
    class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView tvTournament, tvStatus, tvTeam1, tvTeam2, tvScore1, tvScore2,
                 tvDateTime, tvLocation, tvRound, tvPenaltyScore;
        TextView tvAiTeam1Name, tvAiTeam2Name;
        TextView tvAiT1WinBar, tvAiT1DrawBar, tvAiT1LoseBar;
        TextView tvAiT2WinBar, tvAiT2DrawBar, tvAiT2LoseBar;
        LinearLayout layoutEventsSummary, layoutItemTeam1Events, layoutItemTeam2Events;
        LinearLayout layoutAiPrediction;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTournament      = itemView.findViewById(R.id.tvMatchTournament);
            tvStatus          = itemView.findViewById(R.id.tvMatchStatus);
            tvTeam1           = itemView.findViewById(R.id.tvTeam1Name);
            tvTeam2           = itemView.findViewById(R.id.tvTeam2Name);
            tvScore1          = itemView.findViewById(R.id.tvScore1);
            tvScore2          = itemView.findViewById(R.id.tvScore2);
            tvDateTime        = itemView.findViewById(R.id.tvMatchDateTime);
            tvLocation        = itemView.findViewById(R.id.tvMatchLocation);
            tvRound           = itemView.findViewById(R.id.tvMatchRound);
            tvPenaltyScore    = itemView.findViewById(R.id.tvPenaltyScore);

            tvAiTeam1Name     = itemView.findViewById(R.id.tvAiTeam1Name);
            tvAiTeam2Name     = itemView.findViewById(R.id.tvAiTeam2Name);
            tvAiT1WinBar      = itemView.findViewById(R.id.tvAiT1WinBar);
            tvAiT1DrawBar     = itemView.findViewById(R.id.tvAiT1DrawBar);
            tvAiT1LoseBar     = itemView.findViewById(R.id.tvAiT1LoseBar);
            tvAiT2WinBar      = itemView.findViewById(R.id.tvAiT2WinBar);
            tvAiT2DrawBar     = itemView.findViewById(R.id.tvAiT2DrawBar);
            tvAiT2LoseBar     = itemView.findViewById(R.id.tvAiT2LoseBar);
            layoutAiPrediction = itemView.findViewById(R.id.layoutAiPrediction);

            layoutEventsSummary   = itemView.findViewById(R.id.layoutEventsSummary);
            layoutItemTeam1Events = itemView.findViewById(R.id.layoutItemTeam1Events);
            layoutItemTeam2Events = itemView.findViewById(R.id.layoutItemTeam2Events);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null)
                    listener.onMatchClick(matchList.get(pos));
            });
        }

        public void bind(Match match) {
            tvTournament.setText(match.getTournamentName());
            tvStatus.setText(match.getStatus());
            tvTeam1.setText(match.getTeam1Name());
            tvTeam2.setText(match.getTeam2Name());

            // Score / status colors
            if (Match.STATUS_UPCOMING.equals(match.getStatus())) {
                tvScore1.setText("-");
                tvScore2.setText("-");
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8E24AA")));
                tvPenaltyScore.setVisibility(View.GONE);
            } else {
                tvScore1.setText(String.valueOf(match.getScoreTeam1()));
                tvScore2.setText(String.valueOf(match.getScoreTeam2()));
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                        Match.STATUS_ONGOING.equals(match.getStatus())
                                ? Color.parseColor("#43A047")
                                : Color.parseColor("#757575")));

                if (Match.STATUS_FINISHED.equals(match.getStatus()) && match.getPenaltyTeam1() != null && match.getPenaltyTeam2() != null) {
                    tvPenaltyScore.setText("(pen: " + match.getPenaltyTeam1() + "-" + match.getPenaltyTeam2() + ")");
                    tvPenaltyScore.setVisibility(View.VISIBLE);
                } else {
                    tvPenaltyScore.setVisibility(View.GONE);
                }
            }

            String dateTime = (match.getMatchDate() != null ? match.getMatchDate() : "")
                    + " - " + (match.getMatchTime() != null ? match.getMatchTime() : "");
            tvDateTime.setText(dateTime);

            String location = match.getLocation();
            if (location != null && !location.trim().isEmpty()) {
                tvLocation.setText(location);
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }
            tvRound.setText(match.getRound());

            // ── Events summary ─────────────────────────────────────────────
            layoutItemTeam1Events.removeAllViews();
            layoutItemTeam2Events.removeAllViews();
            if (match.getEvents() != null && !match.getEvents().isEmpty()) {
                layoutEventsSummary.setVisibility(View.VISIBLE);
                for (giangvc.cntt.ntu.footballhub.Models.MatchEvent ev : match.getEvents()) {
                    String icon;
                    int textColor;
                    switch (ev.getType()) {
                        case giangvc.cntt.ntu.footballhub.Models.MatchEvent.TYPE_GOAL:
                            icon = "⚽"; textColor = Color.parseColor("#2E7D32"); break;
                        case giangvc.cntt.ntu.footballhub.Models.MatchEvent.TYPE_OWN_GOAL:
                            icon = "⚽"; textColor = Color.parseColor("#D32F2F"); break;
                        case giangvc.cntt.ntu.footballhub.Models.MatchEvent.TYPE_YELLOW:
                            icon = "🟡"; textColor = Color.parseColor("#F9A825"); break;
                        case giangvc.cntt.ntu.footballhub.Models.MatchEvent.TYPE_RED:
                            icon = "🔴"; textColor = Color.parseColor("#C62828"); break;
                        default:
                            icon = "•"; textColor = Color.parseColor("#757575"); break;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(icon).append(" ");
                    if (ev.getPlayerJerseyNumber() > 0) sb.append("#").append(ev.getPlayerJerseyNumber()).append(" ");
                    sb.append(ev.getPlayerName());
                    if (giangvc.cntt.ntu.footballhub.Models.MatchEvent.TYPE_OWN_GOAL.equals(ev.getType())) {
                        sb.append(" (OG)");
                    }
                    if (ev.getMinute() > 0) sb.append(" (").append(ev.getMinute()).append("')");

                    TextView tvEv = new TextView(itemView.getContext());
                    tvEv.setText(sb.toString());
                    tvEv.setTextSize(11);
                    tvEv.setTextColor(textColor);
                    tvEv.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvEv.setPadding(0, 1, 0, 1);

                    boolean isTeam1 = ev.getTeamId().equals(match.getTeam1Id());
                    if (giangvc.cntt.ntu.footballhub.Models.MatchEvent.TYPE_OWN_GOAL.equals(ev.getType())) {
                        isTeam1 = !isTeam1;
                    }

                    if (isTeam1) {
                        tvEv.setGravity(android.view.Gravity.END);
                        layoutItemTeam1Events.addView(tvEv);
                    } else {
                        tvEv.setGravity(android.view.Gravity.START);
                        layoutItemTeam2Events.addView(tvEv);
                    }
                }
            } else {
                layoutEventsSummary.setVisibility(View.GONE);
            }

            // ── AI Prediction ───────────────────────────────────────────────
            String raw = match.getAiPrediction();
            if (raw != null && !raw.trim().isEmpty()) {
                int win  = parsePct(raw, "WIN");
                int draw = parsePct(raw, "DRAW");
                int lose = parsePct(raw, "LOSE");
                String analysis = parseAnalysis(raw);

                if (win >= 0 && draw >= 0 && lose >= 0) {
                    tvAiTeam1Name.setText(match.getTeam1Name());
                    tvAiTeam2Name.setText(match.getTeam2Name());

                    // Đội 1: tỷ lệ lấy nguyên từ kết quả AI
                    setBarWeight(tvAiT1WinBar,  win,  win + "%");
                    setBarWeight(tvAiT1DrawBar, draw, draw + "%");
                    setBarWeight(tvAiT1LoseBar, lose, lose + "%");

                    // Đội 2: đổi ngược tỷ lệ Thắng/Thua của Đội 1
                    setBarWeight(tvAiT2WinBar,  lose, lose + "%");
                    setBarWeight(tvAiT2DrawBar, draw, draw + "%");
                    setBarWeight(tvAiT2LoseBar, win,  win + "%");
                } else {
                    tvAiTeam1Name.setText(match.getTeam1Name());
                    tvAiTeam2Name.setText(match.getTeam2Name());

                    setBarWeight(tvAiT1WinBar,  34, "?");
                    setBarWeight(tvAiT1DrawBar, 33, "?");
                    setBarWeight(tvAiT1LoseBar, 33, "?");

                    setBarWeight(tvAiT2WinBar,  34, "?");
                    setBarWeight(tvAiT2DrawBar, 33, "?");
                    setBarWeight(tvAiT2LoseBar, 33, "?");
                }

                layoutAiPrediction.setVisibility(View.VISIBLE);

                // Long press → Dialog chi tiết
                final String finalAnalysis = analysis;
                final String t1 = match.getTeam1Name();
                final String t2 = match.getTeam2Name();
                layoutAiPrediction.setOnLongClickListener(v -> {
                    String title = "Phân tích: " + t1 + " vs " + t2;
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle(title)
                            .setMessage(finalAnalysis)
                            .setPositiveButton("Đóng", null)
                            .show();
                    return true;
                });

            } else {
                layoutAiPrediction.setVisibility(View.GONE);
                layoutAiPrediction.setOnLongClickListener(null);
            }

            // Admin: nhấn giữ card để tạo hoặc xóa dự đoán (chỉ cho trận Sắp diễn ra)
            if (showPredictButton && Match.STATUS_UPCOMING.equals(match.getStatus())) {
                itemView.setOnLongClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && predictListener != null)
                        predictListener.onPredictClick(matchList.get(pos));
                    return true;
                });
            } else {
                itemView.setOnLongClickListener(null);
            }
        }
    }
}
