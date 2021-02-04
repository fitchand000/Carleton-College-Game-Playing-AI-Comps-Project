package soc.robot.evolutionaryBot;

import java.io.FileWriter;
import java.io.IOException;

public class EvolutionaryPrintWinETA {

    public static void appendFile(int winGameETA, EvolutionaryPlayerTracker evolutionaryPlayerTracker) {
        String simulation_name = evolutionaryPlayerTracker.brain.getClient().simulationName;
        String simulation_name_csv = "evo_" + simulation_name + ".csv";
        try (FileWriter fileWriter = new FileWriter(simulation_name_csv, true)){
            String fileContent= evolutionaryPlayerTracker.brain.getGame().name
                    + "," + evolutionaryPlayerTracker.brain.ourPlayerName
                    + "," + evolutionaryPlayerTracker.brain.getGame().roundCount
                    + "," + evolutionaryPlayerTracker.brain.getGame().turnCount
                    + "," + evolutionaryPlayerTracker.playerNumber + "," + winGameETA + " \n";
            fileWriter.write(fileContent);

        } catch (IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void createFile(EvolutionaryPlayerTracker evolutionaryPlayerTracker){
        try {
            String simulation_name = evolutionaryPlayerTracker.brain.getClient().simulationName;
            String simulation_name_csv = "evo_" + simulation_name + ".csv";
            FileWriter myWriter = new FileWriter(simulation_name_csv);
            myWriter.write("GameName," + "BotName,"+ "RoundNumber," + "TurnNumber," + "PlayerTrackerNumber," + "winETA" + " \n");
            myWriter.close();
            System.out.println("Succesfully created the file for evolutionary");

        } catch (IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}