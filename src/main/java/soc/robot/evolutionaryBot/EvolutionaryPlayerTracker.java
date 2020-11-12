package soc.robot.evolutionaryBot;

import com.google.gson.Gson;
import soc.disableDebug.D;
import soc.game.*;
import soc.robot.*;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class EvolutionaryPlayerTracker extends SOCPlayerTracker {

    // TODO probably want something like "if we have a file that matches our name, make our tree from that, otherwise
    // TODO Generate a new random tree with max depth d

    private GeneticTree winGameAlgorithm;

    public EvolutionaryPlayerTracker(SOCPlayer pl, SOCRobotBrain br) {
        super(pl, br);
        winGameAlgorithm = new GeneticTree();
    }

    public EvolutionaryPlayerTracker(SOCPlayerTracker pt) {
        super(pt);
        winGameAlgorithm = new GeneticTree();
    }

    /**
     * Inner class that contains the genetic algorithm tree
     * <p>
     * Notes: When adding a new input/operation to the tree, updates need to be made in 3 different places:
     * 1. Instance variables, where we define the string that represents this input
     * 2. Either in setUpInputs or setUpOperations. This is where we add the input/operation to its respective list
     * 3.  - For new inputs: getInputVal() in the TreeInput inner class. This is where we actually set the value of the new input
     * - For new operations: calculateTree() in the TreeNode inner class. This is where we say what to do if we encounter that operation
     */
    private class GeneticTree {

        /**
         * All of the different inputs we want our algorithm to be able to take
         */
        private final String TIME_TO_LONGEST_ROAD = "Time To Longest Road";
        private final String ROADS_TO_GO = "Roads To Go";
        private final String KNIGHTS_TO_GO = "Knights To Go";
        private final String LARGEST_ARMY_ETA = "Largest Army ETA";
        private final String TOTAL_RESOURCE_INCOME = "Total Resource Income";
        private final String WHEAT_INCOME = "Wheat Income";
        private final String SHEEP_INCOME = "Sheep Income";
        private final String ORE_INCOME = "Ore Income";
        private final String BRICK_INCOME = "Brick Income";
        private final String LOG_INCOME = "Log Income";
        private final String CURRENT_WHEAT = "Current Wheat";
        private final String CURRENT_SHEEP = "Current Sheep";
        private final String CURRENT_ORE = "Current Ore";
        private final String CURRENT_LOG = "Current Log";
        private final String CURRENT_BRICK = "Current Brick";
        private final String TOTAL_RESOURCES = "Total Resources";
        private final String CURRENT_VP = "Current VP";
        private final String PORT_COUNT = "Port Count";
        private final String DEV_CARD_COUNT = "Dev Card Count";
        private final String BUILD_LOCATION_COUNT = "Build Location Count"; //remaining possible settlement locations on map
        private final String READY_BUILD_SPOT_COUNT = "Ready Build Spot Count"; //Number of spots you can build a settlement now
        private final String SETTLEMENT_ETA = "Settlement ETA";
        private final String CITY_ETA = "City ETA";
        private final String ROAD_ETA = "Road ETA";
        private final String DEV_CARD_ETA = "Development Card ETA";

        /**
         * All of the different operations we want our algorithm to support in the tree
         */
        private final String ADD = "+";
        private final String MINUS = "-";
        private final String MULTIPLY = "*";
        private final String DIVIDE = "/";
        private final String GREATER = ">";
        private final String LESS = "<";


        /**
         * The two different node types in the tree
         */
        private final int OPERATOR_TYPE = 0;
        private final int INPUT_TYPE = 1;

        /**
         * Lists that hold all the operations and inputs above
         */
        private ArrayList<TreeInput> inputs = new ArrayList<>();
        private ArrayList<String> operations = new ArrayList<>();
        private TreeNode root;
        private Random random = new Random();
        private int maxDepth = 5;
        Gson gson = new Gson();


        /**
         * These are the objects that actually make up the tree.
         * <p>
         * Each node has a type, either OPERATOR_TYPE or INPUT_TYPE
         * INPUT_TYPE nodes are guaranteed to be a leaf
         * OPERATOR_TYPE nodes are guaranteed to not be a leaf and have children left and right that are both TreeNodes
         * The children of an Operator type node can be of either type
         */
        private class TreeNode {
            private int type;
            private int nodeDepth;

            private TreeNode left = null;
            private TreeNode right = null;
            private TreeInput value = null;
            private String operator = null;

            /**
             * Constructor for INPUT_TYPE TreeNode
             */
            private TreeNode(TreeInput val, int depth) {
                type = INPUT_TYPE;
                value = val;
                nodeDepth = depth;
            }

            /**
             * Constructor for OPERATOR_TYPE TreeNode
             */
            private TreeNode(String op, TreeNode l, TreeNode r, int depth) {
                type = OPERATOR_TYPE;
                nodeDepth = depth;
                operator = op;
                left = l;
                right = r;
            }

            /**
             * Calculates the value returned when evaluating a tree starting at this node
             */
            private int calculateTree() {
                if (type == INPUT_TYPE) {
                    return value.getInputVal();
                }

                switch (operator) {
                    case ADD:
                        return left.calculateTree() + right.calculateTree();
                    case MINUS:
                        return left.calculateTree() - right.calculateTree();
                    case MULTIPLY:
                        return left.calculateTree() * right.calculateTree();
                    case DIVIDE:
                        return Math.round((float) left.calculateTree() / right.calculateTree());
                    case GREATER:
                        if (left.calculateTree() > right.calculateTree()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    case LESS:
                        if (left.calculateTree() < right.calculateTree()) {
                            return 1;
                        } else {
                            return 0;
                        }
                }
                return -1;
            }

            /**
             * Mutates a particular Node.
             *
             * operatorProbability is the probability that we mutate the node to be an operator. We can set this to
             * -1 to just randomly choose among all possible nodes. Doing this, however, favors choosing inputs because
             * there are so many more inputs than operators.
             */
            private void mutate(int operatorProbability) {
                if (operatorProbability == -1) {
                    operatorProbability = Math.round(((float)operations.size() / (operations.size() + inputs.size())) * 100);
                }
                int probability = random.nextInt(100);

                if (operatorProbability > probability && nodeDepth + 1 < maxDepth) { // operator mutation
                    operator = getRandomOperation();
                    type = OPERATOR_TYPE;
                    value = null;
                    left = make_random_child(operatorProbability, nodeDepth + 1);
                    right = make_random_child(operatorProbability, nodeDepth + 1);
                } else { //input mutation
                    operator = null;
                    left = null;
                    right = null;
                    type = INPUT_TYPE;
                    value = getRandomInput();
                }

            }

            private TreeNode make_random_child(int operatorProbability, int newDepth) {
                TreeNode newChild;
                if (operatorProbability == -1) {
                    operatorProbability = Math.round(((float) operations.size() / (operations.size() + inputs.size())) * 100);
                }
                int probability = random.nextInt(100);

                if (operatorProbability > probability && newDepth + 1 < maxDepth) { // new operator
                    TreeNode child_left = make_random_child(operatorProbability, newDepth + 1);
                    TreeNode child_right = make_random_child(operatorProbability, newDepth + 1);
                    newChild = new TreeNode(getRandomOperation(), child_left, child_right, newDepth);
                } else { // new input
                    newChild = new TreeNode(getRandomInput(), newDepth);
                }
                return newChild;

            }
        }

        /**
         * Inner class representing an input value for the tree. Used by the INPUT_TYPE TreeNodes
         */
        private class TreeInput {

            /**
             * Name of the input type. Should be one of the input instance variables defined in the GeneticTree class
             */
            private String inputName;

            private TreeInput(String name) {
                inputName = name;
            }

            /**
             * Returns the up to date input value that corresponds with the particular input's name
             */
            private int getInputVal() {
                switch (inputName) {
                    case TIME_TO_LONGEST_ROAD:
                        return longestRoadETA;
                    case ROADS_TO_GO:
                        return roadsToGo;
                    case KNIGHTS_TO_GO:
                        return knightsToBuy;
                    case LARGEST_ARMY_ETA:
                        return largestArmyETA;
                    case TOTAL_RESOURCE_INCOME:
                        return 0; //TODO
                    case WHEAT_INCOME:
                        return 0; //TODO
                    case SHEEP_INCOME:
                        return 0; //TODO
                    case ORE_INCOME:
                        return 0; //TODO
                    case BRICK_INCOME:
                        return 0; //TODO
                    case LOG_INCOME:
                        return 0; //TODO
                    case CURRENT_WHEAT:
                        return 0; //TODO
                    case CURRENT_SHEEP:
                        return 0; //TODO
                    case CURRENT_ORE:
                        return 0; //TODO
                    case CURRENT_LOG:
                        return 0; //TODO
                    case CURRENT_BRICK:
                        return 0; //TODO
                    case TOTAL_RESOURCES:
                        return 0; //TODO
                    case CURRENT_VP:
                        return 0; //TODO
                    case PORT_COUNT:
                        return 0; //TODO
                    case DEV_CARD_COUNT:
                        return 0; //TODO
                    case BUILD_LOCATION_COUNT:
                        return 0; //TODO
                    case READY_BUILD_SPOT_COUNT:
                        return 0; //TODO
                    case SETTLEMENT_ETA:
                        return 0; //TODO
                    case CITY_ETA:
                        return 0; //TODO
                    case DEV_CARD_ETA:
                        return 0; //TODO
                    case ROAD_ETA:
                        return 0; //TODO
                }
                return -1;
            }
        }

        /**
         * Generates a random genetic tree that initializes itself by mutating mutationCount times
         */
        private GeneticTree(){
            setUpInput();
            setUpOperations();
            root = new TreeNode(getRandomInput(), 1);
            root.mutate(90);
        }

        /**
         * reads in a genetic tree from a file
         */
        private GeneticTree(String inputFile) {
            setUpInput();
            setUpOperations();

            //TODO get the rootJson (first line) from a file

            // TreeNode root = gson.fromJson(rootJson, TreeNode.class);
        }

        /**
         * puts each input we want to possibly use into the inputs list
         */
        private void setUpInput() {
            inputs.add(new TreeInput(TIME_TO_LONGEST_ROAD));
            inputs.add(new TreeInput(ROADS_TO_GO));
            inputs.add(new TreeInput(LARGEST_ARMY_ETA));
            inputs.add(new TreeInput(KNIGHTS_TO_GO));
            inputs.add(new TreeInput(TOTAL_RESOURCE_INCOME));
            inputs.add(new TreeInput(WHEAT_INCOME));
            inputs.add(new TreeInput(SHEEP_INCOME));
            inputs.add(new TreeInput(ORE_INCOME));
            inputs.add(new TreeInput(BRICK_INCOME));
            inputs.add(new TreeInput(LOG_INCOME));
            inputs.add(new TreeInput(CURRENT_WHEAT));
            inputs.add(new TreeInput(CURRENT_BRICK));
            inputs.add(new TreeInput(CURRENT_LOG));
            inputs.add(new TreeInput(CURRENT_ORE));
            inputs.add(new TreeInput(CURRENT_SHEEP));
            inputs.add(new TreeInput(TOTAL_RESOURCES));
            inputs.add(new TreeInput(CURRENT_VP));
            inputs.add(new TreeInput(PORT_COUNT));
            inputs.add(new TreeInput(DEV_CARD_COUNT));
            inputs.add(new TreeInput(BUILD_LOCATION_COUNT));
            inputs.add(new TreeInput(READY_BUILD_SPOT_COUNT));
            inputs.add(new TreeInput(SETTLEMENT_ETA));
            inputs.add(new TreeInput(DEV_CARD_ETA));
            inputs.add(new TreeInput(ROAD_ETA));
            inputs.add(new TreeInput(CITY_ETA));
        }

        /**
         * puts each operation we want to possibly use into the operations list
         */
        private void setUpOperations() {
            operations.add(ADD);
            operations.add(MINUS);
            operations.add(MULTIPLY);
            operations.add(DIVIDE);
            operations.add(GREATER);
            operations.add(LESS);
        }


        /**
         * returns the win ETA
         */
        private int calculateWinEta() {
            return root.calculateTree();
        }

        /**
         * mutates a random node in the tree
         */
        private void mutate() {
            getRandomNode().mutate(50);
        }

        /**
         * Get a random node from the tree
         *
         */
        private TreeNode getRandomNode() {
            ArrayList<TreeNode> allNodes = new ArrayList<>();
            getAllNodesInTree(root, allNodes);
            int size = allNodes.size();
            int index = random.nextInt(size);
            return allNodes.get(index);
        }

        /**
         *
         * updates allNodes to contain the passed in TreeNode node and all of its children.
         */
        private void getAllNodesInTree(TreeNode node, ArrayList<TreeNode> allNodes) {
            allNodes.add(node);
            if (node.type == OPERATOR_TYPE) {
                getAllNodesInTree(root.left, allNodes);
                getAllNodesInTree(root.right, allNodes);
            }
        }

        /**
         * prints the tree to a json file
         */
        private void treeToFile() {
            String rootJson = gson.toJson(root);
            // TODO Still need to right this string to a file

        }

        private void printReadableTree() {
            // TODO
        }

        /**
         * returns a random TreeInput from inputs
         */
        private TreeInput getRandomInput() {
            int size = inputs.size();
            int index = random.nextInt(size);
            return inputs.get(index);
        }

        /**
         * returns a random operation from operations
         */
        private String getRandomOperation() {
            int size = operations.size();
            int index = random.nextInt(size);
            return operations.get(index);
        }

    }


    @Override
    public void recalcWinGameETA() {
        winGameETA = winGameAlgorithm.calculateWinEta();
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
