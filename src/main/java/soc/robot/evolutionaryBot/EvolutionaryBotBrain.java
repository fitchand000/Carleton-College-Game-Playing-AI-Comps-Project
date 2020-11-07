package soc.robot.evolutionaryBot;

import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCPlayer;
import soc.message.SOCMessage;
import soc.robot.SOCPlayerTracker;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.robot.SOCRobotDM;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;

public class EvolutionaryBotBrain extends SOCRobotBrain {

    public EvolutionaryBotBrain(SOCRobotClient rc, SOCRobotParameters params, SOCGame ga, CappedQueue<SOCMessage> mq) {
        super(rc, params, ga, mq);

        System.out.println("An Evolutionary bot has been created!");
    }


    @Override
    public void setOurPlayerData()
    {
        ourPlayerData = game.getPlayer(client.getNickname());
        ourPlayerTracker = new EvolutionaryPlayerTracker(ourPlayerData, this);
        ourPlayerNumber = ourPlayerData.getPlayerNumber();
        playerTrackers = new EvolutionaryPlayerTracker[game.maxPlayers];
        playerTrackers[ourPlayerNumber] = ourPlayerTracker;

        for (int pn = 0; pn < game.maxPlayers; pn++)
        {
            if ((pn != ourPlayerNumber) && ! game.isSeatVacant(pn))
            {
                SOCPlayerTracker tracker = new EvolutionaryPlayerTracker(game.getPlayer(pn), this);
                playerTrackers[pn] = tracker;
            }
        }

        setStrategyFields();

        dummyCancelPlayerData = new SOCPlayer(-2, game);

        // Verify expected face (fast or smart robot)
        int faceId;
        switch (getRobotParameters().getStrategyType())
        {
            case SOCRobotDM.SMART_STRATEGY:
                faceId = -1;  // smarter robot face
                break;

            default:
                faceId = 0;   // default robot face
        }
        if (ourPlayerData.getFaceId() != faceId)
        {
            ourPlayerData.setFaceId(faceId);
            // robotclient will handle sending it to server
        }
    }

    @Override
    public void addPlayerTracker(int pn) {
        if (null == playerTrackers)
        {
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
}
