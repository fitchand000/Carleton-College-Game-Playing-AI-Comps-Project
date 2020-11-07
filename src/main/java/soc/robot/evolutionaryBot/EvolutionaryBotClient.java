package soc.robot.evolutionaryBot;

import soc.baseclient.ServerConnectInfo;
import soc.game.SOCGame;
import soc.message.SOCMessage;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.util.CappedQueue;
import soc.util.SOCFeatureSet;
import soc.util.SOCRobotParameters;

public class EvolutionaryBotClient extends SOCRobotClient {

    /**
     * Our class name, for {@link #rbclass}: {@code "soc.robot.sample3p.EvolutionaryBotClient"}
     */
    private static final String RBCLASSNAME_SAMPLE = EvolutionaryBotClient.class.getName();


    /**
     * Constructor for connecting to the specified server. Does not actually connect here:
     * Afterwards, must call {@link SOCRobotClient#init()} to actually initialize, start threads, and connect.
     *
     * @param sci server connect info with {@code robotCookie}; not {@code null}
     * @param nn  nickname for robot
     * @param pw  password for robot
     * @throws IllegalArgumentException if {@code sci == null}
     */
    public EvolutionaryBotClient(final ServerConnectInfo sci, final String nn, final String pw)
            throws IllegalArgumentException {
        super(sci, nn, pw);

        rbclass = RBCLASSNAME_SAMPLE;
    }

    /**
     * Build the set of optional client features this bot supports, to send to the server.
     * <p>
     * I'm pretty sure we are only including base catan the way this is currently implemented.
     * <p>
     * Called from {@link SOCRobotClient#init()}.
     */
    @Override
    protected SOCFeatureSet buildClientFeats() {
        SOCFeatureSet feats = new SOCFeatureSet(false, false);

        return feats;
    }

    @Override
    public SOCRobotBrain createBrain
    (final SOCRobotParameters params, final SOCGame ga, final CappedQueue<SOCMessage> mq) {
        return new EvolutionaryBotBrain(this, params, ga, mq);
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Java Settlers sample robotclient");
            System.err.println("usage: java " + RBCLASSNAME_SAMPLE + " hostname port_number bot_nickname password cookie");

            return;
        }

        EvolutionaryBotClient cli = new EvolutionaryBotClient
                (new ServerConnectInfo(args[0], Integer.parseInt(args[1]), args[4]), args[2], args[3]);
        cli.init();
    }

}
