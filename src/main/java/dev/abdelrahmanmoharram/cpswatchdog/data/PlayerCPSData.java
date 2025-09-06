package dev.abdelrahmanmoharram.cpswatchdog.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class PlayerCPSData {
    private final String playerName;
    private final int maxStoredClicks;

    // Separate tracking for left and right clicks
    private final Deque<Long> leftClickTimes;
    private final Deque<Long> rightClickTimes;
    private final List<Double> recentLeftCPSValues;
    private final List<Double> recentRightCPSValues;

    private long lastClickTime;
    private long lastLeftClickTime;
    private long lastRightClickTime;

    private int violationCount;
    private int suspiciousActivityCount;
    private long sessionStartTime;

    private int totalClicks;
    private int totalLeftClicks;
    private int totalRightClicks;

    private static final int CONSISTENCY_WINDOW = 10; // Track last 10 CPS values for consistency

    // Constructor with maxStoredClicks parameter
    public PlayerCPSData(String playerName, int maxStoredClicks) {
        this.playerName = playerName;
        this.maxStoredClicks = maxStoredClicks;
        this.leftClickTimes = new ArrayDeque<>();
        this.rightClickTimes = new ArrayDeque<>();
        this.recentLeftCPSValues = new ArrayList<>();
        this.recentRightCPSValues = new ArrayList<>();
        this.sessionStartTime = System.currentTimeMillis();
        this.violationCount = 0;
        this.suspiciousActivityCount = 0;
        this.totalClicks = 0;
        this.totalLeftClicks = 0;
        this.totalRightClicks = 0;
    }

    // Backward compatibility constructor (uses default max stored clicks)
    public PlayerCPSData(String playerName) {
        this(playerName, 20); // Default to 20 if not specified
    }

    public void addLeftClick() {
        long currentTime = System.currentTimeMillis();
        leftClickTimes.offer(currentTime);
        lastClickTime = currentTime;
        lastLeftClickTime = currentTime;
        totalClicks++;
        totalLeftClicks++;

        // Keep only recent clicks for CPS calculation using configured max
        while (leftClickTimes.size() > maxStoredClicks) {
            leftClickTimes.poll();
        }

        // Update CPS history for consistency checking
        double currentLeftCPS = getCurrentLeftCPS();
        recentLeftCPSValues.add(currentLeftCPS);
        if (recentLeftCPSValues.size() > CONSISTENCY_WINDOW) {
            recentLeftCPSValues.remove(0);
        }
    }

    public void addRightClick() {
        long currentTime = System.currentTimeMillis();
        rightClickTimes.offer(currentTime);
        lastClickTime = currentTime;
        lastRightClickTime = currentTime;
        totalClicks++;
        totalRightClicks++;

        // Keep only recent clicks for CPS calculation using configured max
        while (rightClickTimes.size() > maxStoredClicks) {
            rightClickTimes.poll();
        }

        // Update CPS history for consistency checking
        double currentRightCPS = getCurrentRightCPS();
        recentRightCPSValues.add(currentRightCPS);
        if (recentRightCPSValues.size() > CONSISTENCY_WINDOW) {
            recentRightCPSValues.remove(0);
        }
    }

    // Legacy method for backward compatibility
    public void addClick() {
        addLeftClick(); // Default to left-click for backward compatibility
    }

    public double getCurrentLeftCPS() {
        if (leftClickTimes.size() < 2) {
            return 0.0;
        }

        long timeWindow = lastLeftClickTime - leftClickTimes.peek();
        if (timeWindow <= 0) {
            return 0.0;
        }

        return (leftClickTimes.size() - 1) * 1000.0 / timeWindow;
    }

    public double getCurrentRightCPS() {
        if (rightClickTimes.size() < 2) {
            return 0.0;
        }

        long timeWindow = lastRightClickTime - rightClickTimes.peek();
        if (timeWindow <= 0) {
            return 0.0;
        }

        return (rightClickTimes.size() - 1) * 1000.0 / timeWindow;
    }

    // Combined CPS (for backward compatibility)
    public double getCurrentCPS() {
        return Math.max(getCurrentLeftCPS(), getCurrentRightCPS());
    }

    public double getAverageLeftCPS() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        if (sessionDuration <= 0 || totalLeftClicks == 0) {
            return 0.0;
        }
        return totalLeftClicks * 1000.0 / sessionDuration;
    }

    public double getAverageRightCPS() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        if (sessionDuration <= 0 || totalRightClicks == 0) {
            return 0.0;
        }
        return totalRightClicks * 1000.0 / sessionDuration;
    }

    public double getAverageCPS() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        if (sessionDuration <= 0 || totalClicks == 0) {
            return 0.0;
        }
        return totalClicks * 1000.0 / sessionDuration;
    }

    public double getMaxLeftCPS() {
        return recentLeftCPSValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    public double getMaxRightCPS() {
        return recentRightCPSValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    public double getMaxCPS() {
        return Math.max(getMaxLeftCPS(), getMaxRightCPS());
    }

    public boolean isLeftClickPerfectlyConsistent(double varianceThreshold) {
        if (recentLeftCPSValues.size() < CONSISTENCY_WINDOW) {
            return false;
        }
        double variance = calculateLeftVariance();
        return variance < varianceThreshold;
    }

    public boolean isRightClickPerfectlyConsistent(double varianceThreshold) {
        if (recentRightCPSValues.size() < CONSISTENCY_WINDOW) {
            return false;
        }
        double variance = calculateRightVariance();
        return variance < varianceThreshold;
    }

    public boolean isPerfectlyConsistent(double varianceThreshold) {
        return isLeftClickPerfectlyConsistent(varianceThreshold) ||
                isRightClickPerfectlyConsistent(varianceThreshold);
    }

    private double calculateLeftVariance() {
        if (recentLeftCPSValues.isEmpty()) {
            return 0.0;
        }

        double mean = recentLeftCPSValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return recentLeftCPSValues.stream()
                .mapToDouble(cps -> Math.pow(cps - mean, 2))
                .average()
                .orElse(0.0);
    }

    private double calculateRightVariance() {
        if (recentRightCPSValues.isEmpty()) {
            return 0.0;
        }

        double mean = recentRightCPSValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return recentRightCPSValues.stream()
                .mapToDouble(cps -> Math.pow(cps - mean, 2))
                .average()
                .orElse(0.0);
    }

    private double calculateVariance() {
        // Combined variance calculation
        double leftVar = calculateLeftVariance();
        double rightVar = calculateRightVariance();
        return Math.max(leftVar, rightVar);
    }

    // Pattern detection method (referenced in CPSManager but missing)
    public boolean hasDetectedPattern(String clickType, double patternThreshold) {
        List<Double> cpsValues = clickType.equals("left") ? recentLeftCPSValues : recentRightCPSValues;

        if (cpsValues.size() < 5) {
            return false;
        }

        // Simple pattern detection - check for repeated sequences
        int patternCount = 0;
        for (int i = 0; i < cpsValues.size() - 1; i++) {
            double current = cpsValues.get(i);
            double next = cpsValues.get(i + 1);

            // Check if the difference is very small (indicating a pattern)
            if (Math.abs(current - next) < 0.5) {
                patternCount++;
            }
        }

        double patternRatio = (double) patternCount / (cpsValues.size() - 1);
        return patternRatio >= patternThreshold;
    }

    public void incrementViolations() {
        this.violationCount++;
    }

    public void resetViolations() {
        this.violationCount = 0;
    }

    public void incrementSuspiciousActivity() {
        this.suspiciousActivityCount++;
    }

    // Getters
    public String getPlayerName() { return playerName; }
    public int getMaxStoredClicks() { return maxStoredClicks; }
    public long getLastClickTime() { return lastClickTime; }
    public long getLastLeftClickTime() { return lastLeftClickTime; }
    public long getLastRightClickTime() { return lastRightClickTime; }
    public int getViolationCount() { return violationCount; }
    public int getSuspiciousActivityCount() { return suspiciousActivityCount; }
    public int getTotalClicks() { return totalClicks; }
    public int getTotalLeftClicks() { return totalLeftClicks; }
    public int getTotalRightClicks() { return totalRightClicks; }
    public long getSessionDuration() { return System.currentTimeMillis() - sessionStartTime; }
    public double getVariance() { return calculateVariance(); }
    public double getLeftVariance() { return calculateLeftVariance(); }
    public double getRightVariance() { return calculateRightVariance(); }
    public List<Double> getRecentLeftCPSValues() { return new ArrayList<>(recentLeftCPSValues); }
    public List<Double> getRecentRightCPSValues() { return new ArrayList<>(recentRightCPSValues); }
    public List<Double> getRecentCPSValues() { return new ArrayList<>(recentLeftCPSValues); } // Legacy
}