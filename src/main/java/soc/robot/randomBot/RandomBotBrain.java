package soc.robot.randomBot;

import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.message.SOCMessage;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;

/**
 * A random brain that extends the SOCRobotBrain class
 * It seems like we don't need to change a whole lot in this class.
 * <p>
 * Classes we probably want to overwrite:
 * - setOurPlayerData:
 * - setStrategyFields
 */
public class RandomBotBrain extends SOCRobotBrain {

    /**
     * Standard brain constructor; for javadocs see
     * {@link SOCRobotBrain#SOCRobotBrain(SOCRobotClient, SOCRobotParameters, SOCGame, CappedQueue)}.
     */
    public RandomBotBrain(SOCRobotClient rc, SOCRobotParameters params, SOCGame ga, CappedQueue<SOCMessage> mq)
    {
        super(rc, params, ga, mq);

        System.out.println("A Random bot has been created!");
    }

    /**
     * This is probably where we want to handle bot input. We should be able to pass strings in using the K__EXT_BOT
     * option in {@link SOCGameOption} but for some reason I couldn't get this working. Not sure what the best way
     * to read more complicated data in would be. Our best bet might be to have our passed in value point to a file
     * that the bot reads from?
     */
    @Override
    public void setOurPlayerData()
    {
        super.setOurPlayerData();

        // Lets see if we can get this working
        final String botParam = game.getGameOptionStringValue(SOCGameOption.K__EXT_BOT);
        System.out.println("Passed in Var: " + botParam);
    }

    /**
     * Where we will set our bots random strategies.
     */
    @Override
    protected void setStrategyFields()
    {
        super.setStrategyFields();
    }
}
