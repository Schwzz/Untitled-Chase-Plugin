package com.untitledchasegame;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UCGCommand implements CommandExecutor, TabCompleter {

    private final UntitledChaseGame plugin;
    private final GameManager gameManager;
    private final LocationManager locationManager;

    public UCGCommand(UntitledChaseGame plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.locationManager = plugin.getLocationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start":       return handleStart(sender);
            case "join":        return handleJoin(sender);
            case "leave":       return handleLeave(sender);
            case "vote":        return handleVote(sender, args);
            case "force-start": return handleForceStart(sender);
            case "stop":        return handleStop(sender);
            case "set":         return handleSet(sender, args);
            case "reset":       return handleReset(sender, args);
            case "testphase":   return handleTestPhase(sender, args);
            case "setmax":      return handleSetMax(sender, args);
            case "settimer":    return handleSetTimer(sender, args);
            case "setrole":     return handleSetRole(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /UCG for help.");
                return true;
        }
    }

    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("ucg.admin") && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (gameManager.getState() != GameState.IDLE) {
            sender.sendMessage(ChatColor.RED + "[UCG] A game is already in progress.");
            return true;
        }
        gameManager.startStartingPhase();
        return true;
    }

    private boolean handleJoin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (gameManager.getState() != GameState.STARTING) {
            player.sendMessage(ChatColor.RED + "[UCG] There is no game in the joining phase right now.");
            return true;
        }
        gameManager.joinGame(player);
        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!gameManager.leaveGame(player)) {
            player.sendMessage(ChatColor.RED + "[UCG] You cannot leave right now.");
        }
        return true;
    }

    private boolean handleVote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can vote.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /UCG vote <yes|no>");
            return true;
        }
        String choice = args[1].toLowerCase();
        if (!choice.equals("yes") && !choice.equals("no")) {
            player.sendMessage(ChatColor.RED + "Usage: /UCG vote <yes|no>");
            return true;
        }
        gameManager.castVote(player, choice.equals("yes"));
        return true;
    }

    private boolean handleForceStart(CommandSender sender) {
        if (!sender.hasPermission("ucg.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (gameManager.getState() != GameState.STARTING) {
            sender.sendMessage(ChatColor.RED + "[UCG] No STARTING phase to force-start.");
            return true;
        }
        gameManager.forceStart();
        sender.sendMessage(ChatColor.GREEN + "[UCG] Force-started the game.");
        return true;
    }

    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("ucg.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (gameManager.getState() == GameState.IDLE) {
            sender.sendMessage(ChatColor.RED + "[UCG] No game is running.");
            return true;
        }
        gameManager.stopGame();
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ucg.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can set locations.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /UCG set <Lobby|PlayArea <number>|WaitingArea|Server <number>>");
            return true;
        }
        Player player = (Player) sender;
        String type = args[1].toLowerCase();

        switch (type) {
            case "lobby":
                locationManager.setLobby(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "[UCG] Lobby location set.");
                break;
            case "playarea":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /UCG set PlayArea <number>");
                    return true;
                }
                try {
                    int number = Integer.parseInt(args[2]);
                    if (number < 1) throw new NumberFormatException();
                    locationManager.setPlayArea(number, player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "[UCG] Play area #" + number + " location set.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid number. Use a positive integer (e.g. 1, 2, 3).");
                }
                break;
            case "waitingarea":
                locationManager.setWaitingArea(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "[UCG] Waiting area location set.");
                break;
            case "server":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /UCG set Server <number>");
                    return true;
                }
                try {
                    int number = Integer.parseInt(args[2]);
                    if (number < 0) throw new NumberFormatException();
                    locationManager.setServerNumber(number);
                    player.sendMessage(ChatColor.GREEN + "[UCG] Server number set to " + number + ".");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid number. Use a non-negative integer.");
                }
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown type. Use: Lobby, PlayArea <number>, WaitingArea, Server <number>");
        }
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ucg.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (gameManager.getState() != GameState.IDLE) {
            sender.sendMessage(ChatColor.RED + "[UCG] Cannot reset while a game is active.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /UCG reset <all|lobby|playarea|waiting_area|server>");
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "lobby":
                locationManager.resetLobby();
                sender.sendMessage(ChatColor.GREEN + "[UCG] Lobby location has been cleared.");
                break;
            case "waiting_area":
            case "waitingarea":
                locationManager.resetWaitingArea();
                sender.sendMessage(ChatColor.GREEN + "[UCG] Waiting area location has been cleared.");
                break;
            case "playarea":
                locationManager.resetPlayAreas();
                sender.sendMessage(ChatColor.GREEN + "[UCG] All play area locations have been cleared.");
                break;
            case "server":
                locationManager.resetServerNumber();
                sender.sendMessage(ChatColor.GREEN + "[UCG] Server number has been cleared.");
                break;
            case "all":
                locationManager.resetAll();
                sender.sendMessage(ChatColor.GREEN + "[UCG] All locations and server data have been wiped.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown target. Use: all, lobby, playarea, waiting_area, server");
        }
        return true;
    }

    private boolean handleTestPhase(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ucg.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /UCG testphase <IDLE|STARTING|PLAYING|VOTING|stop>");
            return true;
        }
        String phase = args[1].toLowerCase();
        if (phase.equals("stop")) {
            gameManager.stopTestPhase();
            sender.sendMessage(ChatColor.GRAY + "[UCG-TEST] Test phase stopped.");
            return true;
        }
        GameState target;
        try {
            target = GameState.valueOf(phase.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Unknown phase. Use: IDLE, STARTING, PLAYING, VOTING, or stop");
            return true;
        }
        gameManager.forcePhase(target);
        sender.sendMessage(ChatColor.GRAY + "[UCG-TEST] Switched to phase: " + target.name());
        return true;
    }

    private boolean handleSetMax(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ucg.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /UCG setmax <number>");
            return true;
        }
        try {
            int max = Integer.parseInt(args[1]);
            if (max < 0) throw new NumberFormatException();
            gameManager.setMaxPlayers(max);
            sender.sendMessage(ChatColor.GREEN + "[UCG] Max players set to " + (max == 0 ? "unlimited" : max) + ".");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number. Use a positive integer (0 = unlimited).");
        }
        return true;
    }

    private boolean handleSetTimer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ucg.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /UCG settimer <seconds>");
            return true;
        }
        GameState state = gameManager.getState();
        if (state != GameState.PLAYING && state != GameState.STARTING) {
            sender.sendMessage(ChatColor.RED + "[UCG] Can only set timer during PLAYING or STARTING phase.");
            return true;
        }
        try {
            int seconds = Integer.parseInt(args[1]);
            if (seconds < 0) throw new NumberFormatException();
            gameManager.setTimer(seconds);
            sender.sendMessage(ChatColor.GREEN + "[UCG] Timer set to " + seconds + " seconds.");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number.");
        }
        return true;
    }

    private boolean handleSetRole(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ucg.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (gameManager.getState() != GameState.PLAYING) {
            sender.sendMessage(ChatColor.RED + "[UCG] Can only set roles during PLAYING phase.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /UCG setrole <player> <chaser|runner>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        if (!gameManager.isParticipant(target)) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is not a participant.");
            return true;
        }
        String roleStr = args[2].toLowerCase();
        PlayerRole role;
        if (roleStr.equals("chaser")) {
            role = PlayerRole.CHASER;
        } else if (roleStr.equals("runner")) {
            role = PlayerRole.RUNNER;
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown role. Use: chaser or runner");
            return true;
        }
        gameManager.setRole(target, role);
        sender.sendMessage(ChatColor.GREEN + "[UCG] Set " + target.getName() + "'s role to " + role.name() + ".");
        target.sendMessage(ChatColor.YELLOW + "[UCG] Your role has been changed to " + role.name() + " by an admin.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== UntitledChaseGame Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/UCG start " + ChatColor.WHITE + "- Begin the starting countdown");
        sender.sendMessage(ChatColor.YELLOW + "/UCG join " + ChatColor.WHITE + "- Join the upcoming round");
        sender.sendMessage(ChatColor.YELLOW + "/UCG leave " + ChatColor.WHITE + "- Leave the upcoming/current round");
        sender.sendMessage(ChatColor.YELLOW + "/UCG vote <yes|no> " + ChatColor.WHITE + "- Vote during voting phase");
        if (sender.hasPermission("ucg.admin")) {
            sender.sendMessage(ChatColor.RED + "/UCG force-start " + ChatColor.WHITE + "- Skip countdown");
            sender.sendMessage(ChatColor.RED + "/UCG stop " + ChatColor.WHITE + "- Stop the game");
            sender.sendMessage(ChatColor.RED + "/UCG set <Lobby|PlayArea <number>|WaitingArea|Server <number>> " + ChatColor.WHITE + "- Set a location or server number");
            sender.sendMessage(ChatColor.RED + "/UCG reset <all|lobby|playarea|waiting_area|server> " + ChatColor.WHITE + "- Clear saved data");
            sender.sendMessage(ChatColor.RED + "/UCG testphase <phase|stop> " + ChatColor.WHITE + "- Force a game phase");
            sender.sendMessage(ChatColor.RED + "/UCG setmax <number> " + ChatColor.WHITE + "- Set max players");
            sender.sendMessage(ChatColor.RED + "/UCG settimer <seconds> " + ChatColor.WHITE + "- Set remaining time");
            sender.sendMessage(ChatColor.RED + "/UCG setrole <player> <chaser|runner> " + ChatColor.WHITE + "- Set a player's role");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("start", "join", "leave", "vote"));
            if (sender.hasPermission("ucg.admin")) {
                subs.addAll(Arrays.asList("force-start", "stop", "set", "reset", "testphase", "setmax", "settimer", "setrole"));
            }
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "vote":
                    completions.addAll(Arrays.asList("yes", "no"));
                    break;
                case "set":
                    if (sender.hasPermission("ucg.admin"))
                        completions.addAll(Arrays.asList("Lobby", "PlayArea", "WaitingArea", "Server"));
                    break;
                case "reset":
                    if (sender.hasPermission("ucg.admin"))
                        completions.addAll(Arrays.asList("all", "lobby", "playarea", "waiting_area", "server"));
                    break;
                case "testphase":
                    if (sender.hasPermission("ucg.admin"))
                        completions.addAll(Arrays.asList("IDLE", "STARTING", "PLAYING", "VOTING", "stop"));
                    break;
                case "setrole":
                    if (sender.hasPermission("ucg.admin")) {
                        for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
                    }
                    break;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setrole") && sender.hasPermission("ucg.admin")) {
                completions.addAll(Arrays.asList("chaser", "runner"));
            } else if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("playarea") && sender.hasPermission("ucg.admin")) {
                // Suggest existing slot numbers plus the next available one
                List<Integer> existing = locationManager.getPlayAreaNumbers();
                int next = existing.isEmpty() ? 1 : existing.stream().mapToInt(i -> i).max().getAsInt() + 1;
                for (int n : existing) completions.add(String.valueOf(n));
                if (!existing.contains(next)) completions.add(String.valueOf(next));
            }
        }
        return completions;
    }
}