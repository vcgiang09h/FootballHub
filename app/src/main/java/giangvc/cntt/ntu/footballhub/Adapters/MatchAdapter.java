package giangvc.cntt.ntu.footballhub.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import giangvc.cntt.ntu.footballhub.Models.Match;
import giangvc.cntt.ntu.footballhub.R;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    private List<Match> matchList;
    private OnMatchClickListener listener;

    public interface OnMatchClickListener {
        void onMatchClick(Match match);
    }

    public MatchAdapter(List<Match> matchList) {
        this.matchList = matchList;
    }

    public void setOnMatchClickListener(OnMatchClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matchList.get(position);
        holder.bind(match);
    }

    @Override
    public int getItemCount() {
        return matchList != null ? matchList.size() : 0;
    }

    class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView tvTournament, tvStatus, tvTeam1, tvTeam2, tvScore1, tvScore2, tvDateTime, tvLocation, tvRound;
        LinearLayout layoutEventsSummary, layoutItemTeam1Events, layoutItemTeam2Events;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTournament = itemView.findViewById(R.id.tvMatchTournament);
            tvStatus     = itemView.findViewById(R.id.tvMatchStatus);
            tvTeam1      = itemView.findViewById(R.id.tvTeam1Name);
            tvTeam2      = itemView.findViewById(R.id.tvTeam2Name);
            tvScore1     = itemView.findViewById(R.id.tvScore1);
            tvScore2     = itemView.findViewById(R.id.tvScore2);
            tvDateTime   = itemView.findViewById(R.id.tvMatchDateTime);
            tvLocation   = itemView.findViewById(R.id.tvMatchLocation);
            tvRound      = itemView.findViewById(R.id.tvMatchRound);
            
            layoutEventsSummary   = itemView.findViewById(R.id.layoutEventsSummary);
            layoutItemTeam1Events = itemView.findViewById(R.id.layoutItemTeam1Events);
            layoutItemTeam2Events = itemView.findViewById(R.id.layoutItemTeam2Events);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMatchClick(matchList.get(position));
                }
            });
        }

        public void bind(Match match) {
            tvTournament.setText(match.getTournamentName());
            tvStatus.setText(match.getStatus());
            tvTeam1.setText(match.getTeam1Name());
            tvTeam2.setText(match.getTeam2Name());
            
            if (Match.STATUS_UPCOMING.equals(match.getStatus())) {
                tvScore1.setText("-");
                tvScore2.setText("-");
                tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#8E24AA")));
            } else {
                tvScore1.setText(String.valueOf(match.getScoreTeam1()));
                tvScore2.setText(String.valueOf(match.getScoreTeam2()));
                if (Match.STATUS_ONGOING.equals(match.getStatus())) {
                    tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#43A047"))); // Green
                } else {
                    tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#757575"))); // Grey
                }
            }

            String dateTime = "📅 " + (match.getMatchDate() != null ? match.getMatchDate() : "") + 
                              " - " + (match.getMatchTime() != null ? match.getMatchTime() : "");
            tvDateTime.setText(dateTime);
            
            String location = match.getLocation();
            if (location != null && !location.trim().isEmpty()) {
                tvLocation.setText("📍 " + location);
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }
            
            tvRound.setText(match.getRound());
            
            // Populate events summary
            layoutItemTeam1Events.removeAllViews();
            layoutItemTeam2Events.removeAllViews();
            
            if (match.getEvents() != null && !match.getEvents().isEmpty()) {
                layoutEventsSummary.setVisibility(View.VISIBLE);
                for (giangvc.cntt.ntu.footballhub.Models.MatchEvent event : match.getEvents()) {
                    String icon;
                    int textColor;
                    switch (event.getType()) {
                        case giangvc.cntt.ntu.footballhub.Models.MatchEvent.TYPE_GOAL:
                            icon = "⚽";
                            textColor = Color.parseColor("#2E7D32");
                            break;
                        case giangvc.cntt.ntu.footballhub.Models.MatchEvent.TYPE_YELLOW:
                            icon = "🟡";
                            textColor = Color.parseColor("#F9A825");
                            break;
                        case giangvc.cntt.ntu.footballhub.Models.MatchEvent.TYPE_RED:
                            icon = "🔴";
                            textColor = Color.parseColor("#C62828");
                            break;
                        default:
                            icon = "📌";
                            textColor = Color.parseColor("#757575");
                            break;
                    }
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(icon).append(" ");
                    if (event.getPlayerJerseyNumber() > 0) sb.append("#").append(event.getPlayerJerseyNumber()).append(" ");
                    sb.append(event.getPlayerName());
                    if (event.getMinute() > 0) sb.append(" (").append(event.getMinute()).append("')");
                    
                    TextView tvEvent = new TextView(itemView.getContext());
                    tvEvent.setText(sb.toString());
                    tvEvent.setTextSize(11);
                    tvEvent.setTextColor(textColor);
                    tvEvent.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvEvent.setPadding(0, 1, 0, 1);
                    
                    if (event.getTeamId().equals(match.getTeam1Id())) {
                        tvEvent.setGravity(android.view.Gravity.END);
                        layoutItemTeam1Events.addView(tvEvent);
                    } else if (event.getTeamId().equals(match.getTeam2Id())) {
                        tvEvent.setGravity(android.view.Gravity.START);
                        layoutItemTeam2Events.addView(tvEvent);
                    }
                }
            } else {
                layoutEventsSummary.setVisibility(View.GONE);
            }
        }
    }
}
