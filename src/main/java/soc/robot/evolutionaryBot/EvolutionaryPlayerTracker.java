package soc.robot.evolutionaryBot;

import soc.disableDebug.D;
import soc.game.*;
import soc.robot.*;

import java.util.TreeMap;

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

    public static SOCPlayerTracker[] tryPutPiece
            (final SOCPlayingPiece piece, final SOCGame game, final SOCPlayerTracker[] trackers)
    {
        final SOCPlayerTracker[] trackersCopy = EvolutionaryPlayerTracker.copyPlayerTrackers(trackers);

        if (piece != null)
        {
            game.putTempPiece(piece);

            for (final SOCPlayerTracker trackerCopy : trackersCopy)
            {
                if (trackerCopy == null)
                    continue;

                switch (piece.getType())
                {
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

    public static SOCPlayerTracker[] copyPlayerTrackers(final SOCPlayerTracker[] trackers)
    {
        final SOCPlayerTracker[] trackersCopy
                = new SOCPlayerTracker[trackers.length];  // length == SOCGame.maxPlayers

        //
        // copy the trackers but not the connections between the pieces
        //
        for (SOCPlayerTracker pt : trackers)
        {
            if (pt != null)
                trackersCopy[pt.getPlayer().getPlayerNumber()] = new EvolutionaryPlayerTracker(pt);
        }

        //
        // now make the connections between the pieces
        //
        //D.ebugPrintln(">>>>> Making connections between pieces");

        for (int tpn = 0; tpn < trackers.length; ++tpn)
        {
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

            for (SOCPossibleRoad posRoad : possibleRoads.values())
            {
                SOCPossibleRoad posRoadCopy = possibleRoadsCopy.get(Integer.valueOf(posRoad.getCoordinates()));

                //D.ebugPrintln(">>> posRoad     : "+posRoad);
                //D.ebugPrintln(">>> posRoadCopy : "+posRoadCopy);

                for (SOCPossibleRoad necRoad : posRoad.getNecessaryRoads())
                {
                    //D.ebugPrintln(">> posRoad.necRoad : "+necRoad);
                    //
                    // now find the copy of this necessary road and
                    // add it to the pos road copy's nec road list
                    //
                    SOCPossibleRoad necRoadCopy = possibleRoadsCopy.get(Integer.valueOf(necRoad.getCoordinates()));

                    if (necRoadCopy != null)
                    {
                        posRoadCopy.addNecessaryRoad(necRoadCopy);
                    }
                    else
                    {
                        D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : necRoadCopy == null");
                    }
                }

                for (SOCPossiblePiece newPos : posRoad.getNewPossibilities())
                {
                    //D.ebugPrintln(">> posRoad.newPos : "+newPos);
                    //
                    // now find the copy of this new possibility and
                    // add it to the pos road copy's new possibility list
                    //
                    switch (newPos.getType())
                    {
                        case SOCPossiblePiece.SHIP:  // fall through to ROAD
                        case SOCPossiblePiece.ROAD:

                            SOCPossibleRoad newPosRoadCopy = possibleRoadsCopy.get(Integer.valueOf(newPos.getCoordinates()));

                            if (newPosRoadCopy != null)
                            {
                                posRoadCopy.addNewPossibility(newPosRoadCopy);
                            }
                            else
                            {
                                D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : newPosRoadCopy == null");
                            }

                            break;

                        case SOCPossiblePiece.SETTLEMENT:

                            SOCPossibleSettlement newPosSettlementCopy = possibleSettlementsCopy.get
                                    (Integer.valueOf(newPos.getCoordinates()));

                            if (newPosSettlementCopy != null)
                            {
                                posRoadCopy.addNewPossibility(newPosSettlementCopy);
                            }
                            else
                            {
                                D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : newPosSettlementCopy == null");
                            }

                            break;
                    }
                }
            }


            for (SOCPossibleSettlement posSet : possibleSettlements.values())
            {
                SOCPossibleSettlement posSetCopy
                        = possibleSettlementsCopy.get(Integer.valueOf(posSet.getCoordinates()));

                //D.ebugPrintln(">>> posSet     : "+posSet);
                //D.ebugPrintln(">>> posSetCopy : "+posSetCopy);

                for (SOCPossibleRoad necRoad : posSet.getNecessaryRoads())
                {
                    //D.ebugPrintln(">> posSet.necRoad : "+necRoad);
                    //
                    // now find the copy of this necessary road and
                    // add it to the pos settlement copy's nec road list
                    //
                    SOCPossibleRoad necRoadCopy
                            = possibleRoadsCopy.get(Integer.valueOf(necRoad.getCoordinates()));

                    if (necRoadCopy != null)
                    {
                        posSetCopy.addNecessaryRoad(necRoadCopy);
                    }
                    else
                    {
                        D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : necRoadCopy == null");
                    }
                }


                for (SOCPossibleSettlement conflict : posSet.getConflicts())
                {
                    //D.ebugPrintln(">> posSet.conflict : "+conflict);
                    //
                    // now find the copy of this conflict and
                    // add it to the conflict list in the pos settlement copy
                    //
                    SOCPlayerTracker trackerCopy2 = trackersCopy[conflict.getPlayer().getPlayerNumber()];

                    if (trackerCopy2 == null)
                    {
                        D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : trackerCopy2 == null");
                    }
                    else
                    {
                        SOCPossibleSettlement conflictCopy = trackerCopy2.getPossibleSettlements().get
                                (Integer.valueOf(conflict.getCoordinates()));

                        if (conflictCopy == null)
                        {
                            D.ebugPrintlnINFO("*** ERROR in copyPlayerTrackers : conflictCopy == null");
                        }
                        else
                        {
                            posSetCopy.addConflict(conflictCopy);
                        }
                    }
                }
            }
        }

        return trackersCopy;
    }
}
