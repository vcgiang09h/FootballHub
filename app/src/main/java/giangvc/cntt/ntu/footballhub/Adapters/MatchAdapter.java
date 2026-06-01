package giangvc.cntt.ntu.footballhub.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        TextView tvTournament, tvStatus, tvTeam1, tvTeam2, tvScore1, tvScore2, tvDateTime, tvRound;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTournament = itemView.findViewById(R.id.tvMatchTournament);
            tvStatus     = itemView.findViewById(R.id.tvMatchStatus);
            tvTeam1      = itemView.findViewById(R.id.tvTeam1Name);
            tvTeam2      = itemView.findViewById(R.id.tvTeam2Name);
            tvScore1     = itemView.findViewById(R.id.tvScore1);
            tvScore2     = itemView.findViewById(R.id.tvScore2);
            tvDateTime   = itemView.findViewById(R.id.tvMatchDateTime);
            tvRound      = itemView.findViewById(R.id.tvMatchRound);

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
            tvRound.setText(match.getRound());
        }
    }
}
