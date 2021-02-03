package soc.robot;

import soc.robot.evolutionaryBot.EvolutionaryPlayerTracker;

import java.io.FileWriter;
import java.io.IOException;

public class PrintWinETASmart {
    public static void appendFile(int winGameETA, SOCPlayerTracker playerTracker) {
        String simulation_name = playerTracker.brain.getClient().SimulationName;
        String simulation_name_csv = simulation_name + ".csv";
        try (FileWriter fileWriter = new FileWriter(simulation_name_csv, true)){
            String fileContent= playerTracker.brain.getGame().name + ","
                    + playerTracker.brain.ourPlayerName
                    + "," + playerTracker.brain.getGame().roundCount
                    + "," + playerTracker.brain.getGame().turnCount
                    + "," + playerTracker.playerNumber
                    + "," + winGameETA + " \n";
            fileWriter.write(fileContent);
        } catch (IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void createFile(SOCPlayerTracker playerTracker){
        try {
            String simulation_name = playerTracker.brain.getClient().SimulationName;
            String simulation_name_csv = simulation_name + ".csv";
            FileWriter myWriter = new FileWriter(simulation_name_csv);
            myWriter.write("GameName," + "BotName,"+ "RoundNumber," + "TurnNumber," + "PlayerTrackerNumber," + "winETA" + " \n");
            myWriter.close();
            System.out.println("Succesfully created the file for smart");
        } catch (IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
