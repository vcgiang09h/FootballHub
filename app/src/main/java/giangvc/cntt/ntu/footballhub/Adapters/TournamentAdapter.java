package giangvc.cntt.ntu.footballhub.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import giangvc.cntt.ntu.footballhub.Models.Tournament;
import giangvc.cntt.ntu.footballhub.R;

/**
 * TournamentAdapter — bind Tournament vào item_tournament.xml
 * click / long-click callbacks
 */
public class TournamentAdapter extends RecyclerView.Adapter<TournamentAdapter.TournamentViewHolder> {

    private final List<Tournament> tournamentList;

    public interface OnTournamentClickListener {
        void onTournamentClick(Tournament tournament);
    }

    public interface OnTournamentLongClickListener {
        void onTournamentLongClick(Tournament tournament);
    }

    private OnTournamentClickListener     clickListener;
    private OnTournamentLongClickListener longClickListener;

    public TournamentAdapter(List<Tournament> tournamentList) {
        this.tournamentList = tournamentList;
    }

    public void setOnTournamentClickListener(OnTournamentClickListener l)         { this.clickListener = l; }
    public void setOnTournamentLongClickListener(OnTournamentLongClickListener l) { this.longClickListener = l; }

    @NonNull
    @Override
    public TournamentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tournament, parent, false);
        return new TournamentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TournamentViewHolder holder, int position) {
        holder.bind(tournamentList.get(position));
    }

    @Override
    public int getItemCount() { return tournamentList.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    class TournamentViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTournamentName;
        private final TextView tvTournamentFormat;
        private final TextView tvTournamentStatus;
        private final TextView tvTournamentDate;
        private final TextView tvTournamentTeams;

        TournamentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTournamentName   = itemView.findViewById(R.id.tvTournamentName);
            tvTournamentFormat = itemView.findViewById(R.id.tvTournamentFormat);
            tvTournamentStatus = itemView.findViewById(R.id.tvTournamentStatus);
            tvTournamentDate   = itemView.findViewById(R.id.tvTournamentDate);
            tvTournamentTeams  = itemView.findViewById(R.id.tvTournamentTeams);
        }

        void bind(Tournament t) {
            tvTournamentName.setText(t.getTournamentName());
            tvTournamentFormat.setText(t.getFormatLabel());
            tvTournamentStatus.setText(t.getStatusLabel());
            tvTournamentDate.setText(t.getStartDate() != null ? t.getStartDate() : "---");
            tvTournamentTeams.setText(t.getCurrentTeams() + "/" + t.getMaxTeams() + " đội");

            // Màu badge theo trạng thái
            int badgeColor;
            switch (t.getStatus() != null ? t.getStatus() : "") {
                case Tournament.STATUS_ONGOING:  badgeColor = 0xFF2E7D32; break; // xanh lá
                case Tournament.STATUS_FINISHED: badgeColor = 0xFF757575; break; // xám
                default:                         badgeColor = 0xFFE65100; break; // cam (upcoming)
            }
            tvTournamentStatus.setTextColor(badgeColor);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onTournamentClick(t);
            });
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onTournamentLongClick(t);
                return true;
            });
        }
    }
}
