package giangvc.cntt.ntu.footballhub.Models;

import java.util.List;

public class Tournament {

    public static final String FORMAT_KNOCKOUT = "KNOCKOUT";
    public static final String FORMAT_ROUND_ROBIN = "ROUND_ROBIN";

    public static final String STATUS_PENDING_DRAW = "PENDING_DRAW";
    public static final String STATUS_UPCOMING = "UPCOMING";
    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_FINISHED = "FINISHED";

    private String tournamentId;
    private String tournamentName;
    private String format;
    private String status;
    private int maxTeams;
    private int currentTeams;
    private String startDate;
    private String endDate;
    private int matchesPerDay;
    private String description;
    private List<String> teamIds;
    private List<String> teamNames;

    public Tournament() {
        // Required for Firestore
    }

    public Tournament(String tournamentId, String tournamentName, String format, String status,
                      int maxTeams, int currentTeams, String startDate, String endDate,
                      int matchesPerDay, String description, List<String> teamIds, List<String> teamNames) {
        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
        this.format = format;
        this.status = status;
        this.maxTeams = maxTeams;
        this.currentTeams = currentTeams;
        this.startDate = startDate;
        this.endDate = endDate;
        this.matchesPerDay = matchesPerDay;
        this.description = description;
        this.teamIds = teamIds;
        this.teamNames = teamNames;
    }

    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormatLabel() {
        if (FORMAT_KNOCKOUT.equals(format)) {
            return "Đá loại trực tiếp";
        } else if (FORMAT_ROUND_ROBIN.equals(format)) {
            return "Đá vòng bảng";
        }
        return "Không xác định";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        if (status == null) return "Không xác định";
        switch (status) {
            case STATUS_PENDING_DRAW:
                return "Chờ bốc thăm";
            case STATUS_UPCOMING:
                return "Sắp diễn ra";
            case STATUS_ONGOING:
                return "Đang diễn ra";
            case STATUS_FINISHED:
                return "Đã kết thúc";
            default:
                return "Không xác định";
        }
    }

    public int getMaxTeams() {
        return maxTeams;
    }

    public void setMaxTeams(int maxTeams) {
        this.maxTeams = maxTeams;
    }

    public int getCurrentTeams() {
        return currentTeams;
    }

    public void setCurrentTeams(int currentTeams) {
        this.currentTeams = currentTeams;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getMatchesPerDay() {
        return matchesPerDay;
    }

    public void setMatchesPerDay(int matchesPerDay) {
        this.matchesPerDay = matchesPerDay;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTeamIds() {
        return teamIds;
    }

    public void setTeamIds(List<String> teamIds) {
        this.teamIds = teamIds;
    }

    public List<String> getTeamNames() {
        return teamNames;
    }

    public void setTeamNames(List<String> teamNames) {
        this.teamNames = teamNames;
    }
}
