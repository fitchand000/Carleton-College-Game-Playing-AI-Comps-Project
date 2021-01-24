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
            public int branchDepth = -1;


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

            public String toString() {
                if (type == OPERATOR_TYPE) {
                    return String.format("depth: %d, branch depth: %d, operator: %s", nodeDepth, branchDepth, operator);
                } else {
                    return String.format("depth: %d, branch depth: %d, value: %s", nodeDepth, branchDepth, value.inputName);
                }
            }

            /**
             * Calculates the value returned when evaluating a tree starting at this node
             */
            private int calculateTree(EvolutionaryPlayerTracker pt) {
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
            private int getInputVal(EvolutionaryPlayerTracker pt) {
                switch (inputName) {
                    case TIME_TO_LONGEST_ROAD:
                        return pt.longestRoadETA;
                    case ROADS_TO_GO:
                        return pt.roadsToGo;
                    case KNIGHTS_TO_GO:
                        return pt.knightsToBuy;
                    case LARGEST_ARMY_ETA:
                        return pt.largestArmyETA;

                    // e.g.: income for wheat is the sum of all the wheat gained from all possible die combinations. Takes robber into account, not sure if we want that
                    case TOTAL_RESOURCE_INCOME:
                        return pt.getResourceIncome("total");
                    case WHEAT_INCOME:
                        return pt.getResourceIncome("wheat");
                    case SHEEP_INCOME:
                        return pt.getResourceIncome("sheep");
                    case ORE_INCOME:
                        return pt.getResourceIncome("ore");
                    case BRICK_INCOME:
                        return pt.getResourceIncome("brick");
                    case LOG_INCOME:
                        return pt.getResourceIncome("log");

                    case CURRENT_WHEAT:
                        return pt.getResourceCount("wheat");
                    case CURRENT_SHEEP:
                        return pt.getResourceCount("sheep");
                    case CURRENT_ORE:
                        return pt.getResourceCount("ore");
                    case CURRENT_LOG:
                        return pt.getResourceCount("log");
                    case CURRENT_BRICK:
                        return pt.getResourceCount("brick");
                    case TOTAL_RESOURCES:
                        return pt.getTotalResources();
                    case CURRENT_VP:
                        return pt.getCurrentVP();
                    case PORT_COUNT:
                        return pt.getPortCount();
                    case DEV_CARD_COUNT:
                        return pt.getDevCardCount();
                    case BUILD_LOCATION_COUNT: // How many legal places are there to build
                        return pt.getBuildLocationCount();
                    case READY_BUILD_SPOT_COUNT: // How many spots can you build right now, without placing new roads
                        return pt.getReadyBuildSpotCount();

                    // For all of the ETA cases we are using bse.getEstimatesFromNowFast. There is also a function
                    // bse.getEstiamtesFromNowAccurate that we could use but this seems much slower
                    case SETTLEMENT_ETA:
                        return pt.getBuildETA("settlement");
                    case CITY_ETA:
                        return pt.getBuildETA("city");
                    case DEV_CARD_ETA:
                        return pt.getBuildETA("dev card");
                    case ROAD_ETA:
                        return pt.getBuildETA("road");
                }
                throw new RuntimeException("Tried to get an invalid input value");
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
        public int calculateWinEta(EvolutionaryPlayerTracker pt) {

            //bat experimenting
//            try {
//                FileWriter myWriter = new FileWriter("fileForEachGame.csv");
//                myWriter.write("Game Number" + "Turn_number" + "Player" + "winETA" + " \n");
//                myWriter.close();
//                System.out.println("Succesfully wrote to the file");
//            } catch (IOException e){
//                System.out.println("An error occurred.");
//                e.printStackTrace();
//            }
//
//            System.out.println("Bat: some thing is happening");
            // bat's experiment finishes here
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

        // Everything below here in the Genetic tree class is used for the cross over function.


        public class NodeWithParent {
            private GeneticTree.TreeNode n;
            private GeneticTree.TreeNode p;
            private boolean is_right_child;

            public NodeWithParent(GeneticTree.TreeNode n, GeneticTree.TreeNode p, boolean is_right_child) {
                this.n = n;
                this.p = p;
                this.is_right_child = is_right_child;
            }
        }

        /**
         * Gets a list of all nodes in a tree such that:
         *      - nd + n.branchDepth < MAX_DEPTH AND
         *      - n.nodeDepth + bd < MAX_DEPTH
         *
         * nd = the node depth of the first node we want to cross over with
         * bd = the branch depth of the first node we want to cross over with
         * (set both of these to 0 when we want to consider all nodes)
         *
         * calculates branch depth and keeps track of parent in the process
         */
        private ArrayList<NodeWithParent> getAllNodesWithDepth(int nd, int bd) {
            TreeNode n = this.root;
            ArrayList<NodeWithParent> a = new ArrayList<>();
            n.branchDepth = getAllNodesWithDepth(n, a, nd, bd);
            return a;
        }

        /**
         * Helper method for the method above
         */
        private int getAllNodesWithDepth(GeneticTree.TreeNode n, ArrayList<NodeWithParent> a, int nd, int bd) {
            if (n.type == OPERATOR_TYPE) {
                TreeNode l = n.left;
                TreeNode r = n.right;

                int left_branch_depth = getAllNodesWithDepth(n.left, a, nd, bd);
                if (l.branchDepth + nd <= MAX_DEPTH + 1 && l.nodeDepth + bd <= MAX_DEPTH + 1) {
                    a.add(new NodeWithParent(l, n, false));
                }
                int right_branch_depth = getAllNodesWithDepth(n.right, a, nd, bd);
                if (r.branchDepth + nd <= MAX_DEPTH + 1 && r.nodeDepth + bd <= MAX_DEPTH + 1) {
                    a.add(new NodeWithParent(r, n, true));
                }
                n.branchDepth = Math.max(left_branch_depth, right_branch_depth) + 1;
            } else {
                n.branchDepth = 1;
            }
            return n.branchDepth;
        }

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
     * Gets a random NodeWith parent from a list of NodesWithParents
     *
     * Assumes list is not empty
     */
    public static GeneticTree.NodeWithParent getRandomNodeWithParent(Random random, ArrayList<GeneticTree.NodeWithParent> lst) {
        int size = lst.size();
        int index = random.nextInt(size);
        return lst.get(index);
    }


    /**
     * Performs cross over on two genetic Trees by choosing a two random nodes
     */
    public static void cross_over(GeneticTree t1, GeneticTree t2) {
        Random random = new Random();

        // Decide whether to get first random node from t1 or t2
        GeneticTree start_t = t1;
        GeneticTree end_t = t2;
        int t1_or_t2_root = random.nextInt(2);
        if (t1_or_t2_root == 1) {
            start_t = t2;
            end_t = t1;
        }

        // Get random node with its parent from start tree, can't be the root
        ArrayList<GeneticTree.NodeWithParent> allNodesStart = start_t.getAllNodesWithDepth(0, 0);
        if (allNodesStart.size() == 0) {
            System.out.println("OR HERE");
            return; // tree is just the root
        }
        GeneticTree.NodeWithParent startNode = getRandomNodeWithParent(random, allNodesStart);

        // Get second Node for cross over
        // System.out.println("Number of first nodes to choose from: " + allNodesStart.size());
        ArrayList<GeneticTree.NodeWithParent> allNodesEnd = end_t.getAllNodesWithDepth(startNode.n.nodeDepth, startNode.n.branchDepth);
        if (allNodesEnd.size() == 0) {
            return; // tree is just the root
        }
        // System.out.println("Number of second nodes to choose from: " + allNodesEnd.size());
        GeneticTree.NodeWithParent endNode = getRandomNodeWithParent(random, allNodesEnd);

        // Do crossover
//        System.out.println("Before Crossover");
//        System.out.println("Start Parent: " + startNode.p.toString() + " |left child: " + startNode.p.left.toString() + " |right child: " + startNode.p.right.toString() + " |Node selected is right?: " + startNode.is_right_child);
//        System.out.println("End Parent: " + endNode.p.toString() + " |left child: " + endNode.p.left.toString() + " |right child: " + endNode.p.right.toString() + " |Node selected is right?: " + endNode.is_right_child);

        if (startNode.is_right_child) {
            startNode.p.right = endNode.n;
        } else {
            startNode.p.left = endNode.n;
        }

        if (endNode.is_right_child) {
            endNode.p.right = startNode.n;
        } else {
            endNode.p.left = startNode.n;
        }

        // Recalculate Node depths.
        recalculateDepths(t1.root, 1);
        recalculateDepths(t2.root, 1);
//        System.out.println("\nAfter Crossover");
//        System.out.println("Start Parent: " + startNode.p.toString() + " |left child: " + startNode.p.left.toString() + " |right child: " + startNode.p.right.toString() + " |Node selected is right?: " + startNode.is_right_child);
//        System.out.println("End Parent: " + endNode.p.toString() + " |left child: " + endNode.p.left.toString() + " |right child: " + endNode.p.right.toString() + " |Node selected is right?: " + endNode.is_right_child);
//

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