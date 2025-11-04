package group10.persistence;

public class PlayerStat {
    private String username;
    private double totalPoints;
    private int wins;
    private int losses;
    private int draws;
    private int matchesPlayed;

    public PlayerStat(String username, double totalPoints, int wins, int losses, int draws, int matchesPlayed) {
        this.username = username;
        this.totalPoints = totalPoints;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.matchesPlayed = matchesPlayed;
    }

    // 🧩 Getter & Setter
    public String getUsername() {
        return username;
    }

    public double getTotalPoints() {
        return totalPoints;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getDraws() {
        return draws;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    // ✅ Dễ debug hơn
    @Override
    public String toString() {
        return String.format("%s | %.2f điểm | %dW-%dL-%dD | %d trận",
                username, totalPoints, wins, losses, draws, matchesPlayed);
    }
}
