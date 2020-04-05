package io.github.zap.zombiesplugin.manager;

import io.github.zap.zombiesplugin.ZombiesPlugin;
import io.github.zap.zombiesplugin.events.UserJoinLeaveEventArgs;
import io.github.zap.zombiesplugin.map.Window;
import io.github.zap.zombiesplugin.map.round.Round;
import java.util.List;

import io.github.zap.zombiesplugin.player.PlayerState;
import io.github.zap.zombiesplugin.player.User;
import io.github.zap.zombiesplugin.scoreboard.IInGameScoreboard;
import io.github.zap.zombiesplugin.scoreboard.InGameScoreBoard;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.zap.zombiesplugin.map.round.Wave;
import io.github.zap.zombiesplugin.map.spawn.SpawnPoint;
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class GameManager implements Listener {
    public final String name;

    private GameSettings settings;
    private UserManager userManager;
    private GameState state
    private IInGameScoreboard scoreboard;

    private int currentMobCount = 0;
    private int currentRoundIndex = 0;
    private Round currentRound;

    public GameManager(String name, GameSettings settings) {
        this.name = name;
        this.settings = settings;

        this.scoreboard = new InGameScoreBoard(this);
        userManager = new UserManager(this);
        userManager.getPlayerJoinLeaveHandler().registerEvent(this::onPlayerChange);

        ZombiesPlugin.instance.getServer().getPluginManager().registerEvents(this, ZombiesPlugin.instance);
    }

    public String getName() { return name; }

    public GameSettings getSettings() { return settings; }

    public UserManager getUserManager() { return userManager; }

    public GameState getState() { return state; }

    /**
     * Gets the zero-based round index.
     * @return The zero-based round index
     */
    public int getCurrentRound() { return currentRoundIndex; }

    public boolean runAI() { return state == GameState.STARTED; }

    private void onPlayerChange(Object sender, @NotNull UserJoinLeaveEventArgs e) {
        switch(e.type) {
            case JOIN:
                if(state == GameState.PREGAME && userManager.getPlayers().size() == settings.getGameSize()) {
                    state = GameState.COUNTDOWN;
                    startCountdown();
                }
                break;
            case LEAVE:
                if(state == GameState.COUNTDOWN) {
                    state = GameState.PREGAME;
                    stopCountdown();
                }
                else if(state == GameState.STARTED) {
                    if(userManager.getPlayers().size() == 0) {
                        state = GameState.CANCELED;
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + e.type.toString());
        }
    }

    public void startCountdown() {
        //TODO: timer code

        //should eventually call startRound if the countdown successfully completes
    }

    public void stopCountdown() {
        //TODO: abort timer
    }

    private void startRound(int index) {
        if(state == GameState.COUNTDOWN) {
            state = GameState.STARTED;
        }

        ArrayList<Round> rounds = settings.getGameMap().getRounds();

        if (index == rounds.size()) {
            state = GameState.WON;
            // TODO: Endgame sequence
        } else {
            currentRound = rounds.get(index);

            ArrayList<Wave> waves = currentRound.getWaves();
            for(Wave wave : waves) {
                currentMobCount += wave.getMobs(settings.getDifficulty()).size();
            }

            currentRound.start(this);
        }
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        if(state == GameState.STARTED) {
            AbstractEntity mob = event.getMob().getEntity();
            if(mob.hasMetadata("zp_manager") && mob.getMetadata("zp_manager").isPresent()) {
                GameManager manager = (GameManager)mob.getMetadata("zp_manager").get();
                if(manager == this) {
                    currentMobCount--;
                    if(currentMobCount == 0) {
                        currentRoundIndex++;
                        startRound(currentRoundIndex);
                    }
                }
            }
        }
    }
}
