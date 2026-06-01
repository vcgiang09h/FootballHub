package giangvc.cntt.ntu.footballhub.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import giangvc.cntt.ntu.footballhub.Models.Team;
import giangvc.cntt.ntu.footballhub.R;

/**
 * TeamAdapter — v3 (Full CRUD)
 * Bind Team model vào item_team.xml với:
 *  - Chữ cái đầu tên đội làm avatar
 *  - Tên HLV, số trận đã chơi
 *  - Badge điểm số
 *  - Click → mở chi tiết / sửa
 *  - Long-click → callback xóa nhanh
 */
public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.TeamViewHolder> {

    private final List<Team> teamList;

    // ── Click callbacks ────────────────────────────────────────────────────────
    public interface OnTeamClickListener {
        void onTeamClick(Team team);
    }

    public interface OnTeamLongClickListener {
        void onTeamLongClick(Team team);
    }

    private OnTeamClickListener     clickListener;
    private OnTeamLongClickListener longClickListener;

    public TeamAdapter(List<Team> teamList) {
        this.teamList = teamList;
    }

    public void setOnTeamClickListener(OnTeamClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnTeamLongClickListener(OnTeamLongClickListener listener) {
        this.longClickListener = listener;
    }

    // ── Adapter overrides ──────────────────────────────────────────────────────

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        holder.bind(teamList.get(position));
    }

    @Override
    public int getItemCount() {
        return teamList.size();
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    class TeamViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTeamInitial;
        private final TextView tvTeamName;
        private final TextView tvCoachName;

        TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTeamInitial   = itemView.findViewById(R.id.tvTeamInitial);
            tvTeamName      = itemView.findViewById(R.id.tvTeamName);
            tvCoachName     = itemView.findViewById(R.id.tvCoachName);
        }

        void bind(Team team) {
            // Chữ cái đầu tên đội làm avatar
            String initial = (team.getTeamName() != null && !team.getTeamName().isEmpty())
                    ? String.valueOf(team.getTeamName().charAt(0)).toUpperCase()
                    : "?";
            tvTeamInitial.setText(initial);

            tvTeamName.setText(team.getTeamName());
            tvCoachName.setText("Đội trưởng: " + team.getCaptainName());

            // Click → mở TeamDetailActivity (edit/delete)
            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onTeamClick(team);
            });

            // Long-click → callback xóa nhanh
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onTeamLongClick(team);
                return true; // consume event
            });
        }
    }
}
