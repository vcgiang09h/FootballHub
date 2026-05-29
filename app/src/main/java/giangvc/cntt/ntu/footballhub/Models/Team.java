package giangvc.cntt.ntu.footballhub.Models;

/**
 * Data model for a Football Team.
 * The empty constructor is required for Firebase Firestore deserialization.
 */
public class Team {

    private String teamId;
    private String teamName;
    private String coachName;
    private String logoUrl;
    private int matchesPlayed;
    private int points;
    private int goalDifference;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Empty constructor required by Firestore */
    public Team() {}

    /** Full parameterized constructor */
    public Team(String teamId, String teamName, String coachName,
                String logoUrl, int matchesPlayed, int points, int goalDifference) {
        this.teamId         = teamId;
        this.teamName       = teamName;
        this.coachName      = coachName;
        this.logoUrl        = logoUrl;
        this.matchesPlayed  = matchesPlayed;
        this.points         = points;
        this.goalDifference = goalDifference;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getTeamId()                    { return teamId; }
    public void   setTeamId(String teamId)       { this.teamId = teamId; }

    public String getTeamName()                  { return teamName; }
    public void   setTeamName(String teamName)   { this.teamName = teamName; }

    public String getCoachName()                 { return coachName; }
    public void   setCoachName(String coachName) { this.coachName = coachName; }

    public String getLogoUrl()                   { return logoUrl; }
    public void   setLogoUrl(String logoUrl)     { this.logoUrl = logoUrl; }

    public int  getMatchesPlayed()                      { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed)     { this.matchesPlayed = matchesPlayed; }

    public int  getPoints()                  { return points; }
    public void setPoints(int points)        { this.points = points; }

    public int  getGoalDifference()                     { return goalDifference; }
    public void setGoalDifference(int goalDifference)   { this.goalDifference = goalDifference; }
}
