package com.untitledchasegame;

import com.untitledchasegame.inventory.impl.VotingGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager {

    private final UntitledChaseGame plugin;
    private final BossBarManager bossBarManager;
    private final ScoreboardManager scoreboardManager;
    private final LocationManager locationManager;

    private GameState state = GameState.IDLE;

    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> optedOut = new HashSet<>();
    private final Map<UUID, PlayerRole> roles = new HashMap<>();
    private final Map<UUID, Boolean> votes = new HashMap<>();

    private BukkitTask countdownTask;
    private int timeLeft;
    private int maxPlayers = 0;
    private boolean testPhaseActive = false;
    // Tracks whether the 60-second glow reveal has already been applied this round
    private boolean glowRevealApplied = false;

    public GameManager(UntitledChaseGame plugin) {
        this.plugin = plugin;
        this.bossBarManager = plugin.getBossBarManager();
        this.scoreboardManager = plugin.getScoreboardManager();
        this.locationManager = plugin.getLocationManager();
        this.maxPlayers = plugin.getConfig().getInt("max-players", 0);
    }

    // ─── State Accessors ─────────────────────────────────────────────────────────

    public GameState getState() { return state; }
    public Set<UUID> getParticipants() { return participants; }
    public Map<UUID, PlayerRole> getRoles() { return roles; }
    public int getTimeLeft() { return timeLeft; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int max) { this.maxPlayers = max; }

    // ─── Phase Transitions ────────────────────────────────────────────────────────

    public void startStartingPhase() {
        if (state != GameState.IDLE) return;
        state = GameState.STARTING;
        participants.clear();
        optedOut.clear();
        roles.clear();
        votes.clear();
        glowRevealApplied = false;

        for (Player p : Bukkit.getOnlinePlayers()) {
            participants.add(p.getUniqueId());
        }

        int duration = plugin.getConfig().getInt("starting-duration", 60);
        timeLeft = duration;

        // Teleport all online players to waiting area
        Location waitingArea = locationManager.getWaitingArea();
        if (waitingArea != null) {
            for (Player p : Bukkit.getOnlinePlayers()) p.teleport(waitingArea);
        }

        broadcast(ChatColor.GOLD + "[UCG] " + ChatColor.YELLOW + "A Chase Game is starting in " + duration + " seconds! Use /UCG leave to opt out.");

        cancelCountdown();
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            timeLeft--;
            updateStartingBossBar();
            if (timeLeft <= 0) {
                startPlayingPhase();
            }
        }, 20L, 20L);
    }

    public void startPlayingPhase() {
        cancelCountdown();

        participants.removeAll(optedOut);

        if (maxPlayers > 0 && participants.size() > maxPlayers) {
            List<UUID> list = new ArrayList<>(participants);
            Collections.shuffle(list);
            participants.clear();
            participants.addAll(list.subList(0, maxPlayers));
        }

        if (participants.size() < 2) {
            broadcast(ChatColor.RED + "[UCG] Not enough players to start. Game cancelled.");
            resetToIdle();
            return;
        }

        state = GameState.PLAYING;
        roles.clear();
        glowRevealApplied = false;

        List<UUID> list = new ArrayList<>(participants);
        Collections.shuffle(list);
        UUID chaserUUID = list.get(0);
        for (UUID id : participants) {
            roles.put(id, id.equals(chaserUUID) ? PlayerRole.CHASER : PlayerRole.RUNNER);
        }

        Location playArea = locationManager.getRandomPlayArea();

        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            if (playArea != null) p.teleport(playArea);
            // No glow at game start — clear any existing effects
            p.removePotionEffect(PotionEffectType.GLOWING);
            removeFromGlowTeams(p);
            bossBarManager.show(p);
        }

        int duration = plugin.getConfig().getInt("playing-duration", 300);
        timeLeft = duration;

        Player chaserPlayer = Bukkit.getPlayer(chaserUUID);
        String chaserName = chaserPlayer != null ? chaserPlayer.getName() : "Unknown";
        broadcast(ChatColor.GOLD + "[UCG] " + ChatColor.GREEN + "The game has started! " +
                ChatColor.RED + chaserName + ChatColor.GREEN + " is the first Chaser!");

        // Apply team colors without glow potion
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) applyTeamColor(p, roles.get(id));
        }

        updateScoreboards();
        updateBossBar();

        cancelCountdown();
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            timeLeft--;
            updateBossBar();
            updateScoreboards();

            // At exactly 60 seconds remaining, apply glow to all runners
            if (timeLeft == 60 && !glowRevealApplied) {
                glowRevealApplied = true;
                broadcast(ChatColor.YELLOW + "[UCG] " + ChatColor.BOLD + "1 minute remaining! Runners are now visible!");
                for (UUID id : participants) {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null) continue;
                    if (roles.get(id) == PlayerRole.RUNNER) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                    }
                }
            }

            if (timeLeft <= 0) {
                long runnerCount = roles.values().stream().filter(r -> r == PlayerRole.RUNNER).count();
                if (runnerCount > 0) {
                    endGame(false);
                } else {
                    endGame(true);
                }
            }
        }, 20L, 20L);
    }

    public void startVotingPhase() {
        cancelCountdown();
        state = GameState.VOTING;
        votes.clear();

        int duration = plugin.getConfig().getInt("voting-duration", 30);
        timeLeft = duration;

        // Teleport all online players to waiting area
        Location waitingArea = locationManager.getWaitingArea();
        if (waitingArea != null) {
            for (Player p : Bukkit.getOnlinePlayers()) p.teleport(waitingArea);
        }

        broadcast(ChatColor.GOLD + "[UCG] " + ChatColor.AQUA + "Vote to play again! You have " + duration + " seconds.");

        // Open voting GUI for all online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            VotingGUI gui = new VotingGUI(this);
            plugin.getGuiManager().openGUI(gui, p);
        }

        cancelCountdown();
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            timeLeft--;
            if (timeLeft <= 0) {
                resolveVote();
            }
        }, 20L, 20L);
    }

    public void resolveVote() {
        cancelCountdown();
        int yes = 0, no = 0;
        for (boolean v : votes.values()) {
            if (v) yes++; else no++;
        }
        broadcast(ChatColor.GOLD + "[UCG] " + ChatColor.YELLOW + "Vote results: YES=" + yes + " NO=" + no);
        if (yes > no) {
            broadcast(ChatColor.GREEN + "[UCG] Majority voted YES! Starting a new round...");
            resetToIdle();
            startStartingPhase();
        } else {
            broadcast(ChatColor.RED + "[UCG] Vote did not pass. Returning to idle.");
            teleportAllToLobby();
            resetToIdle();
        }
    }

    public void stopGame() {
        cancelCountdown();
        cleanupEffects();
        bossBarManager.hideAll();
        scoreboardManager.removeAll();
        teleportAllToLobby();
        resetToIdle();
        broadcast(ChatColor.RED + "[UCG] The game has been stopped.");
    }

    private void endGame(boolean chasersWin) {
        cancelCountdown();
        cleanupEffects();
        bossBarManager.hideAll();
        scoreboardManager.removeAll();

        if (chasersWin) {
            broadcast(ChatColor.RED + "[UCG] " + ChatColor.BOLD + "CHASERS WIN! All players have been tagged!");
        } else {
            broadcast(ChatColor.BLUE + "[UCG] " + ChatColor.BOLD + "RUNNERS WIN! Time ran out!");
        }

        // Teleport participants to waiting area before voting
        Location waitingArea = locationManager.getWaitingArea();
        if (waitingArea != null) {
            for (UUID id : participants) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.teleport(waitingArea);
            }
        }

        startVotingPhase();
    }

    private void resetToIdle() {
        state = GameState.IDLE;
        participants.clear();
        optedOut.clear();
        roles.clear();
        votes.clear();
        glowRevealApplied = false;
        testPhaseActive = false;
    }

    private void teleportAllToLobby() {
        Location lobby = locationManager.getLobby();
        if (lobby == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) p.teleport(lobby);
    }

    // ─── Player Actions ───────────────────────────────────────────────────────────

    public boolean joinGame(Player player) {
        if (state != GameState.STARTING) return false;
        if (maxPlayers > 0 && participants.size() >= maxPlayers) {
            player.sendMessage(ChatColor.RED + "[UCG] The game is full!");
            return false;
        }
        optedOut.remove(player.getUniqueId());
        participants.add(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "[UCG] You have joined the upcoming round!");
        return true;
    }

    public boolean leaveGame(Player player) {
        if (state == GameState.STARTING) {
            participants.remove(player.getUniqueId());
            optedOut.add(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "[UCG] You have opted out of the upcoming round.");
            return true;
        }
        if (state == GameState.PLAYING) {
            handlePlayerLeave(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "[UCG] You have left the game.");
            return true;
        }
        return false;
    }

    public void castVote(Player player, boolean yes) {
        if (state != GameState.VOTING) {
            player.sendMessage(ChatColor.RED + "[UCG] There is no active vote right now.");
            return;
        }
        if (votes.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "[UCG] You have already voted.");
            return;
        }
        votes.put(player.getUniqueId(), yes);
        player.sendMessage(ChatColor.GREEN + "[UCG] Your vote has been recorded: " + (yes ? "YES" : "NO"));
    }

    // ─── Role Management ──────────────────────────────────────────────────────────

    public void tagPlayer(Player runner) {
        if (state != GameState.PLAYING) return;
        roles.put(runner.getUniqueId(), PlayerRole.CHASER);
        // Remove glow from runner (they were glowing if reveal happened), apply chaser team color
        runner.removePotionEffect(PotionEffectType.GLOWING);
        applyTeamColor(runner, PlayerRole.CHASER);
        broadcast(ChatColor.RED + "[UCG] " + runner.getName() + " has been tagged and is now a Chaser!");
        updateScoreboards();
        checkWinCondition();
    }

    public void setRole(Player player, PlayerRole role) {
        if (!participants.contains(player.getUniqueId())) return;
        roles.put(player.getUniqueId(), role);
        // Reapply glow state based on current reveal status
        player.removePotionEffect(PotionEffectType.GLOWING);
        applyTeamColor(player, role);
        if (glowRevealApplied && role == PlayerRole.RUNNER) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        }
        updateScoreboards();
    }

    private void checkWinCondition() {
        long runnerCount = roles.values().stream().filter(r -> r == PlayerRole.RUNNER).count();
        if (runnerCount == 0) {
            endGame(true);
        }
    }

    // ─── Disconnect Safety ────────────────────────────────────────────────────────

    public void handlePlayerLeave(UUID uuid) {
        if (state == GameState.PLAYING) {
            PlayerRole role = roles.remove(uuid);
            participants.remove(uuid);

            if (participants.size() < 2) {
                broadcast(ChatColor.RED + "[UCG] Not enough players remaining. Ending game.");
                cancelCountdown();
                cleanupEffects();
                bossBarManager.hideAll();
                scoreboardManager.removeAll();
                teleportAllToLobby();
                resetToIdle();
                return;
            }

            if (role == PlayerRole.CHASER) {
                boolean hasChaserLeft = roles.values().stream().anyMatch(r -> r == PlayerRole.CHASER);
                if (!hasChaserLeft) {
                    List<UUID> runners = new ArrayList<>();
                    for (Map.Entry<UUID, PlayerRole> entry : roles.entrySet()) {
                        if (entry.getValue() == PlayerRole.RUNNER) runners.add(entry.getKey());
                    }
                    if (!runners.isEmpty()) {
                        UUID newChaser = runners.get(new Random().nextInt(runners.size()));
                        roles.put(newChaser, PlayerRole.CHASER);
                        Player newChaserPlayer = Bukkit.getPlayer(newChaser);
                        if (newChaserPlayer != null) {
                            newChaserPlayer.removePotionEffect(PotionEffectType.GLOWING);
                            applyTeamColor(newChaserPlayer, PlayerRole.CHASER);
                            broadcast(ChatColor.RED + "[UCG] The Chaser left! " + newChaserPlayer.getName() + " is now the Chaser!");
                        }
                    }
                }
            }
            updateScoreboards();
            checkWinCondition();
        } else if (state == GameState.STARTING) {
            participants.remove(uuid);
        }
    }

    // ─── Visual Effects ───────────────────────────────────────────────────────────

    // Applies scoreboard team color for glow outline without adding the glow potion
    private void applyTeamColor(Player player, PlayerRole role) {
        org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = role == PlayerRole.CHASER ? "ucg_chaser" : "ucg_runner";
        org.bukkit.scoreboard.Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.setColor(role == PlayerRole.CHASER ? ChatColor.RED : ChatColor.BLUE);
        }
        removeFromGlowTeams(player);
        team.addEntry(player.getName());
    }

    private void removeFromGlowTeams(Player player) {
        org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team chaser = board.getTeam("ucg_chaser");
        org.bukkit.scoreboard.Team runner = board.getTeam("ucg_runner");
        if (chaser != null) chaser.removeEntry(player.getName());
        if (runner != null) runner.removeEntry(player.getName());
    }

    private void cleanupEffects() {
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.removePotionEffect(PotionEffectType.GLOWING);
                removeFromGlowTeams(p);
            }
        }
        org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team chaser = board.getTeam("ucg_chaser");
        org.bukkit.scoreboard.Team runner = board.getTeam("ucg_runner");
        if (chaser != null) chaser.unregister();
        if (runner != null) runner.unregister();
    }

    // ─── Scoreboard & BossBar Updates ─────────────────────────────────────────────

    private void updateScoreboards() {
        long runnerCount = roles.values().stream().filter(r -> r == PlayerRole.RUNNER).count();
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            PlayerRole role = roles.getOrDefault(id, PlayerRole.RUNNER);
            String roleStr = role == PlayerRole.CHASER
                    ? ChatColor.RED + "Chaser"
                    : ChatColor.BLUE + "Runner";
            scoreboardManager.update(p, (int) runnerCount, roleStr);
        }
    }

    private void updateBossBar() {
        int total = plugin.getConfig().getInt("playing-duration", 300);
        double progress = (double) timeLeft / total;
        String title = ChatColor.GOLD + "Chase Game " + ChatColor.WHITE + "— " + formatTime(timeLeft);
        bossBarManager.update(title, progress);
    }

    private void updateStartingBossBar() {
        int total = plugin.getConfig().getInt("starting-duration", 60);
        double progress = (double) timeLeft / total;
        String title = ChatColor.YELLOW + "Game starting in " + ChatColor.WHITE + timeLeft + "s";
        bossBarManager.update(title, progress);
        for (Player p : Bukkit.getOnlinePlayers()) bossBarManager.show(p);
    }

    // ─── Admin / Test Commands ────────────────────────────────────────────────────

    public void forceStart() {
        if (state == GameState.STARTING) {
            startPlayingPhase();
        }
    }

    public void setTimer(int seconds) {
        this.timeLeft = seconds;
        if (state == GameState.PLAYING) {
            updateBossBar();
            updateScoreboards();
        }
    }

    public void forcePhase(GameState targetState) {
        cancelCountdown();
        cleanupEffects();
        bossBarManager.hideAll();
        scoreboardManager.removeAll();
        testPhaseActive = true;

        switch (targetState) {
            case IDLE:
                resetToIdle();
                broadcast(ChatColor.GRAY + "[UCG-TEST] Forced state: IDLE");
                break;
            case STARTING:
                state = GameState.IDLE;
                startStartingPhase();
                broadcast(ChatColor.GRAY + "[UCG-TEST] Forced state: STARTING");
                break;
            case PLAYING:
                if (participants.size() < 2) {
                    for (Player p : Bukkit.getOnlinePlayers()) participants.add(p.getUniqueId());
                }
                state = GameState.STARTING;
                startPlayingPhase();
                broadcast(ChatColor.GRAY + "[UCG-TEST] Forced state: PLAYING");
                break;
            case VOTING:
                state = GameState.PLAYING;
                startVotingPhase();
                broadcast(ChatColor.GRAY + "[UCG-TEST] Forced state: VOTING");
                break;
        }
    }

    public void stopTestPhase() {
        if (!testPhaseActive) return;
        stopGame();
        broadcast(ChatColor.GRAY + "[UCG-TEST] Test phase stopped.");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void broadcast(String message) {
        Bukkit.broadcastMessage(message);
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    public boolean isParticipant(Player player) {
        return participants.contains(player.getUniqueId());
    }

    public PlayerRole getRole(Player player) {
        return roles.get(player.getUniqueId());
    }
}