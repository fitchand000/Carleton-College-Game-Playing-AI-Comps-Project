package soc.robot.evolutionaryBot;

import java.io.FileWriter;
import java.io.IOException;

public class PrintETA {

    public static void appendFile(int winGameETA, EvolutionaryPlayerTracker evolutionaryPlayerTracker) {
        try (FileWriter fileWriter = new FileWriter("simulation1.csv", true)){
            String fileContent= evolutionaryPlayerTracker.brain.getGame().name + "," + evolutionaryPlayerTracker.brain.ourPlayerName + "," + evolutionaryPlayerTracker.brain.getGame().roundCount + "," + evolutionaryPlayerTracker.brain.getGame().turnCount + "," + evolutionaryPlayerTracker.playerNumber + "," + winGameETA + " \n";
            fileWriter.write(fileContent);
            System.out.println("Succesfully wrote to the file");
            fileWriter.close();//should I delete this line?
        } catch (IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void createFile(EvolutionaryPlayerTracker evolutionaryPlayerTracker){
        try {
            String FileName = evolutionaryPlayerTracker.brain.getClient().SimulationName;
            String FileNameFinal = FileName + ".csv";
            System.out.println(FileNameFinal);
            FileWriter myWriter = new FileWriter(FileNameFinal);
            myWriter.write("GameName," + "BotName,"+ "RoundNumber," + "TurnNumber," + "PlayerTrackerNumber," + "winETA" + " \n");
            //write the eta after i create the file
            myWriter.close();
            System.out.println("Succesfully created the file");
        } catch (IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}