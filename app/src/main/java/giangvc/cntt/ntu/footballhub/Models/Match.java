package giangvc.cntt.ntu.footballhub.Models;

public class Match {
    private String matchId;
    private String tournamentId;
    private String tournamentName;
    private String team1Id;
    private String team1Name;
    private String team2Id;
    private String team2Name;
    private String matchDate;
    private String matchTime;
    private String location;
    private String round;
    private String status;
    private int scoreTeam1;
    private int scoreTeam2;
    private String referee;
    private java.util.List<MatchEvent> events;
    private long matchOrder;
    private Integer penaltyTeam1;
    private Integer penaltyTeam2;
    private String aiPrediction; // Dự đoán AI, lưu Firestore, hiển thị công khai

    public static final String STATUS_UPCOMING = "Sắp diễn ra";
    public static final String STATUS_ONGOING  = "Đang diễn ra";
    public static final String STATUS_FINISHED = "Đã kết thúc";

    public Match() {}

    public Match(String matchId, String tournamentId, String tournamentName,
                 String team1Id, String team1Name, String team2Id, String team2Name,
                 String matchDate, String matchTime, String location,
                 String round, String status, int scoreTeam1, int scoreTeam2) {
        this(matchId, tournamentId, tournamentName, team1Id, team1Name, team2Id, team2Name,
             matchDate, matchTime, location, round, status, scoreTeam1, scoreTeam2, "", new java.util.ArrayList<>(), System.currentTimeMillis());
    }

    public Match(String matchId, String tournamentId, String tournamentName,
                 String team1Id, String team1Name, String team2Id, String team2Name,
                 String matchDate, String matchTime, String location,
                 String round, String status, int scoreTeam1, int scoreTeam2,
                 String referee, java.util.List<MatchEvent> events, long matchOrder) {
        this.matchId = matchId;
        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
        this.team1Id = team1Id;
        this.team1Name = team1Name;
        this.team2Id = team2Id;
        this.team2Name = team2Name;
        this.matchDate = matchDate;
        this.matchTime = matchTime;
        this.location = location;
        this.round = round;
        this.status = status;
        this.scoreTeam1 = scoreTeam1;
        this.scoreTeam2 = scoreTeam2;
        this.referee = referee;
        this.events = events;
        this.matchOrder = matchOrder;
    }

    public long getMatchOrder() { return matchOrder; }
    public void setMatchOrder(long matchOrder) { this.matchOrder = matchOrder; }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getTournamentId() { return tournamentId; }
    public void setTournamentId(String tournamentId) { this.tournamentId = tournamentId; }

    public String getTournamentName() { return tournamentName; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }

    public String getTeam1Id() { return team1Id; }
    public void setTeam1Id(String team1Id) { this.team1Id = team1Id; }

    public String getTeam1Name() { return team1Name; }
    public void setTeam1Name(String team1Name) { this.team1Name = team1Name; }

    public String getTeam2Id() { return team2Id; }
    public void setTeam2Id(String team2Id) { this.team2Id = team2Id; }

    public String getTeam2Name() { return team2Name; }
    public void setTeam2Name(String team2Name) { this.team2Name = team2Name; }

    public String getMatchDate() { return matchDate; }
    public void setMatchDate(String matchDate) { this.matchDate = matchDate; }

    public String getMatchTime() { return matchTime; }
    public void setMatchTime(String matchTime) { this.matchTime = matchTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRound() { return round; }
    public void setRound(String round) { this.round = round; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getScoreTeam1() { return scoreTeam1; }
    public void setScoreTeam1(int scoreTeam1) { this.scoreTeam1 = scoreTeam1; }

    public int getScoreTeam2() { return scoreTeam2; }
    public void setScoreTeam2(int scoreTeam2) { this.scoreTeam2 = scoreTeam2; }

    public String getReferee() { return referee; }
    public void setReferee(String referee) { this.referee = referee; }

    public java.util.List<MatchEvent> getEvents() { return events; }
    public void setEvents(java.util.List<MatchEvent> events) { this.events = events; }

    public Integer getPenaltyTeam1() { return penaltyTeam1; }
    public void setPenaltyTeam1(Integer penaltyTeam1) { this.penaltyTeam1 = penaltyTeam1; }

    public Integer getPenaltyTeam2() { return penaltyTeam2; }
    public void setPenaltyTeam2(Integer penaltyTeam2) { this.penaltyTeam2 = penaltyTeam2; }

    public String getAiPrediction() { return aiPrediction; }
    public void setAiPrediction(String aiPrediction) { this.aiPrediction = aiPrediction; }
}
