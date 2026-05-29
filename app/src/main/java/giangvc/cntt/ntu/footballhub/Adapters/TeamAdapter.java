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
 * TeamAdapter binds a list of {@link Team} objects to item_team.xml views
 * inside the RecyclerView on TeamManagementActivity.
 *
 * An optional {@link OnTeamClickListener} callback lets the host Activity
 * react to item taps (e.g., open a detail/edit screen).
 */
public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.TeamViewHolder> {

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<Team> teamList;

    // ── Click callback interface ───────────────────────────────────────────────
    public interface OnTeamClickListener {
        void onTeamClick(Team team);
    }

    private OnTeamClickListener listener;

    // ── Constructor ───────────────────────────────────────────────────────────

    public TeamAdapter(List<Team> teamList) {
        this.teamList = teamList;
    }

    /** Optional: attach a click listener from the Activity */
    public void setOnTeamClickListener(OnTeamClickListener listener) {
        this.listener = listener;
    }

    // ── RecyclerView.Adapter overrides ────────────────────────────────────────

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        Team team = teamList.get(position);
        holder.bind(team);
    }

    @Override
    public int getItemCount() {
        return teamList.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    class TeamViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTeamName;
        private final TextView tvCoachName;

        TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTeamName  = itemView.findViewById(R.id.tvTeamName);
            tvCoachName = itemView.findViewById(R.id.tvCoachName);
        }

        void bind(Team team) {
            tvTeamName.setText(team.getTeamName());
            tvCoachName.setText("Coach: " + team.getCoachName());

            // Forward click events to the optional listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTeamClick(team);
                }
            });
        }
    }
}
