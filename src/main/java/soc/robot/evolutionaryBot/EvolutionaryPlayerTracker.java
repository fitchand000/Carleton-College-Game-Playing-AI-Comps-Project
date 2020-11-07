package soc.robot.evolutionaryBot;

import soc.game.SOCPlayer;
import soc.robot.SOCPlayerTracker;
import soc.robot.SOCRobotBrain;

public class EvolutionaryPlayerTracker extends SOCPlayerTracker {

    public EvolutionaryPlayerTracker(SOCPlayer pl, SOCRobotBrain br) {
        super(pl, br);
    }

    public EvolutionaryPlayerTracker(SOCPlayerTracker pt) {
        super(pt);
    }

    @Override
    public void recalcWinGameETA() {
        winGameETA = 10;
    }
}
