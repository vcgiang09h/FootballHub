package giangvc.cntt.ntu.footballhub.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import giangvc.cntt.ntu.footballhub.Models.Player;
import giangvc.cntt.ntu.footballhub.R;

/**
 * PlayerAdapter — Bind Player model vào item_player.xml
 *  - Số áo làm badge avatar
 *  - Tên cầu thủ, vị trí, số bàn thắng
 *  - Click → mở chi tiết / sửa
 *  - Long-click → callback xóa nhanh
 */
public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {

    private final List<Player> playerList;

    // ── Click callbacks ────────────────────────────────────────────────────────
    public interface OnPlayerClickListener {
        void onPlayerClick(Player player);
    }

    public interface OnPlayerLongClickListener {
        void onPlayerLongClick(Player player);
    }

    private OnPlayerClickListener     clickListener;
    private OnPlayerLongClickListener longClickListener;

    public PlayerAdapter(List<Player> playerList) {
        this.playerList = playerList;
    }

    public void setOnPlayerClickListener(OnPlayerClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnPlayerLongClickListener(OnPlayerLongClickListener listener) {
        this.longClickListener = listener;
    }

    // ── Adapter overrides ──────────────────────────────────────────────────────

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        holder.bind(playerList.get(position));
    }

    @Override
    public int getItemCount() {
        return playerList.size();
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    class PlayerViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvPlayerInitial;
        private final TextView tvPlayerName;
        private final TextView tvPlayerClass;
        private final TextView tvPlayerJerseyPosition;

        PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlayerInitial = itemView.findViewById(R.id.tvPlayerInitial);
            tvPlayerName    = itemView.findViewById(R.id.tvPlayerName);
            tvPlayerClass   = itemView.findViewById(R.id.tvPlayerClass);
            tvPlayerJerseyPosition = itemView.findViewById(R.id.tvPlayerJerseyPosition);
        }

        void bind(Player player) {
            // Nếu có số áo → hiển thị số áo, ngược lại hiển thị chữ cái đầu
            if (player.getJerseyNumber() > 0) {
                tvPlayerInitial.setText(String.valueOf(player.getJerseyNumber()));
            } else {
                String initial = (player.getPlayerName() != null && !player.getPlayerName().isEmpty())
                        ? String.valueOf(player.getPlayerName().charAt(0)).toUpperCase()
                        : "?";
                tvPlayerInitial.setText(initial);
            }
            tvPlayerName.setText(player.getPlayerName());
            tvPlayerClass.setText("Chi đoàn: " + player.getPlayerClass());

            String jerseyStr = player.getJerseyNumber() > 0 ? String.valueOf(player.getJerseyNumber()) : "--";
            String positionStr = (player.getPosition() != null && !player.getPosition().isEmpty()) ? player.getPosition() : "--";
            tvPlayerJerseyPosition.setText("Số áo: " + jerseyStr + " | Vị trí: " + positionStr);

            // Click → mở PlayerDetailActivity (edit/delete)
            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onPlayerClick(player);
            });

            // Long-click → xóa nhanh
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onPlayerLongClick(player);
                return true;
            });
        }
    }
}
