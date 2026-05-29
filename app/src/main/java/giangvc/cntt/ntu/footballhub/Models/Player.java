package giangvc.cntt.ntu.footballhub.Models;

/**
 * Data model for a Player belonging to a Team.
 * The empty constructor is required for Firebase Firestore deserialization.
 */
public class Player {

    private String playerId;
    private String teamId;
    private String playerName;
    private String position;
    private int    jerseyNumber;
    private int    goals;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Empty constructor required by Firestore */
    public Player() {}

    /** Full parameterized constructor */
    public Player(String playerId, String teamId, String playerName,
                  String position, int jerseyNumber, int goals) {
        this.playerId     = playerId;
        this.teamId       = teamId;
        this.playerName   = playerName;
        this.position     = position;
        this.jerseyNumber = jerseyNumber;
        this.goals        = goals;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getPlayerId()                  { return playerId; }
    public void   setPlayerId(String playerId)   { this.playerId = playerId; }

    public String getTeamId()                    { return teamId; }
    public void   setTeamId(String teamId)       { this.teamId = teamId; }

    public String getPlayerName()                        { return playerName; }
    public void   setPlayerName(String playerName)       { this.playerName = playerName; }

    public String getPosition()                  { return position; }
    public void   setPosition(String position)   { this.position = position; }

    public int  getJerseyNumber()                        { return jerseyNumber; }
    public void setJerseyNumber(int jerseyNumber)        { this.jerseyNumber = jerseyNumber; }

    public int  getGoals()               { return goals; }
    public void setGoals(int goals)      { this.goals = goals; }
}
