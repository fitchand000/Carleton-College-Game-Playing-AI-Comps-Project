package soc.robot.evolutionaryBot;
import soc.disableDebug.D;
import soc.game.*;
import soc.robot.*;
import java.util.TreeMap;

public class EvolutionaryPlayerTracker extends SOCPlayerTracker {

    public EvolutionaryPlayerTracker(SOCPlayer pl, EvolutionaryBotBrain br) {
        super(pl, br);
    }

    public EvolutionaryPlayerTracker(SOCPlayerTracker pt) {
        super(pt);
    }

    @Override
    public void recalcWinGameETA() {
        winGameETA = brain.winGameAlgorithm.calculateWinEta(this);
        //System.out.println(winGameETA);
//        System.out.println(playerNumber);
//        System.out.println(this.brain.getGame());

    }

    /**
     * @return estimated time to build road, settlement, city, or development card
     */
    public int getBuildETA(String buildType) {
        SOCBuildingSpeedEstimate buildingSpeedEstimate = brain.getEstimator(player.getNumbers());
        boolean[] ports = player.getPortFlags();
        int[] estimates = buildingSpeedEstimate.getEstimatesFromNowFast(player.getResources(), ports); //Can also do accurate instead of fast
        switch (buildType) {
            case "road":
                return estimates[0];
            case "settlement":
                return estimates[1];
            case "city":
                return estimates[2];
            case "dev card":
                return estimates[3];
        }
        return 0;
    }

    /**
     * @return total number of playable settlement locations in next turn
     */
    public int getReadyBuildSpotCount()
    {
        int[] locations = player.getPotentialSettlements_arr();
        if (locations == null) {
            return 0;
        }
        return locations.length;
    }

    /**
     * @return the total number of development cards and special items
     */
    public int getDevCardCount()
    {
        return player.getInventory().getTotal();
    }

    /**
     * @return total number of remaining possible settlement locations
     */
    public int getBuildLocationCount()
    {
        return player.getLegalSettlements().size();
    }

    /**
     * @return player's port count
     */
    public int getPortCount()
    {
        boolean[] ports = player.getPortFlags();
        int count = 0;
        for (Boolean p : ports) {
            if (p) {
                count++;
            }
        }
        return count;
    }

    public static int getNumRolls(int n) {
        switch(n) {
            case 2:
            case 12:
                return 1;
            case 3:
            case 11:
                return 2;
            case 4:
            case 10:
                return 3;
            case 5:
            case 9:
                return 4;
            case 6:
            case 8:
                return 5;
            case 7:
                return 6;
            default:
                throw new RuntimeException();
        }
    }

    /**
     * @return income for each resource
     */
    public int getResourceIncome(String resource) {

        SOCPlayerNumbers nums = player.getNumbers();
        SOCBuildingSpeedEstimate buildingSpeedEstimate = brain.getEstimator(nums);
        buildingSpeedEstimate.recalculateResourcesForRoll(nums, game.getBoard().getRobberHex());
        SOCResourceSet[] resourceSets = buildingSpeedEstimate.getResourcesForRoll();

        SOCResourceSet set;
        int brick = 0;
        int ore = 0;
        int sheep = 0;
        int wheat = 0;
        int log = 0;
        int multiplier;

        for (int i = 2; i <= 12; i++) {
            set = resourceSets[i];
            multiplier = getNumRolls(i);
            brick += set.getAmount(1) * multiplier;
            ore += set.getAmount(2) * multiplier;
            sheep += set.getAmount(3) * multiplier;
            wheat += set.getAmount(4) * multiplier;
            log += set.getAmount(5) * multiplier;
        }


        int total = brick + ore + sheep + wheat + log;

        switch (resource) {
            case "brick":
                return brick;
            case "ore":
                return ore;
            case "sheep":
                return sheep;
            case "wheat":
                return wheat;
            case "log":
                return log;
            case "total":
                return total;
        }
        return 0;
    }


    /**
     * @return number of a type of resource held by player
     */
    public int getResourceCount(String resourceType) {
        SOCResourceSet rset = player.getResources();

        switch (resourceType) {
            case "brick":
                return rset.getAmount(1);
            case "ore":
                return rset.getAmount(2);
            case "sheep":
                return rset.getAmount(3);
            case "wheat":
                return rset.getAmount(4);
            case "log":
                return rset.getAmount(5);
        }
        return 0;
    }

    /**
     * @return total number of resources
     */
    public int getTotalResources()
    {
        return player.getResources().getTotal();
    }


    /**
     * @return player's total VP: Buildings, longest/largest bonus, Special VP, VP cards/items.
     */
    public int getCurrentVP()
    {
        return player.getTotalVP();
    }
}
