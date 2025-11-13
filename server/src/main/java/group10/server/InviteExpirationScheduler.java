package group10.server;

import group10.common.Invite;
import group10.persistence.InviteDAO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

/**
 * InviteExpirationScheduler
 * Định kỳ kiểm tra và mark expired invites
 */
@Slf4j
public class InviteExpirationScheduler {
    private final MatchmakingManager matchmakingManager;
    private final InviteDAO inviteDAO;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    private static final long CHECK_INTERVAL_MS = 5000; // Check every 5 seconds

    public InviteExpirationScheduler(MatchmakingManager matchmakingManager) {
        this.matchmakingManager = matchmakingManager;
        this.inviteDAO = new InviteDAO();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start the scheduler
     */
    public void start() {
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                checkExpiredInvites();
            } catch (Exception e) {
                log.error("Error checking expired invites", e);
            }
        }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.info("InviteExpirationScheduler started (check interval: {}ms)", CHECK_INTERVAL_MS);
    }

    /**
     * Check and handle expired invites
     */
    private void checkExpiredInvites() {
        // Get all expired invites
        List<Invite> expiredInvites = inviteDAO.getExpiredInvites();

        if (!expiredInvites.isEmpty()) {
            log.debug("Found {} expired invites", expiredInvites.size());

            // Mark them as expired in database
            inviteDAO.markExpiredInvites();

            // Notify players
            matchmakingManager.handleExpiredInvites(expiredInvites);
        }
    }

    /**
     * Stop the scheduler
     */
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }

        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("InviteExpirationScheduler stopped");
    }
}