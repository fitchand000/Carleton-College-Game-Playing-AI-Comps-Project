package soc.robot.evolutionaryBot;

import java.io.FileWriter;
import java.io.IOException;

public class PrintETA {

    public static void appendFile(int winGameETA, EvolutionaryPlayerTracker evolutionaryPlayerTracker) {
        try (FileWriter fileWriter = new FileWriter("simulation1.csv", true)){
            String fileContent= evolutionaryPlayerTracker.brain.getGame().name + "," + evolutionaryPlayerTracker.brain.getGame().roundCount + "," + evolutionaryPlayerTracker.brain.getGame().turnCount + "," + evolutionaryPlayerTracker.playerNumber + "," + winGameETA + " \n";
            fileWriter.write(fileContent);
            System.out.println("Succesfully wrote to the file");
            fileWriter.close();//should I delete this line?
        } catch (IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void createFile(){
        try {
            FileWriter myWriter = new FileWriter("simulation1.csv");
            myWriter.write("GameName," + "RoundNumber," + "TurnNumber," + "PlayerTrackerNumber," + "winETA" + " \n");
            //write the eta after i create the file
            myWriter.close();
            System.out.println("Succesfully created the file");
        } catch (IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}