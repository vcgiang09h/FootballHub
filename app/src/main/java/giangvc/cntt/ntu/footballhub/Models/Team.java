package giangvc.cntt.ntu.footballhub.Models;

/**
 * Data model for a Football Team.
 * The empty constructor is required for Firebase Firestore deserialization.
 */
public class Team {

    private String teamId;
    private String teamName;
    private String teamClass;
    private String captainName;
    private String captainClass;
    private String captainStudentId;
    private String captainPhone;
    private String captainEmail;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Empty constructor required by Firestore */
    public Team() {}

    /** Full parameterized constructor */
    public Team(String teamId, String teamName, String teamClass, String captainName,
                String captainClass, String captainStudentId, String captainPhone, String captainEmail) {
        this.teamId           = teamId;
        this.teamName         = teamName;
        this.teamClass        = teamClass;
        this.captainName      = captainName;
        this.captainClass     = captainClass;
        this.captainStudentId = captainStudentId;
        this.captainPhone     = captainPhone;
        this.captainEmail     = captainEmail;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getTeamClass() { return teamClass; }
    public void setTeamClass(String teamClass) { this.teamClass = teamClass; }

    public String getCaptainName() { return captainName; }
    public void setCaptainName(String captainName) { this.captainName = captainName; }

    public String getCaptainClass() { return captainClass; }
    public void setCaptainClass(String captainClass) { this.captainClass = captainClass; }

    public String getCaptainStudentId() { return captainStudentId; }
    public void setCaptainStudentId(String captainStudentId) { this.captainStudentId = captainStudentId; }

    public String getCaptainPhone() { return captainPhone; }
    public void setCaptainPhone(String captainPhone) { this.captainPhone = captainPhone; }

    public String getCaptainEmail() { return captainEmail; }
    public void setCaptainEmail(String captainEmail) { this.captainEmail = captainEmail; }
}
