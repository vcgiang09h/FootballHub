package giangvc.cntt.ntu.footballhub.Models;

public class MatchEvent {
    public static final String TYPE_GOAL = "Bàn thắng";
    public static final String TYPE_YELLOW = "Thẻ vàng";
    public static final String TYPE_RED = "Thẻ đỏ";

    private String id;
    private String type;
    private String teamId;
    private String teamName;
    private String playerId;
    private String playerName;
    private int minute;

    public MatchEvent() {}

    public MatchEvent(String id, String type, String teamId, String teamName, 
                      String playerId, String playerName, int minute) {
        this.id = id;
        this.type = type;
        this.teamId = teamId;
        this.teamName = teamName;
        this.playerId = playerId;
        this.playerName = playerName;
        this.minute = minute;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }
}
