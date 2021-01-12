package soc.robot.evolutionaryBot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.message.SOCMessage;
import soc.robot.SOCPlayerTracker;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.robot.SOCRobotDM;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class EvolutionaryBotBrain extends SOCRobotBrain {

    public static final int OPERATOR_TYPE = 0;
    public static final int INPUT_TYPE = 1;
    public static final int MAX_DEPTH = 5;

    public EvolutionaryBotBrain(SOCRobotClient rc, SOCRobotParameters params, SOCGame ga, CappedQueue<SOCMessage> mq) {
        super(rc, params, ga, mq);

        System.out.println("An Evolutionary bot has been created!");
    }

    /**
     * For initialization
     */
    public EvolutionaryBotBrain(String name) {
        super(name);
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
    public class GeneticTree {

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
        public final int OPERATOR_TYPE = 0;
        public final int INPUT_TYPE = 1;


        private TreeNode root;
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        EvolutionaryBotBrain br;
        Random random = new Random();
        //int maxDepth = 5;
        ArrayList<GeneticTree.TreeInput> inputs = new ArrayList<>();
        ArrayList<String> operations = new ArrayList<>();



        /**
         * These are the objects that actually make up the tree.
         * <p>
         * Each node has a type, either OPERATOR_TYPE or INPUT_TYPE
         * INPUT_TYPE nodes are guaranteed to be a leaf
         * OPERATOR_TYPE nodes are guaranteed to not be a leaf and have children left and right that are both TreeNodes
         * The children of an Operator type node can be of either type
         */
        private class TreeNode {

            @Expose()
            private int type;

            @Expose()
            private int nodeDepth;

            @Expose()
            private TreeNode left = null;

            @Expose()
            private TreeNode right = null;

            @Expose()
            private TreeInput value = null;

            @Expose()
            private String operator = null;

            @Expose(serialize = false)
            GeneticTree tr;

            @Expose(serialize = false)
            public int branchDepth;


            /**
             * Constructor for INPUT_TYPE TreeNode
             */
            private TreeNode(TreeInput val, int depth, GeneticTree tr) {
                type = INPUT_TYPE;
                value = val;
                nodeDepth = depth;
                this.tr = tr;
            }

            /**
             * Constructor for OPERATOR_TYPE TreeNode
             */
            private TreeNode(String op, TreeNode l, TreeNode r, int depth, GeneticTree tr) {
                type = OPERATOR_TYPE;
                nodeDepth = depth;
                operator = op;
                left = l;
                right = r;
                this.tr = tr;
            }

            /**
             * Calculates the value returned when evaluating a tree starting at this node
             */
            private int calculateTree(SOCPlayerTracker pt) {
                if (type == INPUT_TYPE) {
                    return value.getInputVal(pt);
                }

                switch (operator) {
                    case ADD:
                        return left.calculateTree(pt) + right.calculateTree(pt);
                    case MINUS:
                        return left.calculateTree(pt) - right.calculateTree(pt);
                    case MULTIPLY:
                        return left.calculateTree(pt) * right.calculateTree(pt);
                    case DIVIDE:
                        int divisor = right.calculateTree(pt);
                        if (divisor == 0) {
                            return 0;
                        } else {
                            return Math.round((float) left.calculateTree(pt) / divisor);
                        }
                    case GREATER:
                        if (left.calculateTree(pt) > right.calculateTree(pt)) {
                            return 1;
                        } else {
                            return 0;
                        }
                    case LESS:
                        if (left.calculateTree(pt) < right.calculateTree(pt)) {
                            return 1;
                        } else {
                            return 0;
                        }
                }
                return -1;
            }

            /**
             * Mutates a particular Node.
             * <p>
             * operatorProbability is the probability that we mutate the node to be an operator. We can set this to
             * -1 to just randomly choose among all possible nodes. Doing this, however, favors choosing inputs because
             * there are so many more inputs than operators.
             */
            private void mutate(int operatorProbability) {
                try {
                    if (operatorProbability == -1) {
                        operatorProbability = Math.round(((float) operations.size() / (operations.size() + inputs.size())) * 100);
                    }
                    int probability = tr.random.nextInt(100);

                    if (operatorProbability > probability && nodeDepth < MAX_DEPTH) { // operator mutation
                        operator = tr.getRandomOperation();
                        type = OPERATOR_TYPE;
                        value = null;
                        left = make_random_child(operatorProbability, nodeDepth + 1);
                        right = make_random_child(operatorProbability, nodeDepth + 1);
                    } else { //input mutation
                        operator = null;
                        left = null;
                        right = null;
                        type = INPUT_TYPE;
                        value = tr.getRandomInput();
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }


            private TreeNode make_random_child(int operatorProbability, int newDepth) {
                TreeNode newChild;
                if (operatorProbability == -1) {
                    operatorProbability = Math.round(((float) operations.size() / (operations.size() + inputs.size())) * 100);
                }
                int probability = tr.random.nextInt(100);

                if (operatorProbability > probability && newDepth < MAX_DEPTH) { // new operator
                    TreeNode child_left = make_random_child(operatorProbability, newDepth + 1);
                    TreeNode child_right = make_random_child(operatorProbability, newDepth + 1);
                    newChild = new TreeNode(tr.getRandomOperation(), child_left, child_right, newDepth, tr);
                } else { // new input
                    newChild = new TreeNode(tr.getRandomInput(), newDepth, tr);
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
            @Expose()
            private String inputName;

            private TreeInput(String name) {
                inputName = name;
            }

            /**
             * Returns the up to date input value that corresponds with the particular input's name
             */
            private int getInputVal(SOCPlayerTracker pt) {
                switch (inputName) {
                    case TIME_TO_LONGEST_ROAD:
                        return pt.longestRoadETA;
                    case ROADS_TO_GO:
                        return pt.roadsToGo;
                    case KNIGHTS_TO_GO:
                        return pt.knightsToBuy;
                    case LARGEST_ARMY_ETA:
                        return pt.largestArmyETA;
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
         *
         * Only used by the main function to initialize trees and write them to a file.
         * These trees should never be used directly
         */
        public GeneticTree(EvolutionaryBotBrain br) {
            this.br = br;
            setUpInput();
            setUpOperations();
            root = new TreeNode(getRandomInput(), 1, this);
            root.mutate(90);
            treeToFile();
        }

        /**
         * reads in a genetic tree from a file
         */
        public GeneticTree(String inputFile, EvolutionaryBotBrain br) {
            this.br = br;
            setUpInput();
            setUpOperations();
            try {
                File treeFile = new File(inputFile);
                BufferedReader bur = new BufferedReader(new FileReader(treeFile));
                String rootJson = bur.readLine();
                root = gson.fromJson(rootJson, TreeNode.class);
                root.tr = this;
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        public int calculateWinEta(SOCPlayerTracker pt) {
            return root.calculateTree(pt);
        }

        /**
         * mutates a random node in the tree
         */
        private void mutate(int operatorProbability) {
            TreeNode node_to_mutate = getRandomNode();
            node_to_mutate.tr = root.tr;
            node_to_mutate.mutate(operatorProbability);
        }

        /**
         * Get a random node from the tree
         */
        private TreeNode getRandomNode() {
            ArrayList<TreeNode> allNodes = new ArrayList<>();
            getAllNodesInTree(root, allNodes, false);
            int size = allNodes.size();
            int index = random.nextInt(size);
            return allNodes.get(index);
        }

//        /**
//         * Get a operator node from tree
//         *
//         * Returns the root if there are no operator nodes in the tree
//         */
//        private TreeNode getRandomOperatorNode(ArrayList<TreeNode> allOperators) {
//            int size = allOperators.size();
//
//            if (size == 0) {
//                return root;
//            }
//
//            int index = random.nextInt(size);
//            return allOperators.get(index);
//        }


        /**
         * updates allNodes to contain the passed in TreeNode node and all of its children.
         */
        private void getAllNodesInTree(TreeNode node, ArrayList<TreeNode> allNodes, Boolean operators_only) {
            if (node.type == OPERATOR_TYPE || !operators_only) {
                allNodes.add(node);
            }

            if (node.type == OPERATOR_TYPE) {
                getAllNodesInTree(node.left, allNodes, operators_only);
                getAllNodesInTree(node.right, allNodes, operators_only);
            }
        }

        /**
         * prints the tree to a file
         */
        private void treeToFile() {
            String rootJson = gson.toJson(root);
            String fileName = ourPlayerName + ".txt";
            try {
                FileWriter fileWriter = new FileWriter(fileName);
                fileWriter.write(rootJson);
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * prints the tree to a file with custom file name
         */
        private void treeToFile(String file_name) {
            String rootJson = gson.toJson(root);
            String fileName = file_name + ".txt";
            try {
                FileWriter fileWriter = new FileWriter(fileName);
                fileWriter.write(rootJson);
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        public String getRandomOperation() {
            int size = operations.size();
            int index = random.nextInt(size);
            return operations.get(index);
        }

    }

    /**
     * Repeat method of getAllNodesInTree but is callable from a static context
     *
     * Also keeps track of branch depth
     */
    public static ArrayList<GeneticTree.TreeNode> getAllOperatorsInTree(GeneticTree.TreeNode n) {
        ArrayList<GeneticTree.TreeNode> a = new ArrayList<>();
        n.branchDepth = getAllOperatorsInTree(n, a);
        return a;
    }

    /**
     * Helper method for the method above
     */
    private static int getAllOperatorsInTree(GeneticTree.TreeNode n, ArrayList<GeneticTree.TreeNode> a) {
        if (n.type == OPERATOR_TYPE) {
            a.add(n);
            int d1 = getAllOperatorsInTree(n.left, a);
            int d2 = getAllOperatorsInTree(n.right, a);
            n.branchDepth = Math.max(d1, d2) + 1;
        } else {
            n.branchDepth = 1;
        }
        return n.branchDepth;
    }

    /**
     * Gets a random operator node for cross over from lst.
     * copy of an existing method but in a static context
     *
     * r is just the root of a tree thats returned if the list is empty. should probably do this in a smarter way.
     *
     * WILL RETURN THE ROOT IF PASSED AN EMPTY LIST
     */
    public static GeneticTree.TreeNode getRandomOperator(Random random, ArrayList<GeneticTree.TreeNode> lst, GeneticTree.TreeNode r) {
        int size = lst.size();

        if (size == 0) {
            return r;
        }

        int index = random.nextInt(size);
        return lst.get(index);
    }

    /**
     * Recalculates the node depths of all nodes in a tree given a root node
     */
    public static void recalculateDepths(GeneticTree.TreeNode n, int depth) {
        n.nodeDepth = depth;

        if (n.type == OPERATOR_TYPE) {
            recalculateDepths(n.left, depth + 1);
            recalculateDepths(n.right, depth + 1);
        }
    }

    /**
     * Performs cross over on two genetic Trees by choosing a random child of a random operator.
     *
     * If one tree is just a root with no other nodes, then no cross_over occurs
     *
     * Tries to do a valid crossover 1000 times before giving up
     *
     */
    public static void cross_over(GeneticTree t1, GeneticTree t2) {
        Random random = new Random();
        boolean success = false;

        // TODO randomly choose t1 or t2 to start
        // TODO get list of valid nodes
        ArrayList<GeneticTree.TreeNode> allOperators_1 = getAllOperatorsInTree(t1.root);
        ArrayList<GeneticTree.TreeNode> allOperators_2 = getAllOperatorsInTree(t2.root);

        GeneticTree.TreeNode n1 = null;
        GeneticTree.TreeNode n2 = null;
        int left_or_right_1 = -1;
        int left_or_right_2 = -1;

        for (int i = 0; i < 1000; i++) {
            n1 = getRandomOperator(random, allOperators_1, t1.root);
            n2 = getRandomOperator(random, allOperators_2, t2.root);

            // 0 means left, 1 means right
            left_or_right_1 = random.nextInt(2);
            left_or_right_2 = random.nextInt(2);

            // See if the cross over is valid
            if (left_or_right_1 == 0) {
                if (left_or_right_2 == 0) {
                    if ((n1.branchDepth + n2.left.branchDepth) <= MAX_DEPTH && (n2.branchDepth + n1.left.branchDepth) <= MAX_DEPTH) {
                        //System.out.println("Took " + i + " Attempts to perform crossover");
                        success = true;
                        break;
                    }
                } else {
                    if ((n1.branchDepth + n2.right.branchDepth) <= MAX_DEPTH && (n2.branchDepth + n1.left.branchDepth) <= MAX_DEPTH) {
                        //System.out.println("Took " + i + " Attempts to perform crossover");
                        success = true;
                        break;
                    }
                }
            } else {
                if (left_or_right_2 == 0) {
                    if ((n1.branchDepth + n2.left.branchDepth) <= MAX_DEPTH && (n2.branchDepth + n1.right.branchDepth) <= MAX_DEPTH) {
                        //System.out.println("Took " + i + " Attempts to perform crossover");
                        success = true;
                        break;
                    }
                } else {
                    if ((n1.branchDepth + n2.right.branchDepth) <= MAX_DEPTH && (n2.branchDepth + n1.right.branchDepth) <= MAX_DEPTH) {
                        //System.out.println("Took " + i + " Attempts to perform crossover");
                        success = true;
                        break;
                    }
                }
            }
        }

        if (left_or_right_1 == -1) {
            throw new RuntimeException("Something went bad");
        }

        // we couldnt make a valid crossover (shouldn't really happen)
        if (!success) {
            System.out.println("Failed to find valid crossover");
            return;
        }

        // One of the trees is just the root
        if (n1.type == t1.INPUT_TYPE || n2.type == t2.INPUT_TYPE) {
            return;
        }

        // Do the cross over
        GeneticTree.TreeNode temp;
        if (left_or_right_1 == 0) {
            if (left_or_right_2 == 0) {
                temp = n2.left;
                n2.left = n1.left;
                n1.left = temp;
            } else {
                temp = n2.right;
                n2.right = n1.left;
                n1.left = temp;
            }
        } else {
            if (left_or_right_2 == 0) {
                temp = n2.left;
                n2.left = n1.right;
                n1.right = temp;
            } else {
                temp = n2.right;
                n2.right = n1.right;
                n1.right = temp;
            }
        }

        // Recalculate Node depths.
        recalculateDepths(t1.root, 1);
        recalculateDepths(t2.root, 1);
    }

    @Override
    protected SOCRobotDM createDM() {
        return new EvolutionaryBotDM(this);
    }

    @Override
    public void setOurPlayerData() {
        ourPlayerData = game.getPlayer(client.getNickname());
        ourPlayerTracker = new EvolutionaryPlayerTracker(ourPlayerData, this);
        ourPlayerNumber = ourPlayerData.getPlayerNumber();
        playerTrackers = new EvolutionaryPlayerTracker[game.maxPlayers];
        playerTrackers[ourPlayerNumber] = ourPlayerTracker;

        File temp = new File(ourPlayerName + ".txt");
        if (temp.exists()) {
            winGameAlgorithm = new GeneticTree(ourPlayerName + ".txt", this);
        } else {
            throw new IllegalArgumentException();
        }

        for (int pn = 0; pn < game.maxPlayers; pn++) {
            if ((pn != ourPlayerNumber) && !game.isSeatVacant(pn)) {
                SOCPlayerTracker tracker = new EvolutionaryPlayerTracker(game.getPlayer(pn), this);
                playerTrackers[pn] = tracker;
            }
        }

        setStrategyFields();

        dummyCancelPlayerData = new SOCPlayer(-2, game);

        // Verify expected face (fast or smart robot)
        int faceId;
        switch (getRobotParameters().getStrategyType()) {
            case SOCRobotDM.SMART_STRATEGY:
                faceId = -1;  // smarter robot face
                break;

            default:
                faceId = 0;   // default robot face
        }
        if (ourPlayerData.getFaceId() != faceId) {
            ourPlayerData.setFaceId(faceId);
            // robotclient will handle sending it to server
        }
    }

    @Override
    public void addPlayerTracker(int pn) {
        if (null == playerTrackers) {
            // SITDOWN hasn't been sent for our own player yet.
            // When it is, playerTrackers will be initialized for
            // each non-vacant player, including pn.

            return;
        }

        if (null == playerTrackers[pn])
            playerTrackers[pn] = new EvolutionaryPlayerTracker(game.getPlayer(pn), this);
    }

    @Override
    protected void setStrategyFields() {
        robotParameters.setStrategyType(0);
        super.setStrategyFields();
    }

    public static void main(String[] args) {
        String bot_name;
        if (args.length == 1){
            bot_name = args[0];
            EvolutionaryBotBrain b = new EvolutionaryBotBrain(bot_name);
            b.new GeneticTree(b);
        } else if (args.length == 3) {
            bot_name = args[0];
            String new_bot_name = args[1];
            int operatorProbability = Integer.parseInt(args[2]);
            EvolutionaryBotBrain b = new EvolutionaryBotBrain(bot_name);
            GeneticTree t = b.new GeneticTree(bot_name + ".txt", b);
            t.mutate(operatorProbability);
            t.treeToFile(new_bot_name);
        } else if (args.length == 4) {
            String b1_name = args[0];
            String b2_name = args[1];
            String new_bot1_name = args[2];
            String new_bot2_name = args[3];
            EvolutionaryBotBrain b1 = new EvolutionaryBotBrain(b1_name);
            EvolutionaryBotBrain b2 = new EvolutionaryBotBrain(b2_name);
            GeneticTree t1 = b1.new GeneticTree(b1_name + ".txt", b1);
            GeneticTree t2 = b2.new GeneticTree(b2_name + ".txt", b1);
            EvolutionaryBotBrain.cross_over(t1, t2);
            t1.treeToFile(new_bot1_name);
            t2.treeToFile(new_bot2_name);
        }
        else {
            throw new IllegalArgumentException();
        }
    }
}
