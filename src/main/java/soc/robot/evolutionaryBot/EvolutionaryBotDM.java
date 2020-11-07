package soc.robot.evolutionaryBot;

import soc.disableDebug.D;
import soc.game.*;
import soc.robot.*;
import soc.util.CutoffExceededException;
import soc.util.SOCRobotParameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class EvolutionaryBotDM extends SOCRobotDM {

    public EvolutionaryBotDM(SOCRobotBrain br) {
        super(br);
    }

    public EvolutionaryBotDM (SOCRobotParameters params,
                              OpeningBuildStrategy obs,
                              SOCPlayerTracker[] pt,
                              SOCPlayerTracker opt,
                              SOCPlayer opd,
                              SOCBuildPlanStack bp) {
        super(params, obs, pt, opt, opd, bp);
    }

    @Override
    protected void planRoadBuildingTwoRoads() {
        SOCPossibleRoad secondFavoriteRoad = null;
        D.ebugPrintlnINFO("*** making a plan for road building");

        ///
        /// we need to pick two roads
        ///
        if (favoriteRoad != null) {
            //
            //  pretend to put the favorite road down,
            //  and then score the new pos roads
            //
            //  TODO for now, coastal roads/ships are always built as roads not ships
            //
            final SOCRoutePiece tmpRS;
            if ((favoriteRoad instanceof SOCPossibleShip)
                    && !((SOCPossibleShip) favoriteRoad).isCoastalRoadAndShip)
                tmpRS = new SOCShip(ourPlayerData, favoriteRoad.getCoordinates(), null);
            else
                tmpRS = new SOCRoad(ourPlayerData, favoriteRoad.getCoordinates(), null);

            SOCPlayerTracker[] trackersCopy = EvolutionaryPlayerTracker.tryPutPiece(tmpRS, game, playerTrackers);
            SOCPlayerTracker.updateWinGameETAs(trackersCopy);

            SOCPlayerTracker ourPlayerTrackerCopy = trackersCopy[ourPlayerNumber];

            final int ourCurrentWGETACopy = ourPlayerTrackerCopy.getWinGameETA();
            D.ebugPrintlnINFO("ourCurrentWGETACopy = " + ourCurrentWGETACopy);

            int leadersCurrentWGETACopy = ourCurrentWGETACopy;
            for (final SOCPlayerTracker tracker : trackersCopy) {
                if (tracker == null)
                    continue;

                int wgeta = tracker.getWinGameETA();
                if (wgeta < leadersCurrentWGETACopy) {
                    leadersCurrentWGETACopy = wgeta;
                }
            }

            for (SOCPossiblePiece newPos : favoriteRoad.getNewPossibilities()) {
                if (newPos instanceof SOCPossibleRoad) {
                    newPos.resetScore();
                    // float wgetaScore = getWinGameETABonusForRoad
                    //   ((SOCPossibleRoad)newPos, currentBuildingETAs[SOCBuildingSpeedEstimate.ROAD], leadersCurrentWGETACopy, trackersCopy);


                    D.ebugPrintlnINFO("$$$ new pos road at " + Integer.toHexString(newPos.getCoordinates()));  // +" has a score of "+newPos.getScore());

                    if (favoriteRoad.getCoordinates() != newPos.getCoordinates()) {
                        if (secondFavoriteRoad == null) {
                            secondFavoriteRoad = (SOCPossibleRoad) newPos;
                        } else {
                            if (newPos.getScore() > secondFavoriteRoad.getScore()) {
                                secondFavoriteRoad = (SOCPossibleRoad) newPos;
                            }
                        }
                    }
                }
            }

            for (SOCPossibleRoad threatenedRoad : threatenedRoads) {
                D.ebugPrintlnINFO("$$$ threatened road at " + Integer.toHexString(threatenedRoad.getCoordinates()));

                //
                // see how building this piece impacts our winETA
                //
                threatenedRoad.resetScore();
                // float wgetaScore = getWinGameETABonusForRoad
                //   (threatenedRoad, currentBuildingETAs[SOCBuildingSpeedEstimate.ROAD], leadersCurrentWGETA, playerTrackers);

                D.ebugPrintlnINFO("$$$  final score = 0");  // +threatenedRoad.getScore());

                if (favoriteRoad.getCoordinates() != threatenedRoad.getCoordinates()) {
                    if (secondFavoriteRoad == null) {
                        secondFavoriteRoad = threatenedRoad;
                    } else {
                        if (threatenedRoad.getScore() > secondFavoriteRoad.getScore()) {
                            secondFavoriteRoad = threatenedRoad;
                        }
                    }
                }
            }

            for (SOCPossibleRoad goodRoad : goodRoads) {
                D.ebugPrintlnINFO("$$$ good road at " + Integer.toHexString(goodRoad.getCoordinates()));
                //
                // see how building this piece impacts our winETA
                //
                goodRoad.resetScore();
                // float wgetaScore = getWinGameETABonusForRoad
                //   (goodRoad, currentBuildingETAs[SOCBuildingSpeedEstimate.ROAD], leadersCurrentWGETA, playerTrackers);

                D.ebugPrintlnINFO("$$$  final score = 0");  // +goodRoad.getScore());

                if (favoriteRoad.getCoordinates() != goodRoad.getCoordinates()) {
                    if (secondFavoriteRoad == null) {
                        secondFavoriteRoad = goodRoad;
                    } else {
                        if (goodRoad.getScore() > secondFavoriteRoad.getScore()) {
                            secondFavoriteRoad = goodRoad;
                        }
                    }
                }
            }

            SOCPlayerTracker.undoTryPutPiece(tmpRS, game);

            if (!buildingPlan.empty()) {
                SOCPossiblePiece planPeek = buildingPlan.peek();
                if ((planPeek == null) ||
                        (!(planPeek instanceof SOCPossibleRoad))) {
                    if (secondFavoriteRoad != null) {
                        D.ebugPrintlnINFO("### SECOND FAVORITE ROAD IS AT " + Integer.toHexString(secondFavoriteRoad.getCoordinates()));
                        D.ebugPrintlnINFO("###   WITH A SCORE OF " + secondFavoriteRoad.getScore());
                        D.ebugPrintlnINFO("$ PUSHING " + secondFavoriteRoad);
                        buildingPlan.push(secondFavoriteRoad);
                        D.ebugPrintlnINFO("$ PUSHING " + favoriteRoad);
                        buildingPlan.push(favoriteRoad);
                    }
                } else if (secondFavoriteRoad != null) {
                    SOCPossiblePiece tmp = buildingPlan.pop();
                    D.ebugPrintlnINFO("$ POPPED OFF");
                    D.ebugPrintlnINFO("### SECOND FAVORITE ROAD IS AT " + Integer.toHexString(secondFavoriteRoad.getCoordinates()));
                    D.ebugPrintlnINFO("###   WITH A SCORE OF " + secondFavoriteRoad.getScore());
                    D.ebugPrintlnINFO("$ PUSHING " + secondFavoriteRoad);
                    buildingPlan.push(secondFavoriteRoad);
                    D.ebugPrintlnINFO("$ PUSHING " + tmp);
                    buildingPlan.push(tmp);
                }
            }
        }
    }

    protected void smartGameStrategy(final int[] buildingETAs)
    {
        D.ebugPrintlnINFO("***** smartGameStrategy *****");

        // If this game is on the 6-player board, check whether we're planning for
        // the Special Building Phase.  Can't buy cards or trade in that phase.
        final boolean forSpecialBuildingPhase =
                game.isSpecialBuilding() || (game.getCurrentPlayerNumber() != ourPlayerNumber);

        //
        // save the lr paths list to restore later
        //
        @SuppressWarnings("unchecked")
        List<SOCLRPathData>[] savedLRPaths = new List[game.maxPlayers];
        for (int pn = 0; pn < game.maxPlayers; pn++)
        {
            savedLRPaths[pn] = new ArrayList<SOCLRPathData>();
            savedLRPaths[pn].addAll(game.getPlayer(pn).getLRPaths());
        }

        int ourCurrentWGETA = ourPlayerTracker.getWinGameETA();
        D.ebugPrintlnINFO("ourCurrentWGETA = "+ourCurrentWGETA);

        int leadersCurrentWGETA = ourCurrentWGETA;
        for (final SOCPlayerTracker tracker : playerTrackers)
        {
            if (tracker == null)
                continue;

            int wgeta = tracker.getWinGameETA();
            if (wgeta < leadersCurrentWGETA) {
                leadersCurrentWGETA = wgeta;
            }
        }

    /*
    boolean goingToPlayRB = false;
    if (! ourPlayerData.hasPlayedDevCard() &&
        ourPlayerData.getNumPieces(SOCPlayingPiece.ROAD) >= 2 &&
        ourPlayerData.getInventory().getAmount(SOCInventory.OLD, SOCDevCardConstants.ROADS) > 0) {
      goingToPlayRB = true;
    }
    */

        ///
        /// score the possible settlements into threatenedSettlements and goodSettlements
        ///
        if (ourPlayerData.getNumPieces(SOCPlayingPiece.SETTLEMENT) > 0)
        {
            scorePossibleSettlements(buildingETAs[SOCBuildingSpeedEstimate.SETTLEMENT], leadersCurrentWGETA);
        }

        ///
        /// collect roads that we can build now into goodRoads
        ///
        if (ourPlayerData.getNumPieces(SOCPlayingPiece.ROAD) > 0)
        {
            Iterator<SOCPossibleRoad> posRoadsIter = ourPlayerTracker.getPossibleRoads().values().iterator();
            while (posRoadsIter.hasNext())
            {
                SOCPossibleRoad posRoad = posRoadsIter.next();
                if (! posRoad.isRoadNotShip())
                    continue;  // ignore ships in this loop, ships have other conditions to check

                if ((posRoad.getNecessaryRoads().isEmpty())
                        && (! threatenedRoads.contains(posRoad))
                        && (! goodRoads.contains(posRoad)))
                {
                    goodRoads.add(posRoad);
                }
            }
        }

        ///
        /// and collect ships we can build now
        /// (if the pirate is adjacent, can't build there right now)
        ///
        if (ourPlayerData.getNumPieces(SOCPlayingPiece.SHIP) > 0)
        {
            final SOCBoard board = game.getBoard();
            final int pirateHex =
                    (board instanceof SOCBoardLarge)
                            ? ((SOCBoardLarge) board).getPirateHex()
                            : 0;
            final int[] pirateEdges =
                    (pirateHex != 0)
                            ? ((SOCBoardLarge) board).getAdjacentEdgesToHex_arr(pirateHex)
                            : null;

            Iterator<SOCPossibleRoad> posRoadsIter = ourPlayerTracker.getPossibleRoads().values().iterator();
            while (posRoadsIter.hasNext())
            {
                final SOCPossibleRoad posRoad = posRoadsIter.next();
                if (posRoad.isRoadNotShip())
                    continue;  // ignore roads in this loop, we want ships

                if (posRoad.getNecessaryRoads().isEmpty()
                        && (! threatenedRoads.contains(posRoad))
                        && (! goodRoads.contains(posRoad)))
                {
                    boolean edgeOK = true;
                    if (pirateEdges != null)
                    {
                        final int edge = posRoad.getCoordinates();
                        for (int i = 0; i < pirateEdges.length; ++i)
                        {
                            if (edge == pirateEdges[i])
                            {
                                edgeOK = false;
                                break;
                            }
                        }
                    }

                    if (edgeOK)
                        goodRoads.add(posRoad);
                }
            }
        }

    /*
    ///
    /// check everything
    ///
    Enumeration threatenedSetEnum = threatenedSettlements.elements();
    while (threatenedSetEnum.hasMoreElements()) {
      SOCPossibleSettlement threatenedSet = (SOCPossibleSettlement)threatenedSetEnum.nextElement();
      D.ebugPrintlnINFO("*** threatened settlement at "+Integer.toHexString(threatenedSet.getCoordinates())+" has a score of "+threatenedSet.getScore());
      if (threatenedSet.getNecessaryRoads().isEmpty() &&
          ! ourPlayerData.isPotentialSettlement(threatenedSet.getCoordinates())) {
        D.ebugPrintlnINFO("POTENTIAL SETTLEMENT ERROR");
        //System.exit(0);
      }
    }
    Enumeration goodSetEnum = goodSettlements.elements();
    while (goodSetEnum.hasMoreElements()) {
      SOCPossibleSettlement goodSet = (SOCPossibleSettlement)goodSetEnum.nextElement();
      D.ebugPrintlnINFO("*** good settlement at "+Integer.toHexString(goodSet.getCoordinates())+" has a score of "+goodSet.getScore());
      if (goodSet.getNecessaryRoads().isEmpty() &&
          ! ourPlayerData.isPotentialSettlement(goodSet.getCoordinates())) {
        D.ebugPrintlnINFO("POTENTIAL SETTLEMENT ERROR");
        //System.exit(0);
      }
    }
    Enumeration threatenedRoadEnum = threatenedRoads.elements();
    while (threatenedRoadEnum.hasMoreElements()) {
      SOCPossibleRoad threatenedRoad = (SOCPossibleRoad)threatenedRoadEnum.nextElement();
      D.ebugPrintlnINFO("*** threatened road at "+Integer.toHexString(threatenedRoad.getCoordinates())+" has a score of "+threatenedRoad.getScore());
      if (threatenedRoad.getNecessaryRoads().isEmpty() &&
          ! ourPlayerData.isPotentialRoad(threatenedRoad.getCoordinates())) {
        D.ebugPrintlnINFO("POTENTIAL ROAD ERROR");
        //System.exit(0);
      }
    }
    Enumeration goodRoadEnum = goodRoads.elements();
    while (goodRoadEnum.hasMoreElements()) {
      SOCPossibleRoad goodRoad = (SOCPossibleRoad)goodRoadEnum.nextElement();
      D.ebugPrintlnINFO("*** good road at "+Integer.toHexString(goodRoad.getCoordinates())+" has a score of "+goodRoad.getScore());
      if (goodRoad.getNecessaryRoads().isEmpty() &&
          ! ourPlayerData.isPotentialRoad(goodRoad.getCoordinates())) {
        D.ebugPrintlnINFO("POTENTIAL ROAD ERROR");
        //System.exit(0);
      }
    }
    */

        D.ebugPrintlnINFO("PICKING WHAT TO BUILD");

        ///
        /// pick what we want to build
        ///

        ///
        /// pick favoriteSettlement that can be built now
        ///
        if (ourPlayerData.getNumPieces(SOCPlayingPiece.SETTLEMENT) > 0)
        {
            for (SOCPossibleSettlement threatenedSet : threatenedSettlements)
            {
                if (threatenedSet.getNecessaryRoads().isEmpty())
                {
                    D.ebugPrintlnINFO("$$$$$ threatened settlement at "+Integer.toHexString(threatenedSet.getCoordinates())+" has a score of "+threatenedSet.getScore());

                    if ((favoriteSettlement == null)
                            || (threatenedSet.getScore() > favoriteSettlement.getScore()))
                    {
                        favoriteSettlement = threatenedSet;
                    }
                }
            }

            for (SOCPossibleSettlement goodSet : goodSettlements)
            {
                if (goodSet.getNecessaryRoads().isEmpty())
                {
                    D.ebugPrintlnINFO("$$$$$ good settlement at "+Integer.toHexString(goodSet.getCoordinates())+" has a score of "+goodSet.getScore());

                    if ((favoriteSettlement == null)
                            || (goodSet.getScore() > favoriteSettlement.getScore()))
                    {
                        favoriteSettlement = goodSet;
                    }
                }
            }
        }

        //
        // restore the LRPath list
        //
        D.ebugPrintlnINFO("%%% RESTORING LRPATH LIST %%%");
        for (int pn = 0; pn < game.maxPlayers; pn++)
        {
            game.getPlayer(pn).setLRPaths(savedLRPaths[pn]);
        }

        ///
        /// pick a road that can be built now
        ///
        if (ourPlayerData.getNumPieces(SOCPlayingPiece.ROAD) > 0)
        {
            for (SOCPossibleRoad threatenedRoad : threatenedRoads)
            {
                D.ebugPrintlnINFO("$$$$$ threatened road at "+Integer.toHexString(threatenedRoad.getCoordinates()));

                if ((brain != null) && (brain.getDRecorder().isOn()))
                {
                    brain.getDRecorder().startRecording
                            ((threatenedRoad.isRoadNotShip() ? "ROAD" : "SHIP") + threatenedRoad.getCoordinates());
                    brain.getDRecorder().record("Estimate value of road at "
                            + game.getBoard().edgeCoordToString(threatenedRoad.getCoordinates()));
                }

                //
                // see how building this piece impacts our winETA
                //
                threatenedRoad.resetScore();
                float wgetaScore = getWinGameETABonusForRoad
                        (threatenedRoad, buildingETAs[SOCBuildingSpeedEstimate.ROAD], leadersCurrentWGETA, playerTrackers);
                if ((brain != null) && (brain.getDRecorder().isOn())) {
                    brain.getDRecorder().stopRecording();
                }

                D.ebugPrintlnINFO("wgetaScore = "+wgetaScore);

                if (favoriteRoad == null)
                {
                    favoriteRoad = threatenedRoad;
                } else {
                    if (threatenedRoad.getScore() > favoriteRoad.getScore())
                        favoriteRoad = threatenedRoad;
                }
            }

            for (SOCPossibleRoad goodRoad : goodRoads)
            {
                D.ebugPrintlnINFO("$$$$$ good road at "+Integer.toHexString(goodRoad.getCoordinates()));

                if ((brain != null) && (brain.getDRecorder().isOn()))
                {
                    brain.getDRecorder().startRecording
                            ( (goodRoad.isRoadNotShip() ? "ROAD" : "SHIP") + goodRoad.getCoordinates());
                    brain.getDRecorder().record("Estimate value of road at "
                            + game.getBoard().edgeCoordToString(goodRoad.getCoordinates()));
                }

                //
                // see how building this piece impacts our winETA
                //
                // TODO better ETA scoring for coastal ships/roads
                //
                goodRoad.resetScore();
                final int etype =
                        ((goodRoad instanceof SOCPossibleShip) && ! ((SOCPossibleShip) goodRoad).isCoastalRoadAndShip)
                                ? SOCBuildingSpeedEstimate.ROAD
                                : SOCBuildingSpeedEstimate.SHIP;
                float wgetaScore = getWinGameETABonusForRoad(goodRoad, buildingETAs[etype], leadersCurrentWGETA, playerTrackers);
                if ((brain != null) && (brain.getDRecorder().isOn())) {
                    brain.getDRecorder().stopRecording();
                }

                D.ebugPrintlnINFO("wgetaScore = "+wgetaScore);

                if (favoriteRoad == null)
                {
                    favoriteRoad = goodRoad;
                } else {
                    if (goodRoad.getScore() > favoriteRoad.getScore())
                        favoriteRoad = goodRoad;
                }
            }
        }

        //
        // restore the LRPath list
        //
        D.ebugPrintlnINFO("%%% RESTORING LRPATH LIST %%%");
        for (int pn = 0; pn < game.maxPlayers; pn++) {
            game.getPlayer(pn).setLRPaths(savedLRPaths[pn]);
        }

        ///
        /// pick a city that can be built now
        ///
        if (ourPlayerData.getNumPieces(SOCPlayingPiece.CITY) > 0)
        {
            SOCPlayerTracker[] trackersCopy = EvolutionaryPlayerTracker.copyPlayerTrackers(playerTrackers);
            SOCPlayerTracker ourTrackerCopy = trackersCopy[ourPlayerNumber];
            int originalWGETAs[] = new int[game.maxPlayers];
            int WGETAdiffs[] = new int[game.maxPlayers];
            Vector<SOCPlayerTracker> leaders = new Vector<SOCPlayerTracker>();
            int bestWGETA = 1000;
            // int bonus = 0;

            Iterator<SOCPossibleCity> posCitiesIter = ourPlayerTracker.getPossibleCities().values().iterator();
            while (posCitiesIter.hasNext())
            {
                SOCPossibleCity posCity = posCitiesIter.next();
                if ((brain != null) && (brain.getDRecorder().isOn()))
                {
                    brain.getDRecorder().startRecording("CITY"+posCity.getCoordinates());
                    brain.getDRecorder().record("Estimate value of city at "
                            + game.getBoard().nodeCoordToString(posCity.getCoordinates()));
                }

                //
                // see how building this piece impacts our winETA
                //
                leaders.clear();
                if ((brain != null) && (brain.getDRecorder().isOn())) {
                    brain.getDRecorder().suspend();
                }
                SOCPlayerTracker.updateWinGameETAs(trackersCopy);

                // TODO refactor? This section is like a copy of calcWGETABonus, with something added in the middle

                for (final SOCPlayerTracker trackerBefore : trackersCopy)
                {
                    if (trackerBefore == null)
                        continue;

                    final int pn = trackerBefore.getPlayer().getPlayerNumber();
                    D.ebugPrintlnINFO("$$$ win game ETA for player " + pn + " = " + trackerBefore.getWinGameETA());
                    originalWGETAs[pn] = trackerBefore.getWinGameETA();
                    WGETAdiffs[pn] = trackerBefore.getWinGameETA();
                    if (trackerBefore.getWinGameETA() < bestWGETA)
                    {
                        bestWGETA = trackerBefore.getWinGameETA();
                        leaders.removeAllElements();
                        leaders.addElement(trackerBefore);
                    } else if (trackerBefore.getWinGameETA() == bestWGETA) {
                        leaders.addElement(trackerBefore);
                    }
                }
                D.ebugPrintlnINFO("^^^^ bestWGETA = "+bestWGETA);
                if ((brain != null) && (brain.getDRecorder().isOn())) {
                    brain.getDRecorder().resume();
                }
                //
                // place the city
                //
                SOCCity tmpCity = new SOCCity(ourPlayerData, posCity.getCoordinates(), null);
                game.putTempPiece(tmpCity);

                ourTrackerCopy.addOurNewCity(tmpCity);

                SOCPlayerTracker.updateWinGameETAs(trackersCopy);

                float wgetaScore = calcWGETABonusAux(originalWGETAs, trackersCopy, leaders);

                //
                // remove the city
                //
                ourTrackerCopy.undoAddOurNewCity(posCity);
                game.undoPutTempPiece(tmpCity);

                D.ebugPrintlnINFO("*** ETA for city = "+buildingETAs[SOCBuildingSpeedEstimate.CITY]);
                if ((brain != null) && (brain.getDRecorder().isOn())) {
                    brain.getDRecorder().record("ETA = "+buildingETAs[SOCBuildingSpeedEstimate.CITY]);
                }

                float etaBonus = getETABonus(buildingETAs[SOCBuildingSpeedEstimate.CITY], leadersCurrentWGETA, wgetaScore);
                D.ebugPrintlnINFO("etaBonus = "+etaBonus);

                posCity.addToScore(etaBonus);
                //posCity.addToScore(wgetaScore);

                if ((brain != null) && (brain.getDRecorder().isOn()))
                {
                    brain.getDRecorder().record("WGETA score = "+df1.format(wgetaScore));
                    brain.getDRecorder().record("Total city score = "+df1.format(etaBonus));
                    brain.getDRecorder().stopRecording();
                }

                D.ebugPrintlnINFO("$$$  final score = "+posCity.getScore());

                D.ebugPrintlnINFO("$$$$$ possible city at "+Integer.toHexString(posCity.getCoordinates())+" has a score of "+posCity.getScore());

                if ((favoriteCity == null)
                        || (posCity.getScore() > favoriteCity.getScore()))
                {
                    favoriteCity = posCity;
                }
            }
        }

        if (favoriteSettlement != null) {
            D.ebugPrintlnINFO("### FAVORITE SETTLEMENT IS AT "+Integer.toHexString(favoriteSettlement.getCoordinates()));
            D.ebugPrintlnINFO("###   WITH A SCORE OF "+favoriteSettlement.getScore());
            D.ebugPrintlnINFO("###   WITH AN ETA OF "+buildingETAs[SOCBuildingSpeedEstimate.SETTLEMENT]);
            D.ebugPrintlnINFO("###   WITH A TOTAL SPEEDUP OF "+favoriteSettlement.getSpeedupTotal());
        }

        if (favoriteCity != null) {
            D.ebugPrintlnINFO("### FAVORITE CITY IS AT "+Integer.toHexString(favoriteCity.getCoordinates()));
            D.ebugPrintlnINFO("###   WITH A SCORE OF "+favoriteCity.getScore());
            D.ebugPrintlnINFO("###   WITH AN ETA OF "+buildingETAs[SOCBuildingSpeedEstimate.CITY]);
            D.ebugPrintlnINFO("###   WITH A TOTAL SPEEDUP OF "+favoriteCity.getSpeedupTotal());
        }

        final int road_eta_type =
                ((favoriteRoad != null) && (favoriteRoad instanceof SOCPossibleShip)
                        && ! ((SOCPossibleShip) favoriteRoad).isCoastalRoadAndShip)  // TODO better ETA calc for coastal roads/ships
                        ? SOCBuildingSpeedEstimate.SHIP
                        : SOCBuildingSpeedEstimate.ROAD;

        if (favoriteRoad != null) {
            D.ebugPrintlnINFO("### FAVORITE ROAD IS AT "+Integer.toHexString(favoriteRoad.getCoordinates()));
            D.ebugPrintlnINFO("###   WITH AN ETA OF "+buildingETAs[road_eta_type]);
            D.ebugPrintlnINFO("###   WITH A SCORE OF "+favoriteRoad.getScore());
        }

        int pick = -1;  // piece type, if any, to be pushed onto buildingPlan;
        // use ROAD for road or ship, use MAXPLUSONE for dev card

        float pickScore = 0f;  // getScore() of picked piece

        ///
        /// if the favorite settlement and road can wait, and
        /// favoriteCity has the best score and ETA, then build the city
        ///
        if ((favoriteCity != null) &&
                (ourPlayerData.getNumPieces(SOCPlayingPiece.CITY) > 0) &&
                (favoriteCity.getScore() > 0) &&
                ((favoriteSettlement == null) ||
                        (ourPlayerData.getNumPieces(SOCPlayingPiece.SETTLEMENT) == 0) ||
                        (favoriteCity.getScore() > favoriteSettlement.getScore()) ||
                        ((favoriteCity.getScore() == favoriteSettlement.getScore()) &&
                                (buildingETAs[SOCBuildingSpeedEstimate.CITY] < buildingETAs[SOCBuildingSpeedEstimate.SETTLEMENT]))) &&
                ((favoriteRoad == null) ||
                        (ourPlayerData.getNumPieces(favoriteRoad.getType()) == 0) ||
                        (favoriteCity.getScore() > favoriteRoad.getScore()) ||
                        ((favoriteCity.getScore() == favoriteRoad.getScore()) &&
                                (buildingETAs[SOCBuildingSpeedEstimate.CITY] < buildingETAs[road_eta_type]))))
        {
            D.ebugPrintlnINFO("### PICKED FAVORITE CITY");
            pick = SOCPlayingPiece.CITY;
            pickScore = favoriteCity.getScore();
        }

        ///
        /// if there is a road with a better score than
        /// our favorite settlement, then build the road,
        /// else build the settlement
        ///
        else if ((favoriteRoad != null)
                && (ourPlayerData.getNumPieces(favoriteRoad.getType()) > 0)
                && (favoriteRoad.getScore() > 0)
                && ((favoriteSettlement == null)
                || (ourPlayerData.getNumPieces(SOCPlayingPiece.SETTLEMENT) == 0)
                || (favoriteSettlement.getScore() < favoriteRoad.getScore())))
        {
            D.ebugPrintlnINFO("### PICKED FAVORITE ROAD");
            pick = SOCPlayingPiece.ROAD;  // also represents SHIP here
            pickScore = favoriteRoad.getScore();
        }
        else if ((favoriteSettlement != null)
                && (ourPlayerData.getNumPieces(SOCPlayingPiece.SETTLEMENT) > 0))
        {
            D.ebugPrintlnINFO("### PICKED FAVORITE SETTLEMENT");
            pick = SOCPlayingPiece.SETTLEMENT;
            pickScore = favoriteSettlement.getScore();
        }

        ///
        /// if buying a card is better than building...
        ///

        //
        // see how buying a card improves our win game ETA
        //
        float devCardScore = 0;
        if ((game.getNumDevCards() > 0) && ! forSpecialBuildingPhase)
        {
            if ((brain != null) && (brain.getDRecorder().isOn())) {
                brain.getDRecorder().startRecording("DEVCARD");
                brain.getDRecorder().record("Estimate value of a dev card");
            }

            possibleCard = getDevCardScore(buildingETAs[SOCBuildingSpeedEstimate.CARD], leadersCurrentWGETA);
            devCardScore = possibleCard.getScore();
            D.ebugPrintlnINFO("### DEV CARD SCORE: "+devCardScore);
            if ((brain != null) && (brain.getDRecorder().isOn())) {
                brain.getDRecorder().stopRecording();
            }

            if ((pick == -1)
                    || (devCardScore > pickScore))
            {
                D.ebugPrintlnINFO("### BUY DEV CARD");
                pick = SOCPlayingPiece.MAXPLUSONE;
                pickScore = devCardScore;
            }
        }

        if (game.isGameOptionSet(SOCGameOption.K_SC_PIRI)
                || game.isGameOptionSet(SOCGameOption.K_SC_WOND))
        {
            if (scenarioGameStrategyPlan
                    (pickScore, devCardScore, true, (pick == SOCPlayingPiece.MAXPLUSONE),
                            getEstimator(ourPlayerData.getNumbers()),
                            leadersCurrentWGETA, forSpecialBuildingPhase))
                return;  // <--- Early return: Scenario-specific buildingPlan was pushed ---
        }

        //
        // push our picked piece onto buildingPlan
        //
        switch (pick)
        {
            case SOCPlayingPiece.ROAD:
                D.ebugPrintlnINFO("$ PUSHING " + favoriteRoad);
                buildingPlan.push(favoriteRoad);
                break;

            case SOCPlayingPiece.SETTLEMENT:
                D.ebugPrintlnINFO("$ PUSHING " + favoriteSettlement);
                buildingPlan.push(favoriteSettlement);
                break;

            case SOCPlayingPiece.CITY:
                D.ebugPrintlnINFO("$ PUSHING " + favoriteCity);
                buildingPlan.push(favoriteCity);
                break;

            case SOCPlayingPiece.MAXPLUSONE:
                D.ebugPrintlnINFO("$ PUSHING " + possibleCard);
                buildingPlan.push(possibleCard);
                break;
        }

    }

    @Override
    protected void scorePossibleSettlements(final int settlementETA, final int leadersCurrentWGETA)
    {
        D.ebugPrintlnINFO("****** scorePossibleSettlements");
        // int ourCurrentWGETA = ourPlayerTracker.getWinGameETA();

    /*
    boolean goingToPlayRB = false;
    if (! ourPlayerData.hasPlayedDevCard() &&
        ourPlayerData.getNumPieces(SOCPlayingPiece.ROAD) >= 2 &&
        ourPlayerData.getInventory().getAmount(SOCInventory.OLD, SOCDevCardConstants.ROADS) > 0) {
      goingToPlayRB = true;
    }
    */

        Iterator<SOCPossibleSettlement> posSetsIter = ourPlayerTracker.getPossibleSettlements().values().iterator();
        while (posSetsIter.hasNext())
        {
            SOCPossibleSettlement posSet = posSetsIter.next();
            D.ebugPrintlnINFO("*** scoring possible settlement at "+Integer.toHexString(posSet.getCoordinates()));
            if (! threatenedSettlements.contains(posSet))
            {
                threatenedSettlements.add(posSet);
            } else if (! goodSettlements.contains(posSet)) {
                goodSettlements.add(posSet);
            }

            //
            // only consider settlements we can build now
            //
            if (posSet.getNecessaryRoads().isEmpty())
            {
                D.ebugPrintlnINFO("*** no roads needed");
                //
                //  no roads needed
                //
                //
                //  get wgeta score
                //
                SOCBoard board = game.getBoard();
                SOCSettlement tmpSet = new SOCSettlement(ourPlayerData, posSet.getCoordinates(), board);
                if ((brain != null) && (brain.getDRecorder().isOn())) {
                    brain.getDRecorder().startRecording("SETTLEMENT"+posSet.getCoordinates());
                    brain.getDRecorder().record("Estimate value of settlement at "+board.nodeCoordToString(posSet.getCoordinates()));
                }

                SOCPlayerTracker[] trackersCopy = EvolutionaryPlayerTracker.tryPutPiece(tmpSet, game, playerTrackers);
                SOCPlayerTracker.updateWinGameETAs(trackersCopy);
                float wgetaScore = calcWGETABonus(playerTrackers, trackersCopy);
                D.ebugPrintlnINFO("***  wgetaScore = "+wgetaScore);

                D.ebugPrintlnINFO("*** ETA for settlement = "+settlementETA);
                if ((brain != null) && (brain.getDRecorder().isOn())) {
                    brain.getDRecorder().record("ETA = "+settlementETA);
                }

                float etaBonus = getETABonus(settlementETA, leadersCurrentWGETA, wgetaScore);
                D.ebugPrintlnINFO("etaBonus = "+etaBonus);

                //posSet.addToScore(wgetaScore);
                posSet.addToScore(etaBonus);

                if ((brain != null) && (brain.getDRecorder().isOn())) {
                    brain.getDRecorder().record("WGETA score = "+df1.format(wgetaScore));
                    brain.getDRecorder().record("Total settlement score = "+df1.format(etaBonus));
                    brain.getDRecorder().stopRecording();
                }

                SOCPlayerTracker.undoTryPutPiece(tmpSet, game);
            }
        }
    }

    @Override
    protected float getWinGameETABonus(final SOCPossiblePiece posPiece)
    {
        SOCPlayerTracker[] trackersCopy = null;
        SOCSettlement tmpSet = null;
        SOCCity tmpCity = null;
        SOCRoutePiece tmpRS = null;  // road or ship
        float bonus = 0;

        D.ebugPrintlnINFO("--- before [start] ---");
        //SOCPlayerTracker.playerTrackersDebug(playerTrackers);
        D.ebugPrintlnINFO("our player numbers = "+ourPlayerData.getNumbers());
        D.ebugPrintlnINFO("--- before [end] ---");

        switch (posPiece.getType())
        {
            case SOCPossiblePiece.SETTLEMENT:
                tmpSet = new SOCSettlement(ourPlayerData, posPiece.getCoordinates(), null);
                trackersCopy = EvolutionaryPlayerTracker.tryPutPiece(tmpSet, game, playerTrackers);
                break;

            case SOCPossiblePiece.CITY:
                trackersCopy = EvolutionaryPlayerTracker.copyPlayerTrackers(playerTrackers);
                tmpCity = new SOCCity(ourPlayerData, posPiece.getCoordinates(), null);
                game.putTempPiece(tmpCity);
                SOCPlayerTracker trackerCopy = trackersCopy[ourPlayerNumber];
                if (trackerCopy != null) {
                    trackerCopy.addOurNewCity(tmpCity);
                }
                break;

            case SOCPossiblePiece.ROAD:
                tmpRS = new SOCRoad(ourPlayerData, posPiece.getCoordinates(), null);
                trackersCopy = EvolutionaryPlayerTracker.tryPutPiece(tmpRS, game, playerTrackers);
                break;

            case SOCPossiblePiece.SHIP:
                tmpRS = new SOCShip(ourPlayerData, posPiece.getCoordinates(), null);
                trackersCopy = EvolutionaryPlayerTracker.tryPutPiece(tmpRS, game, playerTrackers);
                break;
        }

        //trackersCopyIter = trackersCopy.iterator();
        //while (trackersCopyIter.hasNext()) {
        //        SOCPlayerTracker trackerCopy = (SOCPlayerTracker)trackersCopyIter.next();
        //        trackerCopy.updateThreats(trackersCopy);
        //}

        D.ebugPrintlnINFO("--- after [start] ---");
        //SOCPlayerTracker.playerTrackersDebug(trackersCopy);
        SOCPlayerTracker.updateWinGameETAs(trackersCopy);

        float WGETABonus = calcWGETABonus(playerTrackers, trackersCopy);
        D.ebugPrintlnINFO("$$$ win game ETA bonus : +"+WGETABonus);
        bonus = WGETABonus;

        D.ebugPrintlnINFO("our player numbers = "+ourPlayerData.getNumbers());
        D.ebugPrintlnINFO("--- after [end] ---");

        switch (posPiece.getType())
        {
            case SOCPossiblePiece.SETTLEMENT:
                SOCPlayerTracker.undoTryPutPiece(tmpSet, game);
                break;

            case SOCPossiblePiece.CITY:
                game.undoPutTempPiece(tmpCity);
                break;

            case SOCPossiblePiece.SHIP:  // fall through to ROAD
            case SOCPossiblePiece.ROAD:
                SOCPlayerTracker.undoTryPutPiece(tmpRS, game);
                break;
        }

        D.ebugPrintlnINFO("our player numbers = "+ourPlayerData.getNumbers());
        D.ebugPrintlnINFO("--- cleanup done ---");

        return bonus;
    }

    @Override
    protected float getWinGameETABonusForRoad
            (final SOCPossibleRoad posRoad, final int roadETA, final int leadersCurrentWGETA,
             final SOCPlayerTracker[] plTrackers)
    {
        D.ebugPrintlnINFO("--- addWinGameETABonusForRoad");
        int ourCurrentWGETA = ourPlayerTracker.getWinGameETA();
        D.ebugPrintlnINFO("ourCurrentWGETA = "+ourCurrentWGETA);

        SOCPlayerTracker[] trackersCopy = null;
        SOCRoutePiece tmpRS = null;
        // Building road or ship?  TODO Better ETA calc for coastal road/ship
        final boolean isShip = (posRoad instanceof SOCPossibleShip)
                && ! ((SOCPossibleShip) posRoad).isCoastalRoadAndShip;
        final SOCResourceSet rsrcs = (isShip ? SOCShip.COST : SOCRoad.COST);

        D.ebugPrintlnINFO("--- before [start] ---");
        SOCResourceSet originalResources = ourPlayerData.getResources().copy();
        SOCBuildingSpeedEstimate estimate = getEstimator(ourPlayerData.getNumbers());
        //SOCPlayerTracker.playerTrackersDebug(playerTrackers);
        D.ebugPrintlnINFO("--- before [end] ---");
        try
        {
            SOCResSetBuildTimePair btp = estimate.calculateRollsAndRsrcFast
                    (ourPlayerData.getResources(), rsrcs, 50, ourPlayerData.getPortFlags());
            btp.getResources().subtract(rsrcs);
            ourPlayerData.getResources().setAmounts(btp.getResources());
        } catch (CutoffExceededException e) {
            D.ebugPrintlnINFO("crap in getWinGameETABonusForRoad - "+e);
        }
        tmpRS = (isShip)
                ? new SOCShip(ourPlayerData, posRoad.getCoordinates(), null)
                : new SOCRoad(ourPlayerData, posRoad.getCoordinates(), null);

        trackersCopy = EvolutionaryPlayerTracker.tryPutPiece(tmpRS, game, plTrackers);
        SOCPlayerTracker.updateWinGameETAs(trackersCopy);
        float score = calcWGETABonus(plTrackers, trackersCopy);

        if (! posRoad.getThreats().isEmpty())
        {
            score *= threatMultiplier;
            D.ebugPrintlnINFO("***  (THREAT MULTIPLIER) score * "+threatMultiplier+" = "+score);
        }
        D.ebugPrintlnINFO("*** ETA for road = "+roadETA);
        float etaBonus = getETABonus(roadETA, leadersCurrentWGETA, score);
        D.ebugPrintlnINFO("$$$ score = "+score);
        D.ebugPrintlnINFO("etaBonus = "+etaBonus);
        posRoad.addToScore(etaBonus);

        if ((brain != null) && (brain.getDRecorder().isOn())) {
            brain.getDRecorder().record("ETA = "+roadETA);
            brain.getDRecorder().record("WGETA Score = "+df1.format(score));
            brain.getDRecorder().record("Total road score = "+df1.format(etaBonus));
        }

        D.ebugPrintlnINFO("--- after [end] ---");
        SOCPlayerTracker.undoTryPutPiece(tmpRS, game);
        ourPlayerData.getResources().clear();
        ourPlayerData.getResources().add(originalResources);
        D.ebugPrintlnINFO("--- cleanup done ---");

        return etaBonus;
    }




}
