package group10.client;

import java.util.List;

public interface GameCallbacks {
    void onStartRound(String matchId,
                      int roundId,
                      List<String> items,
                      String order,
                      long deadlineLocalDisplayMs,
                      long serverStartTimeMs,
                      long clockOffsetMs);

    void onRoundResult(int roundId,
                       boolean correct,
                       int myRoundScore,
                       int oppRoundScore,
                       long myTotalScore,
                       long oppTotalScore,
                       String explanation);

    void onScoreUpdate(long myScore, long oppScore);

    void onGameOver(String result, String message);

    void onDisconnected(String reason);
}
