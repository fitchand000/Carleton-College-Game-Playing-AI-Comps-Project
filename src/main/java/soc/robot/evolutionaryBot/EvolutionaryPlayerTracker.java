package soc.robot.evolutionaryBot;
import soc.disableDebug.D;
import soc.game.*;
import soc.robot.*;
import java.util.TreeMap;
import  java.io.File;

public class EvolutionaryPlayerTracker extends SOCPlayerTracker {

    //public EvolutionaryBotBrain brain;

    public EvolutionaryPlayerTracker(SOCPlayer pl, EvolutionaryBotBrain br) {
        super(pl, br);
    }

    public EvolutionaryPlayerTracker(SOCPlayerTracker pt) {
        super(pt);
    }

    @Override
    public void recalcWinGameETA() {
        // TODO make sure player tracker stuff is up to date.
        // TODO There are other methods called at the start of recalcWinGameETA in Player Tracker that set the values for the inputs we care about
        // TODO e.g. the settelmentETA is set via a building speed estimator in this function

        winGameETA = brain.winGameAlgorithm.calculateWinEta(this);
        String GameName = this.brain.getGame().name;

        File CollectorFile = new File("/Users/batorgil/Documents/code/Carleton-College-Game-Playing-AI-Comps-Project/python/simulation1.csv");
        boolean exists = CollectorFile.exists();

        if (exists) {
            PrintETA.appendFile(winGameETA, this);
        } else {
            PrintETA.createFile();
            PrintETA.appendFile(winGameETA, this);
        }
        //System.out.println(winGameETA);
//        System.out.println(playerNumber);
//        System.out.println(this.brain.getGame());

//        System.out.println("game name:" + this.brain.getGame().name);
//        System.out.println("player number" + playerNumber);
//        System.out.println("winGameETA" + winGameETA);
//        System.out.println("turnCount within the SOCGame: " + this.brain.getGame().turnCount); //same numbers get to printed different times
//        // ....101, 20, 97, 48, 100, 46, 20 they start new turn number with the player 0
//        System.out.println("roundCount within the SOCGame: " + this.brain.getGame().roundCount); // 20 it was called times
//        System.out.println("----------------------");
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


    public static SOCPlayerTracker[] tryPutPiece
            (final SOCPlayingPiece piece, final SOCGame game, final SOCPlayerTracker[] trackers) {
        final SOCPlayerTracker[] trackersCopy = EvolutionaryPlayerTracker.copyPlayerTrackers(trackers);

        if (piece != null) {
            game.putTempPiece(piece);

            for (final SOCPlayerTracker trackerCopy : trackersCopy) {
                if (trackerCopy == null)
                    continue;

                switch (piece.getType()) {
                    case SOCPlayingPiece.SHIP:  // fall through to ROAD
                    case SOCPlayingPiece.ROAD:
                        trackerCopy.addNewRoadOrShip((SOCRoutePiece) piece, trackersCopy);

                        break;

                    case SOCPlayingPiece.SETTLEMENT:
                        trackerCopy.addNewSettlement((SOCSettlement) piece, trackersCopy);

                        break;

                    case SOCPlayingPiece.CITY:
                        trackerCopy.addOurNewCity((SOCCity) piece);

                        break;
                }
            }
        }

        return trackersCopy;
    }

    public static SOCPlayerTracker[] copyPlayerTrackers(final SOCPlayerTracker[] trackers) {
        final SOCPlayerTracker[] trackersCopy
                = new SOCPlayerTracker[trackers.length];  // length == SOCGame.maxPlayers

        //
        // copy the trackers but not the connections between the pieces
        //
        for (SOCPlayerTracker pt : trackers) {
            if (pt != null)
                trackersCopy[pt.getPlayer().getPlayerNumber()] = new EvolutionaryPlayerTracker(pt);
        }

        //
        // now make the connections between the pieces
        //
        //D.ebugPrintln(">>>>> Making connections between pieces");

        for (int tpn = 0; tpn < trackers.length; ++tpn) {
            final SOCPlayerTracker tracker = trackers[tpn];
            if (tracker == null)
                continue;
            final SOCPlayerTracker trackerCopy = trackersCopy[tracker.getPlayer().getPlayerNumber()];

            //D.ebugPrintln(">>>> Player num for tracker is "+tracker.getPlayer().getPlayerNumber());
            //D.ebugPrintln(">>>> Player num for trackerCopy is "+trackerCopy.getPlayer().getPlayerNumber());
            TreeMap<Integer, SOCPossibleRoad> possibleRoads = tracker.getPossibleRoads();
            TreeMap<Integer, SOCPossibleRoad> possibleRoadsCopy = trackerCopy.getPossibleRoads();
            TreeMap<Integer, SOCPossibleSettlement> possibleSettlements = tracker.getPossibleSettlements();
            TreeMap<Integer, SOCPossibleSettlement> possibleSettlementsCopy = trackerCopy.getPossibleSettlements();

            for (SOCPossibleRoad posRoad : possibleRoads.values()) {
                SOCPossibleRoad posRoadCopy = possibleRoadsCopy.get(Integer.valueOf(posRoad.getCoordinates()));

                //D.ebugPrintln(">>> posRoad     : "+posRoad);
                //D.ebugPrintln(">>> posRoadCopy : "+posRoadCopy);

                for (SOCPossibleRoad necRoad : posRoad.getNecessaryRoads()) {
                    //D.ebugPrintln(">> posRoad.necRoad : "+necRoad);
                    //
                    // now find the copy of this necessary road and
                    // add it to the pos road copy's nec road list
                    //
                    SOCPossibleRoad necRoadCopy = possibleRoadsCopy.get(Integer.valueOf(necRoad.getCoordinates()));

                    if (necRoadCopy != null) {
                        posRoadCopy.addNecessaryRoad(necRoadCopy);
                    } else {
                        D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : necRoadCopy == null");
                    }
                }

                for (SOCPossiblePiece newPos : posRoad.getNewPossibilities()) {
                    //D.ebugPrintln(">> posRoad.newPos : "+newPos);
                    //
                    // now find the copy of this new possibility and
                    // add it to the pos road copy's new possibility list
                    //
                    switch (newPos.getType()) {
                        case SOCPossiblePiece.SHIP:  // fall through to ROAD
                        case SOCPossiblePiece.ROAD:

                            SOCPossibleRoad newPosRoadCopy = possibleRoadsCopy.get(Integer.valueOf(newPos.getCoordinates()));

                            if (newPosRoadCopy != null) {
                                posRoadCopy.addNewPossibility(newPosRoadCopy);
                            } else {
                                D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : newPosRoadCopy == null");
                            }

                            break;

                        case SOCPossiblePiece.SETTLEMENT:

                            SOCPossibleSettlement newPosSettlementCopy = possibleSettlementsCopy.get
                                    (Integer.valueOf(newPos.getCoordinates()));

                            if (newPosSettlementCopy != null) {
                                posRoadCopy.addNewPossibility(newPosSettlementCopy);
                            } else {
                                D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : newPosSettlementCopy == null");
                            }

                            break;
                    }
                }
            }


            for (SOCPossibleSettlement posSet : possibleSettlements.values()) {
                SOCPossibleSettlement posSetCopy
                        = possibleSettlementsCopy.get(Integer.valueOf(posSet.getCoordinates()));

                //D.ebugPrintln(">>> posSet     : "+posSet);
                //D.ebugPrintln(">>> posSetCopy : "+posSetCopy);

                for (SOCPossibleRoad necRoad : posSet.getNecessaryRoads()) {
                    //D.ebugPrintln(">> posSet.necRoad : "+necRoad);
                    //
                    // now find the copy of this necessary road and
                    // add it to the pos settlement copy's nec road list
                    //
                    SOCPossibleRoad necRoadCopy
                            = possibleRoadsCopy.get(Integer.valueOf(necRoad.getCoordinates()));

                    if (necRoadCopy != null) {
                        posSetCopy.addNecessaryRoad(necRoadCopy);
                    } else {
                        D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : necRoadCopy == null");
                    }
                }


                for (SOCPossibleSettlement conflict : posSet.getConflicts()) {
                    //D.ebugPrintln(">> posSet.conflict : "+conflict);
                    //
                    // now find the copy of this conflict and
                    // add it to the conflict list in the pos settlement copy
                    //
                    SOCPlayerTracker trackerCopy2 = trackersCopy[conflict.getPlayer().getPlayerNumber()];

                    if (trackerCopy2 == null) {
                        D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : trackerCopy2 == null");
                    } else {
                        SOCPossibleSettlement conflictCopy = trackerCopy2.getPossibleSettlements().get
                                (Integer.valueOf(conflict.getCoordinates()));

                        if (conflictCopy == null) {
                            D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : conflictCopy == null");
                        } else {
                            posSetCopy.addConflict(conflictCopy);
                        }
                    }
                }
            }
        }

        return trackersCopy;
    }
}
