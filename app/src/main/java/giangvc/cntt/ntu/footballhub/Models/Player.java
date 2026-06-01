package giangvc.cntt.ntu.footballhub.Models;

/**
 * Data model for a Player belonging to a Team.
 * The empty constructor is required for Firebase Firestore deserialization.
 */
public class Player {

    private String playerId;
    private String teamId;
    private String playerName;
    private String playerClass;
    private String studentId;
    private String phone;
    private String email;
    private int jerseyNumber;
    private String position;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Empty constructor required by Firestore */
    public Player() {}

    /** Full parameterized constructor */
    public Player(String playerId, String teamId, String playerName, String playerClass,
                  String studentId, String phone, String email, int jerseyNumber, String position) {
        this.playerId    = playerId;
        this.teamId      = teamId;
        this.playerName  = playerName;
        this.playerClass = playerClass;
        this.studentId   = studentId;
        this.phone       = phone;
        this.email       = email;
        this.jerseyNumber = jerseyNumber;
        this.position    = position;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getPlayerClass() { return playerClass; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getJerseyNumber() { return jerseyNumber; }
    public void setJerseyNumber(int jerseyNumber) { this.jerseyNumber = jerseyNumber; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
}
