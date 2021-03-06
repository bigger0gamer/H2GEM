import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.ArrayList;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

/* 
 * Program:     Halo 2 Gametype Editor & Manager (H2GEM)
 * Author:      ~ ♀ Yuri Bacon ♀ ~
 * Description: A program that makes editing H2X and H2V gametypes easy, as well as converting H2X gametypes for use in H2V, and vice versa.
 * Date:        22/06/2020
 * 
 * Down with toxicity and bigotry, up with wholesomeness and trans rights~!!
 */
class H2GEM
{
  //This variable holds a single character, the character you get when you try to parse the byte value 0x00 as a character, formally know as the null character.
  //Because H2V annoyingly pads its metadata.bin file out to 256 bytes, I have to reference this character to figure out when the string I need actually ends.
  //There is probably a more "proper" and "cleaner" way to do this, but I found this to be the most straight forward to code for, and is effective for what I need it for.
  //Do not remove this character, or try to edit it, even if it appears to be more than one character (or corrupted) in your text editor. It is only one (uncorrupted) character, and removing it will cause this program to have compile time errors.
  final static char NULLCHAR = ' ';
  
  public static void main(String[] args)
  {
    //Creating a Scanner and String input for taking input from the user
    //Also creating boolean quit, which when true, causes the main loop to break and the program to exit, but repeats the main menu otherwise
    Scanner in = new Scanner(System.in);
    String input = "";
    boolean quit = false;
    
    //Creating the File variables for the folders containing H2V and H2X gametypes
    File h2vGametypesFolder = new File("C:" + File.separator + "Users" + File.separator + System.getProperty("user.name") + File.separator + "AppData" + File.separator + "Local" + File.separator + "Microsoft" + File.separator + "Halo 2" + File.separator + "Saved Games");
    File h2xGametypesFolder = new File("." + File.separator + "4d530064");
    
    //If this program isn't running on Windows, and wasn't given any arguments, we promt the user for their H2V directory.
    //If we do get arguments, we use the path provided.
    //This should only apply when running on Linux or Mac.
    if(System.getProperty("os.name").indexOf("Windows") == -1)
    {
      boolean promptForPath = true;
      if(args.length == 2)
      {
        if(args[0].equals("-ap"))
        {
          h2vGametypesFolder = new File(args[1]);
          promptForPath = false;
        }
        if(args[0].equals("-wp"))
        {
          h2vGametypesFolder = new File(args[1] + File.separator + "drive_c" + File.separator + "users" + File.separator + System.getProperty("user.name") + File.separator + "Local Settings" + File.separator + "Application Data" + File.separator + "Microsoft" + File.separator + "Halo 2" + File.separator + "Saved Games");
          promptForPath = false;
        }
      }
      
      if(promptForPath)
      {
        System.out.println("H2GEM has detected that it is not running under Windows.\n" + 
                           "For future reference, you can pass the arguments \"-ap\" for the absolute path to the \"Saved Games\" folder, " + 
                           "or \"-wp\" for just the path to your wine prefix (H2GEM will find your gametypes at \"<-wp>/drive_c/users/<user>/Local Settings/Application Data/Microsoft/Halo 2/Saved Games\".\n" +
                           "If you wish to edit or convert H2V gametypes, please enter the path to your \"Saved Games\" folder now (leave blank for \"./Saved Games\"):");
        input = in.nextLine();
        System.out.println();
        
        if(input.equals(""))
          h2vGametypesFolder = new File("." + File.separator + "Saved Games");
        else
          h2vGametypesFolder = new File(input);
        
        String[] folders = {};
        if(h2vGametypesFolder.isDirectory())
          folders = h2vGametypesFolder.list();
        
        System.out.println("Here is the contents of the specified path. You should see folders with names in the form of SXXXXXXX. If you don't, then you have entered the path incorrectly, and will have to restart this program.");
        for(int i = 0; i < folders.length; i++)
        {
          System.out.println(folders[i]);
        }
        
        System.out.println();
      }
    }
    
    //ArrayLists to hold all the gametype metadata
    ArrayList<H2GT> h2vGametypes = new ArrayList<H2GT>();
    ArrayList<H2GT> h2xGametypes = new ArrayList<H2GT>();
    
    //Testing if the h2v gametype folder exists and populate the gametypes found in the gametype ArrayLists
    if(h2vGametypesFolder.isDirectory())
    {
      System.out.print("Loading H2V gametype metadata...");
      
      h2vGametypes = loadMetadata(h2vGametypesFolder.getPath(), false, true);
      
      System.out.println(" Done. list.txt has been generated.");
    }
    
    //Testing if the h2x gametype folder exists and populated the gametypes found in the gametype ArrayLists
    if(h2xGametypesFolder.isDirectory())
    {
      System.out.print("Loading H2X gametype metadata...");
      
      h2xGametypes = loadMetadata(h2xGametypesFolder.getPath(), true, true);
      
      System.out.println(" Done. list.txt has been generated.");
    }
    
    //If either the H2X or H2V directories don't exist, we just create the folders anyway so we can save things to them if we need to later.
    if(!h2xGametypesFolder.exists())
      h2xGametypesFolder.mkdirs();
    if(!h2vGametypesFolder.exists())
      h2vGametypesFolder.mkdirs();
    
    //One time startup welcome message~!
    System.out.println("\nWelcome to Halo 2 Gametype Editor & Manager~!!");
    
    do
    {
      //Promt user to pick one of the options, and then takes in that input.
      //Prints extra blank line after taking input to put white space between blocks of console output for aesthetics, 'cause Windows CMD is ugly. Well, all terminals are kinda ugly, but Windows especially.
      System.out.print("Please select a option:\n\nQ)uit Program\nC)onvert Gametypes\nE)dit Gametypes\n");
      input = in.nextLine().toLowerCase();
      System.out.println();
      
      //Processing the user input string, and executing the corresponding code blocks. Loops when any are finished
      if(input.equals("q") || input.equals("quit"))
      {
        //Tells user to have fun with more Halo 2, and sets quit to true so the program will exit
        System.out.println("Exiting program. Have fun playing more Halo 2~!!");
        quit = true;
      }else if(input.equals("e") || input.equals("edit"))
      {
        //Creates boolean optionSelected so we can keep looping the user input promt until we have valid input
        boolean optionSelected = false;
        
        do
        {
          //Promts user which gametypes they'd like to edit, H2X or H2V
          System.out.println("Which gametype(s) would you like to edit?\n1) Edit H2X gametype(s)\n2) Edit H2V gametype(s)");
          input = in.nextLine();
          System.out.println();
          
          //If the input is 1 or 2, we set optionSlected to true so the input loop stops, else re-promts for user input
          if(input.equals("1") || input.equals("2"))
            optionSelected = true;
          else
            System.out.println("Sorry, couldn't process what option you have selected.");
        }while(!optionSelected);
        
        //If the gametype to edit is H2x, this is true, otherwise false
        boolean h2x;
        if(input.equals("1"))
          h2x = true;
        else if(input.equals("2"))
          h2x = false;
        else
        {
          System.out.println("Error: Processed option successfully, but failed to run associated code. This should not happen!! Assuming H2X to H2V conversion...");
          h2x = true;
        }
        
        //Create boolean inputIsNumber, which is used both to make sure the user input is a number before we try to parse it as an integer, and that the input number is within the valid range of options.
        //We assume its true, then check if its false. If it passes our check, we don't repeat the input loop.
        boolean inputIsNumber = true;
        
        do
        {
          //set inputIsNumber back to true in case this is the second or on repeat of this loop
          inputIsNumber = true;
          
          //Prompt user for which gametype they'd like to convert, listing each gametype in the ArrayList as its own option, and adding 2 more: 
          //Convert all, which the option number is equal to the ArrayList size, and cancel, which is equal to the ArrayList size + 1.
          System.out.println("Please select which gametype you'd like to edit:");
          if(h2x)
          {
            for(int i = 0; i < h2xGametypes.size(); i++)
              System.out.println(i + ") " + h2xGametypes.get(i).gametypeName + " (" + h2xGametypes.get(i).gametypeGamemode + ")");
            System.out.println(h2xGametypes.size() + ") Cancel");
          }else
          {
            for(int i = 0; i < h2vGametypes.size(); i++)
              System.out.println(i + ") " + h2vGametypes.get(i).gametypeName + " (" + h2vGametypes.get(i).gametypeGamemode + ")");
            System.out.println(h2vGametypes.size() + ") Cancel");
          }
          input = in.nextLine();
          System.out.println();
          
          //we check each character in the string to see if any of them are a number. If any character is found that isn't a number 0-9, we set inputIsNumber to false to make this input loop repeat
          for(int i = 0; i < input.length(); i++)
            if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
              inputIsNumber = false;
          
          //if the string test above failed, we prompt the user to enter a vaild number. If it passed, we check to make sure it is in range. If it isn't we set inputIsNumber to false to make the input loop repeat, and promt them to enter a valid number.
          if(inputIsNumber)
          {
            if(h2x)
            {
              if(!(Integer.parseInt(input) >= 0 && Integer.parseInt(input) <= h2xGametypes.size()))
              {
                System.out.println("Input does not correspond to a gametype listed. Please enter a number listed below.");
                inputIsNumber = false;
              }
            }else
            {
              if(!(Integer.parseInt(input) >= 0 && Integer.parseInt(input) <= h2vGametypes.size()))
              {
                System.out.println("Input does not correspond to a gametype listed. Please enter a number listed below.");
                inputIsNumber = false;
              }
            }
          }else
            System.out.println("Input is not a number. Please enter a numbered option listed for the desired gametype to edit.");
        }while(!inputIsNumber);
        
        //Haha, INTput? Get it? I'll show myself out.
        //Create an int of the input number, now that we have confirmed it to be valid.
        int intPut = Integer.parseInt(input);
        
        if(intPut >= 0 && intPut < h2xGametypes.size() && h2x || intPut >= 0 && intPut < h2vGametypes.size() && !h2x)
        {
          //Hand the gametype off to the gametype editing method, then inform the user we successfully converted the gametype
          if(h2x)
          {
            editGametype(h2xGametypesFolder.getPath() + File.separator + h2xGametypes.get(intPut).gametypeFolder, true);
          }else
          {
            editGametype(h2vGametypesFolder.getPath() + File.separator + h2vGametypes.get(intPut).gametypeFolder, false);
          }
        }
        
        //Recalling the loadMetadata() method so that our ArrayLists reflect the changes we just made, and so does the list.txt file
        System.out.print("Refreshing H2V gametype metadata...");
        h2xGametypes = loadMetadata(h2vGametypesFolder.getPath(), false, true);
        System.out.println(" Done. list.txt has been updated.");
        
        System.out.print("Refreshing H2X gametype metadata...");
        h2xGametypes = loadMetadata(h2xGametypesFolder.getPath(), true, true);
        System.out.println(" Done. list.txt has been updated.");
      }else if(input.equals("c") || input.equals("convert"))
      {
        //Creates boolean optionSelected so we can keep looping the user input promt until we have valid input
        boolean optionSelected = false;
        
        do
        {
          //Promts user which direction they'd like to convert, H2X -> H2V or H2V -> H2X
          System.out.println("Which conversion would you like to make?\n1) Convert H2X gametype(s) for use in H2V\n2) Convert H2V gametype(s) for use in H2X");
          input = in.nextLine();
          System.out.println();
          
          //If the input is 1 or 2, we set optionSlected to true so the input loop stops, else re-promts for user input
          if(input.equals("1") || input.equals("2"))
            optionSelected = true;
          else
            System.out.println("Sorry, couldn't process what option you have selected.");
        }while(!optionSelected);
        
        //If the conversion is H2X -> H2V, this is true, otherwise false
        boolean h2x2h2v;
        if(input.equals("1"))
          h2x2h2v = true;
        else if(input.equals("2"))
          h2x2h2v = false;
        else
        {
          System.out.println("Error: Processed option successfully, but failed to run associated code. This should not happen!! Assuming H2X to H2V conversion...");
          h2x2h2v = true;
        }
        
        //Create boolean inputIsNumber, which is used both to make sure the user input is a number before we try to parse it as an integer, and that the input number is within the valid range of options.
        //We assume its true, then check if its false. If it passes our check, we don't repeat the input loop.
        boolean inputIsNumber = true;
        
        do
        {
          //set inputIsNumber back to true in case this is loop has repeated
          inputIsNumber = true;
          
          //Prompt user for which gametype they'd like to convert, listing each gametype in the ArrayList as its own option, and adding 2 more: 
          //Convert all, which the option number is equal to the ArrayList size, and cancel, which is equal to the ArrayList size + 1.
          System.out.println("Please select which gametype you'd like to convert:");
          if(h2x2h2v)
          {
            for(int i = 0; i < h2xGametypes.size(); i++)
              System.out.println(i + ") " + h2xGametypes.get(i).gametypeName + " (" + h2xGametypes.get(i).gametypeGamemode + ")");
            System.out.println(h2xGametypes.size() + ") Convert all gametypes\n" + (h2xGametypes.size() + 1) + ") Cancel");
          }else
          {
            for(int i = 0; i < h2vGametypes.size(); i++)
              System.out.println(i + ") " + h2vGametypes.get(i).gametypeName + " (" + h2vGametypes.get(i).gametypeGamemode + ")");
            System.out.println(h2vGametypes.size() + ") Convert all gametypes\n" + (h2vGametypes.size() + 1) + ") Cancel");
          }
          input = in.nextLine();
          System.out.println();
          
          //we check each character in the string to see if any of them are a number. If any character is found that isn't a number 0-9, we set inputIsNumber to false to make this input loop repeat
          for(int i = 0; i < input.length(); i++)
            if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
              inputIsNumber = false;
          
          //if the string test above failed, we prompt the user to enter a vaild number. If it passed, we check to make sure it is in range. If it isn't we set inputIsNumber to false to make the input loop repeat, and promt them to enter a valid number.
          if(inputIsNumber)
          {
            if(h2x2h2v)
            {
              if(!(Integer.parseInt(input) >= 0 && Integer.parseInt(input) <= h2xGametypes.size() + 1))
              {
                System.out.println("Input does not correspond to a gametype listed. Please enter a number listed below.");
                inputIsNumber = false;
              }
            }else
            {
              if(!(Integer.parseInt(input) >= 0 && Integer.parseInt(input) <= h2vGametypes.size() + 1))
              {
                System.out.println("Input does not correspond to a gametype listed. Please enter a number listed below.");
                inputIsNumber = false;
              }
            }
          }else
            System.out.println("Input is not a number. Please enter a numbered option listed for the desired gametype to convert to H2V.");
        }while(!inputIsNumber);
        
        //Haha, INTput? Get it? I'll show myself out.
        //Create an int of the input number, now that we have confirmed it to be valid.
        int intPut = Integer.parseInt(input);
        
        //if the input number is equal to the array size, we make a for loop that calls the convertGametype() method once for each gametype.
        //If not, we check if the input number is in the range of the ArrayList. If it is, we call the convertGametype() method once for that gametype
        //The only possible input number that should be possilbe is the metadata ArrayList size + 1, which was cancel, so we do nothing special and let this code block break back to the main menu.
        if(intPut == h2xGametypes.size() && h2x2h2v || intPut == h2vGametypes.size() && !h2x2h2v)
        {
          int forLoopLimitI;
          if(h2x2h2v)
            forLoopLimitI = h2xGametypes.size();
          else
            forLoopLimitI = h2vGametypes.size();
          for(int i = 0; i < forLoopLimitI; i++)
          {
            //We find a valid folder name that isn't already used. For H2V gametypes, the folder name format is SXXXXXXX, where X is a decimal number 0-9
            
            //folderName holds the int version of the folderName we are checking to see if it exists
            //folderNameString is the text version of folderName, and will be the name we use once we check it isn't already used
            //boolean folderNameUsed is used to check if the folder name is used. It defaults to false, but changes to true when we detect its been used
            int folderName = 0;
            String folderNameString = "";
            boolean folderNameUsed = false;
            do
            {
              //resets to false on every loop
              folderNameUsed = false;
              
              //we get a text representation of folderName
              if(h2x2h2v)
                folderNameString = Integer.toString(folderName);
              else
                folderNameString = Integer.toHexString(folderName);
              
              //If we're converting to H2V, we make sure folderNameString is padded out to 7 characters long, then we add "S"
              //otherwise, we pad it out to 12 digits long
              if(h2x2h2v)
              {
                while(folderNameString.length() < 7)
                  folderNameString = "0" + folderNameString;
                folderNameString = "S" + folderNameString;
              }else
              {
                while(folderNameString.length() < 12)
                  folderNameString = "0" + folderNameString;
              }
              
              //we check the name we just generated and see if it exists in the gametypes folder that the converted gametype will go into, and change folderNameUsed to true if we find a match
              //we also check to see if a file or folder of this name exists already, since if we happen to generate the name of a profile folder, it won't be in our metadata Arraylists
              int forLoopLimitR;
              if(h2x2h2v)
                forLoopLimitR = h2vGametypes.size();
              else
                forLoopLimitR = h2xGametypes.size();
              for(int r = 0; r < forLoopLimitR; r++)
              {
                if(h2x2h2v)
                {
                  if(h2vGametypes.get(r).gametypeFolder.equals(folderNameString))
                    folderNameUsed = true;
                  if(!folderNameUsed)
                    if(new File(h2vGametypesFolder.getPath() + File.separator + folderNameString).exists())
                      folderNameUsed = true;
                }else
                {
                  if(h2xGametypes.get(r).gametypeFolder.equals(folderNameString))
                    folderNameUsed = true;
                  if(!folderNameUsed)
                    if(new File(h2xGametypesFolder.getPath() + File.separator + folderNameString).exists())
                      folderNameUsed = true;
                }
              }
              
              //if we found the name was used already, we increment folderName by 1 and try again
              if(folderNameUsed)
                folderName++;
            }while(folderNameUsed); //repeat while we haven't found a name used yet
            
            //We call another method to do the conversion now that we know what we are converting
            if(h2x2h2v)
              convertGametype(h2xGametypesFolder.getPath() + File.separator + h2xGametypes.get(i).gametypeFolder, true, h2vGametypesFolder.getPath() + File.separator + folderNameString);
            else
              convertGametype(h2vGametypesFolder.getPath() + File.separator + h2vGametypes.get(i).gametypeFolder, false, h2xGametypesFolder.getPath() + File.separator + folderNameString);
            
            //Inform the user we successfully converted the gametype
            if(h2x2h2v)
              System.out.println(h2xGametypes.get(i).gametypeName + " has been converted to H2V format and saved in folder " + folderNameString + "~!!\n");
            else
              System.out.println(h2vGametypes.get(i).gametypeName + " has been converted to H2X format and saved in folder " + folderNameString + "~!!\n");
            
            //update our metadata ArrayList before we find another folder name
            h2xGametypes = loadMetadata(h2xGametypesFolder.getPath(), true, false);
            h2vGametypes = loadMetadata(h2vGametypesFolder.getPath(), false, false);
          }
        }else if(intPut >= 0 && intPut < h2xGametypes.size())
        {
          //We find a valid folder name that isn't already used. For H2V gametypes, the folder name format is SXXXXXXX, where X is a decimal number 0-9
          
          //folderName hold the int version of the folderName we are checking to see if it exists
          //folderNameString is the text version of folderName, and will be the name we use once we check it isn't already used
          //boolean folderNameUsed is used to check if the folder name is used. It defaults to false, but changes to true when we detect its been used
          int folderName = 0;
          String folderNameString = "";
          boolean folderNameUsed = false;
          do
          {
            //resets to false on every loop
            folderNameUsed = false;
            
            //we get a text representation of folderName
            if(h2x2h2v)
              folderNameString = Integer.toString(folderName);
            else
              folderNameString = Integer.toHexString(folderName);
            
            //If we're converting to H2V, we make sure folderNameString is padded out to 7 characters long, then we add "S"
            //otherwise, we pad it out to 12 digits long
            if(h2x2h2v)
            {
              while(folderNameString.length() < 7)
                folderNameString = "0" + folderNameString;
              folderNameString = "S" + folderNameString;
            }else
            {
              while(folderNameString.length() < 12)
                folderNameString = "0" + folderNameString;
            }
            
            //we check the name we just generated and see if it exists in the gametypes folder that the converted gametype will go into, and change folderNameUsed to true if we find a match
            //we also check to see if a file or folder of this name exists already, since if we happen to generate the name of a profile folder, it won't be in our metadata Arraylists
            int forLoopLimitR;
            if(h2x2h2v)
              forLoopLimitR = h2vGametypes.size();
            else
              forLoopLimitR = h2xGametypes.size();
            for(int r = 0; r < forLoopLimitR; r++)
            {
              if(h2x2h2v)
              {
                if(h2vGametypes.get(r).gametypeFolder.equals(folderNameString))
                  folderNameUsed = true;
                if(!folderNameUsed)
                  if(new File(h2vGametypesFolder.getPath() + File.separator + folderNameString).exists())
                    folderNameUsed = true;
              }else
              {
                if(h2xGametypes.get(r).gametypeFolder.equals(folderNameString))
                  folderNameUsed = true;
                if(!folderNameUsed)
                  if(new File(h2xGametypesFolder.getPath() + File.separator + folderNameString).exists())
                    folderNameUsed = true;
              }
            }
            
            //if we found the name was used already, we increment folderName by 1 and try again
            if(folderNameUsed)
              folderName++;
          }while(folderNameUsed); //repeat while we haven't found a name used yet
          
          //We call another method to do the conversion now that we know what we are converting
          if(h2x2h2v)
            convertGametype(h2xGametypesFolder.getPath() + File.separator + h2xGametypes.get(intPut).gametypeFolder, true, h2vGametypesFolder.getPath() + File.separator + folderNameString);
          else
            convertGametype(h2vGametypesFolder.getPath() + File.separator + h2vGametypes.get(intPut).gametypeFolder, false, h2xGametypesFolder.getPath() + File.separator + folderNameString);
          
          //Inform the user we successfully converted the gametype
          if(h2x2h2v)
            System.out.println(h2xGametypes.get(intPut).gametypeName + " has been converted to H2V format and saved in folder " + folderNameString + "~!!\n");
          else
            System.out.println(h2vGametypes.get(intPut).gametypeName + " has been converted to H2X format and saved in folder " + folderNameString + "~!!\n");
        }
        
        //Recalling the loadMetadata() method so that our ArrayLists reflect the changes we just made, and so does the list.txt file
        System.out.print("Refreshing H2V gametype metadata...");
        h2vGametypes = loadMetadata(h2vGametypesFolder.getPath(), false, true);
        System.out.println(" Done. list.txt has been updated.");
        
        System.out.print("Refreshing H2X gametype metadata...");
        h2xGametypes = loadMetadata(h2xGametypesFolder.getPath(), true, true);
        System.out.println(" Done. list.txt has been updated.\n");
      }else
        System.out.println("Sorry, couldn't process what option you have selected.");
    }while(!quit);
  }
  
  //String gametypeFolderPath is the path of the gametypes folder we want to load all the metadata from
  //boolean h2x should be true if the folder contains h2x gametypes, so we know to load SaveMeta.xbx instead of savemeta.bin, otherwise it should be false
  //boolean saveListTxt will save all the save data we find if true, otherwise it will skip saving list.txt
  private static ArrayList<H2GT> loadMetadata(String gametypeFolderPath, boolean h2x, boolean saveListTxt)
  {
    //Makes a File object for the gametype folder we are loading metadata for, and an ArrayList we are putting the metadata in to eventually return
    File h2GametypesFolder = new File(gametypeFolderPath);
    ArrayList<H2GT> h2Gametypes = new ArrayList<H2GT>();
    
    //Gets an array of all the files and folders in the gametype folder
    String[] folders = h2GametypesFolder.list();
    
    //Iterating through every entry and test if it is a folder. If it is, we look for a gametype file and metadata file.
    //If we can't find both, we ignore the folder. If we find both, we add it to the gametype array.
    for(int i = 0; i < folders.length; i++)
    {
      //We create a File object for the current file or folder we are dealing with, and check if it is a directory
      File folder = new File(h2GametypesFolder.getPath() + File.separator + folders[i]);
      
      //Variables to be sure this folder is a gametype folder
      boolean hasMetadata = false;
      boolean hasGametype = false;
      
      if(folder.isDirectory())
      {
        //An array with the current file we are checking for.
        String[] files = folder.list();
        
        for(int r = 0; r < files.length; r++)
        {
          if(h2x)
          {
            if(files[r].equals("SaveMeta.xbx"))
              hasMetadata = true;
          }else
          {
            if(files[r].equals("savemeta.bin"))
              hasMetadata = true;
          }
          if(files[r].equals("slayer") || files[r].equals("ctf") || files[r].equals("oddball") || files[r].equals("koth") || files[r].equals("juggernaut") || files[r].equals("territories") || files[r].equals("assault") || files[r].equals("profile"))
            hasGametype = true;
        }
      }
      
      //If we find a valid gametype in this folder, we load and parse the metadata to load what gamemode it is (slayer, koth, etc.) and what the gametype name is
      if(hasMetadata && hasGametype)
      {
        //File object of the metadata we want to load
        File gametypeMetadata;
        
        if(h2x)
          gametypeMetadata = new File(folder.getPath() + File.separator + "SaveMeta.xbx");
        else
          gametypeMetadata = new File(folder.getPath() + File.separator + "savemeta.bin");
        
        //The String to hold the metadata once we are done loading it
        String metadata = "";
        try
        {
          //The String we will temporarily store the metadata in until we are sure its good
          String line = "";
          
          //Creating a FileInputStream, so that we can specificy the characterset when making the InputStreamReader, so the BufferedReader loads the files text correctly.
          FileInputStream is = new FileInputStream(gametypeMetadata);
          InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-16LE"));
          BufferedReader bufferedReader = new BufferedReader(isr);
          
          //Read the whole file into the String
          line = bufferedReader.readLine();
          
          //We check to make sure the String has actual data in it, and if it does we put it in the metadata String
          if(line != null)
            metadata = line;
        }
        catch(IOException e)
        {
          //Generic Error Catching because if the file we loaded failed in any way, the above code checking for the gametype is to blame :P
          e.printStackTrace();
        }
        
        //If this is a H2X folder, Truncating the String to remove the file header and "Name=" String at the start of the file.
        //Thankfully, the file ends in 0x0D, which is a line break, so readLine() ends the String at the end of the useful metadata.
        //else, Truncating the String to remove all of the null characters at the end because thanks H2V!!/s
        if(h2x)
          metadata = metadata.substring(6);
        else
          metadata = metadata.substring(0, metadata.indexOf(NULLCHAR));
        
        //Seperating the metadata into the gamemode and gametype name, and adding it to the metadata ArrayList
        String gamemode = metadata.substring(0, metadata.indexOf(':'));
        String gametypeName = metadata.substring(metadata.indexOf(':') + 2);
        
        //And finally, add the gametype to the Arraylist of gametypes
        h2Gametypes.add(new H2GT(folders[i], gamemode, gametypeName));
      }
    }
    
    //save list.txt if we were told to. Useful for keeping metadata ArrayLists up to date during batch conversions without spamming the hard drive
    if(saveListTxt)
    {
      //Creating String list, and adding the metadata for all the gametypes we found into it so we can save it to list.txt
      String list = "";
      for(int i = 0; i < h2Gametypes.size(); i++)
        list += h2Gametypes.get(i).gametypeFolder + " - " + h2Gametypes.get(i).gametypeGamemode + ": " + h2Gametypes.get(i).gametypeName + "\n";
      
      //saving list.txt here
      try
      {
        PrintWriter print = new PrintWriter(h2GametypesFolder.getPath() + File.separator + "list.txt");
        print.print(list);
        print.close();
      }catch(IOException e)
      {
        e.printStackTrace();
      }
    }
    
    //Remove profiles from the gametype metadata ArrayList, since this program can't edit or convert them
    for(int r = 0; r < h2Gametypes.size(); r++)
    {
      if(h2Gametypes.get(r).gametypeGamemode.equals("Profile"))
      {
        h2Gametypes.remove(r);
        r--;
      }
    }
    
    //sorting by gamemode alphabetically here???
    
    //now that loading metadata and saving it to list.txt is all done, we return the ArrayList to the main method
    return h2Gametypes;
  }
  
  //String gametypeFolderPath should be the folder of the gametype to be edited
  //boolean h2x should be true if the gametype we are editing is h2x, otherwise it should be false
  private static void editGametype(String gametypeFolderPath, boolean h2x)
  {
    //File object for the gametypes metadata
    File gametypeMetadata;
    
    //String for user input
    String input = "";
    Scanner in = new Scanner(System.in);
    
    //Loading the metadata and making our metadataGamemode and metadataGametypeName variable from it
    //initializing the metadata object with the path to the right file
    if(h2x)
      gametypeMetadata = new File(gametypeFolderPath + File.separator + "SaveMeta.xbx");
    else
      gametypeMetadata = new File(gametypeFolderPath + File.separator + "savemeta.bin");
    
    //The String to hold the metadata once we are done loading it
    String metadata = "";
    try
    {
      //The String we will temporarily store the metadata in until we are sure its good
      String line = "";
      
      //Creating a FileInputStream, so that we can specificy the characterset when making the InputStreamReader, so the BufferedReader loads the files text correctly.
      FileInputStream is = new FileInputStream(gametypeMetadata);
      InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-16LE"));
      BufferedReader bufferedReader = new BufferedReader(isr);
      
      //Read the whole file into the String
      line = bufferedReader.readLine();
      
      //We check to make sure the String has actual data in it, and if it does we put it in the metadata String
      if(line != null)
        metadata = line;
    }catch(IOException e)
    {
      //Generic Error Catching because if the file we loaded failed in any way, the above code checking for the gametype is to blame :P
      e.printStackTrace();
    }
    
    //If this is a H2X gametype, Truncating the String to remove the file header and "Name=" String at the start of the file.
    //Thankfully, the file ends in 0x0D, which is a line break, so readLine() ends the String at the end of the useful metadata.
    //else, Truncating the String to remove all of the null characters at the end because thanks H2V!!/s
    if(h2x)
      metadata = metadata.substring(6);
    else
      metadata = metadata.substring(0, metadata.indexOf(NULLCHAR));
    
    //Extracting the gamemode out of the metadata
    String metadataGamemode = metadata.substring(0, metadata.indexOf(':'));
    String metadataGametypeName = metadata.substring(metadata.indexOf(':') + 2);
    
    //Swaps the gamemode name used in the metadata with the name used in the file name, so we know what file name to load and save from later.
    if(metadataGamemode.equals("Slayer"))
      metadataGamemode = "slayer";
    if(metadataGamemode.equals("Capture the Flag"))
      metadataGamemode = "ctf";
    if(metadataGamemode.equals("Oddball"))
      metadataGamemode = "oddball";
    if(metadataGamemode.equals("King of the Hill"))
      metadataGamemode = "koth";
    if(metadataGamemode.equals("Juggernaut"))
      metadataGamemode = "juggernaut";
    if(metadataGamemode.equals("Territories"))
      metadataGamemode = "territories";
    if(metadataGamemode.equals("Assault"))
      metadataGamemode = "assault";
    
    //Loading the gametype as one long String. We do this so we can get the actual gametype name out of the file, and compare it to the metadata. If they don't match, we'll let the user fix the metadata to match up with the gametype file.
    //The String to hold the gametype in (as a String) once we are done loading it
    String gametypeName = "";
    try
    {
      //The String we will temporarily store the gametype in until we are sure its good
      String line = "";
      
      //Creating a FileInputStream, so that we can specificy the characterset when making the InputStreamReader, so the BufferedReader loads the files text correctly.
      FileInputStream is = new FileInputStream(gametypeFolderPath + File.separator + metadataGamemode);
      InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-16LE"));
      BufferedReader bufferedReader = new BufferedReader(isr);
      
      boolean repeat = true;
      while(repeat)
      {
        //Read the whole file into the String
        line = bufferedReader.readLine();
        
        //We check to make sure the String has actual data in it, and if it does we put it in the metadata String
        if(line != null)
          gametypeName += line;
        else
          repeat = false;
      }
    }catch(IOException e)
    {
      //Generic Error Catching because if the file we loaded failed in any way, the above code checking for the gametype is to blame :P
      e.printStackTrace();
    }
    
    //Reduce gametypeName down to just the 16 character long gametype name at the start of the file, then reduce it to the name's actual length
    gametypeName = gametypeName.substring(2, 18);
    gametypeName = gametypeName.substring(0, gametypeName.indexOf(NULLCHAR));
    
    //Now we load the gametype in as ints and convert them to bytes for actually editing the gametypes. In the convertGametype() method, we do this simply because we can't load files in as bytes directly, but we'll actually use both ints and bytes to get everything into variables.
    //We create an int array to load the gametype into
    int[] gametype = new int[304];
    
    //we load in the raw byte values of the gametype file. We stop after 304 bytes because thats the end of the gametype itself, so if its a H2X gametype we don't read in the cryptographic hash, as we don't need it.
    try
    {
      int next = -2;
      File file = new File(gametypeFolderPath + File.separator + metadataGamemode);
      FileInputStream fileStream = new FileInputStream(file);
      for(int i = 0; i < gametype.length; i++)
      {
        next = fileStream.read();
        if(next >= 0)
          gametype[i] = next;
      }
      fileStream.close();
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }
    
    //Now we create an actual byte array to store the gametype in. 
    byte[] gametypeBytes = new byte[304];
    
    //We fill in each byte in the array with the byte value of the corresponding int from the int array
    for(int i = 0; i < gametypeBytes.length; i++)
      gametypeBytes[i] = Integer.valueOf(gametype[i]).byteValue();
    
    //We check to make sure that the file header in the gametype is valid. If it isn't, we check if its an H2V gametype. If it is, we assume its encrypted and ask them to decrypt it first. If it isn't, we just assume its corrupted.
    if(!(gametypeBytes[0] == 0 && gametypeBytes[1] == 0 && gametypeBytes[2] == 0 && gametypeBytes[3] == -1))
    {
      if(!h2x)
        System.err.println("Error! Encrypted H2V gametype detected! This program does not handle decrypting H2V gametypes. Please use this tool to decrypt your gametypes, then try loading them in H2GEM again: https://halo2.online/threads/halo-2-saved-game-profiles-and-variants-transfer.2634/ You can also load it up in Project Cartographer to decrypt it.");
      else
        System.err.println("Error! Corrupted Gametype detected! Please either delete this gametype, or manually verify the file is uncorrupted before converting it again.");
    }
    
    //Now we check to make sure the metadata matches up with the gametype, and allow the user to correct the metadata if they don't (but only if they want to).
    if(!gametypeName.equals(metadataGametypeName))
    {
      System.out.println("Gametype name stored in gametype file doesn't match it's metadata. The metadata says the gametype's name is " + metadataGametypeName + " but the gametype's name is actually " + gametypeName + ". Do you want to correct the mismatch?\nY)es\nN)o changes");
      
      boolean repeat = true;
      while(repeat)
      {
        input = in.nextLine().toLowerCase();
        
        if(input.equals("y") || input.equals("yes"))
        {
          metadataGametypeName = gametypeName;
          System.out.println("Fixed!");
          repeat = false;
        }else if(input.equals("n") || input.equals("no"))
        {
          System.out.println("Okay, I won't correct the mismatch.");
          repeat = false;
        }else
          System.out.println("Sorry, couldn't process what option you have selected.");
      }
      
      input = "";
    }
    
    //We load the actual gamemode from the gametype, so that we can edit it properly
    String gametypeGamemode = "";
    if(gametypeBytes[68] == 2)
      gametypeGamemode = "Slayer";
    if(gametypeBytes[68] == 1)
      gametypeGamemode = "Capture the Flag";
    if(gametypeBytes[68] == 3)
      gametypeGamemode = "Oddball";
    if(gametypeBytes[68] == 4)
      gametypeGamemode = "King of the Hill";
    if(gametypeBytes[68] == 7)
      gametypeGamemode = "Juggernaut";
    if(gametypeBytes[68] == 8)
      gametypeGamemode = "Territories";
    if(gametypeBytes[68] == 9)
      gametypeGamemode = "Assault";
    
    //We'll be needing this for any 2 byte/16 bit signed integer
    ByteBuffer byteBuffer = ByteBuffer.allocate(2);
    
    //All of the gametype settings, listed into well named variables, sorted by the menu they appear in
    
    //Each different gamemode has its own unique options, but each one saves it into the same byte, so we'll have to load 'em into universal variables. 
    //gmOp# = gamemode option #
    boolean gmOp0;
    boolean gmOp1;
    boolean gmOp2;
    boolean gmOp3;
    boolean gmOp4;
    boolean gmOp5;
    boolean gmOp6;
    boolean gmOp7;
    
    //Match Options
    byte numberOfRounds = gametypeBytes[76];
    short score = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(0, gametypeBytes[80]).put(1, gametypeBytes[81]).getShort(0);
    short timeLimit = byteBuffer.put(0, gametypeBytes[84]).put(1, gametypeBytes[85]).getShort(0);
    boolean roundsResetMap;
    boolean resolveTies;
    
    //Player Options
    byte maxPlayers = gametypeBytes[120];
    short lives = (short) gametype[124];
    short respawnTime = (short) gametype[128];
    short suicidePenalty = (short) gametype[132];
    byte shieldType = gametypeBytes[136];
    boolean motionSensor;
    boolean activeCamo;
    boolean extraDamage;
    boolean damageResistence;
    
    //Team Options
    boolean teamPlay;
    byte teamScoring = gametypeBytes[164];
    boolean teamChanging;
    boolean friendlyFire;
    byte teamRespawnModifier = gametypeBytes[168];
    short betrayalPenalty = (short) gametype[172];
    boolean forceEvenTeams;
    
    //Gamemode Options
    
    //King of the Hill
    short kothMovingHillTime = byteBuffer.put(0, gametypeBytes[244]).put(1, gametypeBytes[245]).getShort(0);
    
    //Oddball
    byte oddballBallCount = gametypeBytes[244];
    byte oddballBallDamage = gametypeBytes[246];
    byte oddballMovementSpeed = gametypeBytes[248];
    byte oddballBallIndicator = gametypeBytes[250];
    
    //Juggernaut
    byte juggernautMovementSpeed = gametypeBytes[244];
    
    //Capture the Flag
    short ctfFlagResetTime = byteBuffer.put(0, gametypeBytes[244]).put(1, gametypeBytes[245]).getShort(0);
    short ctfSlowWithFlag = gametypeBytes[248];
    byte ctfFlagType = gametypeBytes[260];
    byte ctfFlagDamage = gametypeBytes[252];
    byte ctfFlagIndicator = gametypeBytes[256];
    
    //Assault
    short assaultBombResetTime = byteBuffer.put(0, gametypeBytes[244]).put(1, gametypeBytes[245]).getShort(0);
    byte assaultSlowWithBomb = gametypeBytes[248];
    byte assaultBombType = gametypeBytes[260];
    byte assaultBombIndicator = gametypeBytes[256];
    byte assaultBombArmTime = gametypeBytes[264];
    byte assaultBombDamage = gametypeBytes[253];
    
    //Territories
    short territoriesContestTime = byteBuffer.put(0, gametypeBytes[242]).put(1, gametypeBytes[243]).getShort(0);
    short territoriesControlTime = byteBuffer.put(0, gametypeBytes[244]).put(1, gametypeBytes[245]).getShort(0);
    byte territoryCount = gametypeBytes[240];
    
    
    //Vehicle Options
    byte vehicleRespawn = gametypeBytes[204];
    byte primaryVehicle = gametypeBytes[205];
    byte secondaryVehicle = gametypeBytes[206];
    byte primaryHeavyVehicle = gametypeBytes[207];
    byte banshee = gametypeBytes[208];
    byte primaryTurret = gametypeBytes[210];
    byte secondaryTurret = gametypeBytes[211];
    
    //Equipment Options
    boolean startingGrenades;
    boolean grenadesOnMap;
    boolean overshieldOnMap;
    boolean activeCamoOnMap;
    byte primaryWeapon = gametypeBytes[214];
    byte secondaryWeapon = gametypeBytes[215];
    byte weaponsOnMap = gametypeBytes[212];
    byte weaponRespawnTime = gametypeBytes[213];
    
    
    //All of the gamemode unique boolean variables are stored multiple vars to the byte, so we'll have to do some pre-requisit work before we can store them in our more descriptively named variables
    String gtBools1 = Integer.toBinaryString(gametype[72]);
    String gtBools2 = Integer.toBinaryString(gametype[73]);
    String gmBools = Integer.toBinaryString(gametype[240]);
    
    while(gtBools1.length() < 8)
      gtBools1 = "0" + gtBools1;
    while(gtBools2.length() < 8)
      gtBools2 = "0" + gtBools2;
    while(gmBools.length() < 8)
      gmBools = "0" + gmBools;
    
    if(gtBools1.charAt(0) == '1')
      friendlyFire = true;
    else
      friendlyFire = false;
    if(gtBools1.charAt(1) == '1')
      teamChanging = true;
    else
      teamChanging = false;
    if(gtBools1.charAt(3) == '1')
      resolveTies = true;
    else
      resolveTies = false;
    if(gtBools1.charAt(4) == '1')
      roundsResetMap = true;
    else
      roundsResetMap = false;
    if(gtBools1.charAt(5) == '1')
      activeCamo = true;
    else
      activeCamo = false;
    if(gtBools1.charAt(6) == '1')
      motionSensor = true;
    else
      motionSensor = false;
    if(gtBools1.charAt(7) == '1')
      teamPlay = true;
    else
      teamPlay = false;
    
    if(gtBools2.charAt(1) == '1')
      forceEvenTeams = true;
    else
      forceEvenTeams = false;
    if(gtBools2.charAt(2) == '1')
      damageResistence = true;
    else
      damageResistence = false;
    if(gtBools2.charAt(3) == '1')
      extraDamage = true;
    else
      extraDamage = false;
    if(gtBools2.charAt(4) == '1')
      startingGrenades = true;
    else
      startingGrenades = false;
    if(gtBools2.charAt(5) == '1')
      grenadesOnMap = true;
    else
      grenadesOnMap = false;
    if(gtBools2.charAt(6) == '1')
      activeCamoOnMap = true;
    else
      activeCamoOnMap = false;
    if(gtBools2.charAt(7) == '1')
      overshieldOnMap = true;
    else
      overshieldOnMap = false;
    
    if(gmBools.charAt(0) == '1')
      gmOp0 = true;
    else
      gmOp0 = false;
    if(gmBools.charAt(1) == '1')
      gmOp1 = true;
    else
      gmOp1 = false;
    if(gmBools.charAt(2) == '1')
      gmOp2 = true;
    else
      gmOp2 = false;
    if(gmBools.charAt(3) == '1')
      gmOp3 = true;
    else
      gmOp3 = false;
    if(gmBools.charAt(4) == '1')
      gmOp4 = true;
    else
      gmOp4 = false;
    if(gmBools.charAt(5) == '1')
      gmOp5 = true;
    else
      gmOp5 = false;
    if(gmBools.charAt(6) == '1')
      gmOp6 = true;
    else
      gmOp6 = false;
    if(gmBools.charAt(7) == '1')
      gmOp7 = true;
    else
      gmOp7 = false;
    
    
    //Now we can start the editing!! We present the user with all option category and take their input.
    //I couldn't be bothered to actually comment all of this like I did for the main method.
    //Its tons of print() statements and nextLine() anyways :P There's tons of it there, but its not much to look at really. 
    boolean run = true;
    boolean save = true;
    while(run)
    {
      input = "";
      System.out.println("\nEditing " + gametypeName + "...\n\n0) Match Options\n1) Player Options\n2) Team Options\n3) " + gametypeGamemode + " Options\n4) Vehicle Options\n5) Equipment Options\nS) Save and Quit\nQ) Quit without Saving");
      input = in.nextLine();
      
      if(input.equals("0"))
      {
        boolean matchRun = true;
        while(matchRun)
        {
          System.out.print("\nEditing " + gametypeName + " - Match Options...\n\n0) Number of Rounds: ");
          if(numberOfRounds == 0)
            System.out.println("1 Round");
          else if(numberOfRounds == 1)
            System.out.println("2 Rounds");
          else if(numberOfRounds == 2)
            System.out.println("4 Rounds");
          else if(numberOfRounds == 3)
            System.out.println("6 Rounds");
          else if(numberOfRounds == 4)
            System.out.println("First to 2");
          else if(numberOfRounds == 5)
            System.out.println("First to 3");
          else if(numberOfRounds == 6)
            System.out.println("First to 4");
          System.out.print("1) Score to Win Round: ");
          if(gametypeGamemode.equals("Oddball") || gametypeGamemode.equals("King of the Hill") || gametypeGamemode.equals("Territories"))
            System.out.println((score / 3600) + " hours, " + ((score - ((score / 3600) * 3600)) / 60) + " minutes, and " + (score - (((score / 3600) * 3600) + (((score - ((score / 3600) * 3600)) / 60) * 60))) + " seconds");
          else if(gametypeGamemode.equals("Slayer"))
            System.out.println(score + " kills");
          else if(gametypeGamemode.equals("Capture the Flag"))
            System.out.println(score + " captures");
          else if(gametypeGamemode.equals("Juggernaut"))
            System.out.println(score + " kills as Juggernaut");
          else if(gametypeGamemode.equals("Assault"))
            System.out.println(score + " bombs planted");
          System.out.println("2) Round Time Limit: " + (timeLimit / 3600) + " hours, " + ((timeLimit - ((timeLimit / 3600) * 3600)) / 60) + " minutes, and " + (timeLimit - (((timeLimit / 3600) * 3600) + (((timeLimit - ((timeLimit / 3600) * 3600)) / 60) * 60))) + " seconds\n3) Rounds Reset Map: " + roundsResetMap + "\n4) Resolve Ties: " + resolveTies + "\nR) Return to Main");
          
          input = "";
          input = in.nextLine();
          
          if(input.equals("0"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) 1 Round\n1) 2 Rounds\n2) 4 Rounds\n3) 6 Rounds\n4) First to 2\n5) First to 3\n6) First to 4");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 6)
                {
                  numberOfRounds = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("1"))
          {
            if(gametypeGamemode.equals("Oddball") || gametypeGamemode.equals("King of the Hill") || gametypeGamemode.equals("Territories"))
            {
              input = "";
              boolean inputValid = true;
              int hours = 0;
              int minutes = 0;
              int seconds = 0;
              
              do
              {
                System.out.println("Please enter how many hours you'd like in the score limit (0-9):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 9)
                  {
                    hours = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of hours.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many minutes you'd like in the score limit (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9)
                    {
                      if(intPut > 6)
                      {
                        System.out.println("Score limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        minutes = intPut;
                    }else
                      minutes = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of minutes.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many seconds you'd like in the score limit (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9 && minutes == 6)
                    {
                      if(intPut > 7)
                      {
                        System.out.println("Score limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        seconds = intPut;
                    }else
                      seconds = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of seconds.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              score = (short) ((hours * 3600) + (minutes * 60) + seconds);
            }else
            {
              input = "";
              boolean inputValid = true;
              
              do
              {
                System.out.println("Please enter the score limit (0 = Unlimited, 1-32767):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 6 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 32767)
                  {
                    score = (short) intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }
          }else if(input.equals("2"))
          {
            input = "";
            boolean inputValid = true;
            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            
            do
            {
              System.out.println("Please enter how many hours you'd like in the time limit (0-9):");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 9)
                {
                  hours = intPut;
                }else
                {
                  System.out.println("Please select a valid amount of hours.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
            
            do
            {
              System.out.println("Please enter how many minutes you'd like in the time limit (0-59):");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 3 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 59)
                {
                  if(hours == 9)
                  {
                    if(intPut > 6)
                    {
                      System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                      inputValid = false;
                    }else
                      minutes = intPut;
                  }else
                    minutes = intPut;
                }else
                {
                  System.out.println("Please select a valid amount of minutes.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
            
            do
            {
              System.out.println("Please enter how many seconds you'd like in the time limit (0-59):");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 3 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 59)
                {
                  if(hours == 9 && minutes == 6)
                  {
                    if(intPut > 7)
                    {
                      System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                      inputValid = false;
                    }else
                      seconds = intPut;
                  }else
                    seconds = intPut;
                }else
                {
                  System.out.println("Please select a valid amount of seconds.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
            
            timeLimit = (short) ((hours * 3600) + (minutes * 60) + seconds);
          }else if(input.equals("3"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Rounds Reset Map? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                roundsResetMap = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                roundsResetMap = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("4"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Resolve Ties? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                resolveTies = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                resolveTies = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("R") || input.equals("r"))
          {
            matchRun = false;
          }else
            System.out.println("Sorry, couldn't process what option you have selected.");
        }
      }else if(input.equals("1"))
      {
        boolean playerRun = true;
        while(playerRun)
        {
          System.out.print("\nEditing " + gametypeName + " - Player Options...\n\n0) Max Active Players: " + maxPlayers + " players\n1) Lives Per Round: " + lives + " lives\n2) Respawn Time: " + respawnTime + " seconds\n3) Suicide Penalty: " + suicidePenalty + " seconds\n4) Shield Type: ");
          if(shieldType == 0)
            System.out.println("Normal Shields");
          else if(shieldType == 1)
            System.out.println("No Shields");
          else if(shieldType == 2)
            System.out.println("Overshields");
          System.out.println("5) Motion Sensor: " + motionSensor + "\n6) Active Camo: " + activeCamo + "\n7) Extra Damage: " + extraDamage + "\n8) Damage Resistence: " + damageResistence + "\nR) Return to Main");
          
          input = "";
          input = in.nextLine();
          
          if(input.equals("0"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Enter the maximum amount of active players (0-16):");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 3 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 16)
                {
                  maxPlayers = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid amount of players.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("1"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Enter the amount of lives per player (0 = Unlimited, 1-255):");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 4 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 255)
                {
                  lives = (short) intPut;
                }else
                {
                  System.out.println("Please select a valid amount of lives.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("2"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Enter the seconds it takes to respawn (0 = Instant, 1-255):");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 4 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 255)
                {
                  respawnTime = (short) intPut;
                }else
                {
                  System.out.println("Please select a valid amount of seconds.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("3"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Enter the extra number of seconds it takes to respawn for commiting suicide (0 = None, 1-255):");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 4 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 255)
                {
                  suicidePenalty = (short) intPut;
                }else
                {
                  System.out.println("Please select a valid amount of seconds.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("4"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Normal Shields\n1) No Shields\n2) Overshields");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 2)
                {
                  shieldType = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("5"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Motion Sensor? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                motionSensor = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                motionSensor = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("6"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Active Camo? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                activeCamo = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                activeCamo = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("7"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Extra Damage? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                extraDamage = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                extraDamage = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("8"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Damage Resistence? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                damageResistence = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                damageResistence = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("R") || input.equals("r"))
          {
            playerRun = false;
          }else
            System.out.println("Sorry, couldn't process what option you have selected.");
        }
      }else if(input.equals("2"))
      {
        boolean teamRun = true;
        while(teamRun)
        {
          System.out.print("\nEditing " + gametypeName + " - Team Options...\n\n0) Team Play: " + teamPlay + "\n1) Team Scoring: ");
          if(teamScoring == 0)
            System.out.println("Sum");
          else if(teamScoring == 1)
            System.out.println("Minimum");
          else if(teamScoring == 2)
            System.out.println("Maximum");
          System.out.print("2) Team Changing: " + teamChanging + "\n3) Friendly Fire: " + friendlyFire + "\n4) Respawn Time Modifier: ");
          if(teamRespawnModifier == 0)
            System.out.println("Inheritance");
          else if(teamRespawnModifier == 1)
            System.out.println("Cycling");
          else if(teamRespawnModifier == 2)
            System.out.println("None");
          System.out.println("5) Betrayal Penalty: " + betrayalPenalty + " seconds\n6) Force Even Teams: " + forceEvenTeams + "\nR) Return to Main");
          
          input = "";
          input = in.nextLine();
          
          if(input.equals("0"))
          {
            if(gametypeGamemode.equals("Capture the Flag") || gametypeGamemode.equals("Juggernaut") || gametypeGamemode.equals("Assault"))
              System.out.println("Sorry, you can't change Team Play for this gamemode.");
            else
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Team Play? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  teamPlay = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  teamPlay = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }
          }else if(input.equals("1"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Sum\n1) Minimum\n2) Maximum");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 2)
                {
                  teamScoring = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("2"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Team Changing? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                teamChanging = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                teamChanging = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("3"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Friendly Fire? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                friendlyFire = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                friendlyFire = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("4"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Inheritance\n1) Cycling\n2) None");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 2)
                {
                  teamRespawnModifier = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("5"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Enter how many seconds to add to respawn for betrayals (0-255):");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 4 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 255)
                {
                  betrayalPenalty = (short) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("6"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Force Even Teams? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                forceEvenTeams = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                forceEvenTeams = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("R") || input.equals("r"))
          {
            teamRun = false;
          }else
            System.out.println("Sorry, couldn't process what option you have selected.");
        }
      }else if(input.equals("3"))
      {
        boolean gtRun = true;
        while(gtRun)
        {
          if(gametypeGamemode.equals("Capture the Flag")) //CTF
          {
            System.out.print("\nEditing " + gametypeName + " - CTF Options...\n\n0) Flag Type: ");
            if(ctfFlagType == 0)
              System.out.println("Flag Per Team");
            else if(ctfFlagType == 1)
              System.out.println("Single Flag");
            else if(ctfFlagType == 2)
              System.out.println("Neutral Flag");
            System.out.print("1) Sudden Death: " + gmOp6 + "\n2) Flag At Home to Score: " + gmOp4 + "\n3) Flag Touch Return: " + gmOp5 + "\n4) Flag Reset Time: " + (ctfFlagResetTime / 3600) + " hours, " + ((ctfFlagResetTime - ((ctfFlagResetTime / 3600) * 3600)) / 60) + " minutes, and " + (ctfFlagResetTime - (((ctfFlagResetTime / 3600) * 3600) + (((ctfFlagResetTime - ((ctfFlagResetTime / 3600) * 3600)) / 60) * 60))) + " seconds\n5) Slow With Flag: ");
            if(ctfSlowWithFlag == 0)
              System.out.println("Slow");
            else if(ctfSlowWithFlag == 1)
              System.out.println("Normal");
            else if(ctfSlowWithFlag == 2)
              System.out.println("Fast");
            System.out.print("6) Flag Hit Damage: ");
            if(ctfFlagDamage == 0)
              System.out.println("Massive");
            else if(ctfFlagDamage == 1)
              System.out.println("Normal");
            System.out.print("7) Flag Damage Resistence: " + gmOp1 + "\n8) Flag Active Camo: " + gmOp0 + "\n9) Flag Vehicle Operation: " + gmOp4 + "\n10) Flag Indicator: ");
            if(ctfFlagIndicator == 0)
              System.out.println("When Uncontrolled");
            else if(ctfFlagIndicator == 1)
              System.out.println("Always On");
            else if(ctfFlagIndicator == 2)
              System.out.println("Away From Home");
            else if(ctfFlagIndicator == 3)
              System.out.println("Off");
            System.out.println("R) Return to Main");
            
            input = "";
            input = in.nextLine();
            
            if(input.equals("0"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Flag Per Team\n1) Single Flag\n2) Neutral Flag");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 2)
                  {
                    ctfFlagType = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("1"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Sudden Death? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp6 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp6 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("2"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Flag At Home to Score? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp4 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp4 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("3"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Flag Touch Return? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp5 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp5 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("4"))
            {
              input = "";
              boolean inputValid = true;
              int hours = 0;
              int minutes = 0;
              int seconds = 0;
              
              do
              {
                System.out.println("Please enter how many hours you'd like in the flag reset time (0-9):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 9)
                  {
                    hours = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of hours.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many minutes you'd like in the flag reset time (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9)
                    {
                      if(intPut > 6)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        minutes = intPut;
                    }else
                      minutes = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of minutes.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many seconds you'd like in the flag reset time (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9 && minutes == 6)
                    {
                      if(intPut > 7)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        seconds = intPut;
                    }else
                      seconds = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of seconds.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              ctfFlagResetTime = (short) ((hours * 3600) + (minutes * 60) + seconds);
            }else if(input.equals("5"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Slow\n1) Normal\n2) Fast");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 2)
                  {
                    ctfSlowWithFlag = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("6"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Massive\n1) Normal");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 1)
                  {
                    ctfFlagDamage = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("7"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Flag Damage Resistence? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp1 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp1 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("8"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Flag Active Camo? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp0 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp0 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("9"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Flag Vehicle Operation? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp4 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp4 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("10"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) When Uncontrolled\n1) Always On\n2) Away From Home\n3) Off");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 3)
                  {
                    ctfFlagIndicator = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("R") || input.equals("r"))
            {
              gtRun = false;
            }else
              System.out.println("Sorry, couldn't process what option you have selected.");
          }else if(gametypeGamemode.equals("Slayer")) //Slayer
          {
            System.out.println("\nEditing " + gametypeName + " - Slayer Options...\n\n0) Bonus Points: " + gmOp7 + "\n1) Suicide Point Loss: " + gmOp6 + "\n2) Death Point Loss: " + gmOp5 + "\nR) Return to Main");
            
            input = "";
            input = in.nextLine();
            
            if(input.equals("0"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Bonus Points? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp7 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp7 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("1"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Suicide Point Loss? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp6 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp6 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("2"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Death Point Loss? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp5 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp5 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("R") || input.equals("r"))
            {
              gtRun = false;
            }else
              System.out.println("Sorry, couldn't process what option you have selected.");
          }else if(gametypeGamemode.equals("Oddball")) //Oddball
          {
            System.out.print("\nEditing " + gametypeName + " - Oddball Options...\n\n0) Ball Count: " + oddballBallCount + " Oddballs\n1) Ball Hit Damage: ");
            if(oddballBallDamage == 0)
              System.out.println("Massive");
            else if(oddballBallDamage == 1)
              System.out.println("Normal");
            System.out.print("2) Speed With Ball: ");
            if(oddballMovementSpeed == 0)
              System.out.println("Slow");
            else if(oddballMovementSpeed == 1)
              System.out.println("Normal");
            else if(oddballMovementSpeed == 2)
              System.out.println("Fast");
            System.out.print("3) Toughness With Ball: " + gmOp5 + "\n4) Active Camo With Ball: " + gmOp6 + "\n5) Vehicle Operation With Ball: " + gmOp7 + "\n6) Ball Indicator: ");
            if(oddballBallIndicator == 0)
              System.out.println("Always On");
            else if(oddballBallIndicator == 1)
              System.out.println("When Dropped");
            else if(oddballBallIndicator == 2)
              System.out.println("Team Control");
            else if(oddballBallIndicator == 3)
              System.out.println("Off");
            System.out.println("R) Return to Main");
            
            input = "";
            input = in.nextLine();
            
            if(input.equals("0"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Enter the amount of oddballs in play (0-3):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 3)
                  {
                    oddballBallCount = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of oddballs.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("1"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Massive\n1) Normal");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 1)
                  {
                    oddballBallDamage = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("2"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Slow\n1) Normal\n2) Fast");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 2)
                  {
                    oddballMovementSpeed = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("3"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Toughness with Ball? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp5 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp5 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("4"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Active Camo with Ball? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp6 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp6 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("5"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Vehicle Operation with Ball? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp7 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp7 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("6"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Always On\n1) When Dropped\n2) Team Control\n3) Off");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 3)
                  {
                    oddballBallIndicator = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("R") || input.equals("r"))
            {
              gtRun = false;
            }else
              System.out.println("Sorry, couldn't process what option you have selected.");
          }else if(gametypeGamemode.equals("King of the Hill")) //KOTH
          {
            System.out.println("\nEditing " + gametypeName + " - King of the Hill Options...\n\n0) Uncontested Hill: " + gmOp7 + "\n1) Moving Hill: " + (kothMovingHillTime / 3600) + " hours, " + ((kothMovingHillTime - ((kothMovingHillTime / 3600) * 3600)) / 60) + " minutes, and " + (kothMovingHillTime - (((kothMovingHillTime / 3600) * 3600) + (((kothMovingHillTime - ((kothMovingHillTime / 3600) * 3600)) / 60) * 60))) + " seconds\n2) Team Time Multiplier: " + gmOp6 + "\n3) Extra Damage: " + gmOp5 + "\n4) Damage Resistence: " + gmOp4 + "\n5) Active Camo: " + gmOp3 + "\nR) Return to Main");
            
            input = "";
            input = in.nextLine();
            
            if(input.equals("0"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Hill must be uncontested to score? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp7 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp7 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("1"))
            {
              input = "";
              boolean inputValid = true;
              int hours = 0;
              int minutes = 0;
              int seconds = 0;
              
              do
              {
                System.out.println("Please enter how many hours would you like for the hill to move (0-9):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 9)
                  {
                    hours = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of hours.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many minutes would you like for the hill to move (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9)
                    {
                      if(intPut > 6)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        minutes = intPut;
                    }else
                      minutes = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of minutes.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many seconds would you like for the hill to move (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9 && minutes == 6)
                    {
                      if(intPut > 7)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        seconds = intPut;
                    }else
                      seconds = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of seconds.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              kothMovingHillTime = (short) ((hours * 3600) + (minutes * 60) + seconds);
            }else if(input.equals("2"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Earn a second per teammate on the hill? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp6 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp6 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("3"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Extra Damage on Hill? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp5 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp5 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("4"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Damage Resistence on Hill? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp4 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp4 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("5"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Active Camo on Hill? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp3 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp3 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("R") || input.equals("r"))
            {
              gtRun = false;
            }else
              System.out.println("Sorry, couldn't process what option you have selected.");
          }else if(gametypeGamemode.equals("Juggernaut")) //Juggernaut
          {
            System.out.print("\nEditing " + gametypeName + " - Juggernaut Options...\n\n0) Betrayal Point Loss: " + gmOp4 + "\n1) Juggernaut Extra Damage: " + gmOp3 + "\n2) Juggernaut Infinite Ammo: " + gmOp2 + "\n3) Juggernaut Overshield: " + gmOp6 + "\n4) Juggernaut Active Camo: " + gmOp5 + "\n5) Juggernaut Motion Sensor: " + gmOp7 + "\n6) Juggernaut Movement: ");
            if(juggernautMovementSpeed == 0)
              System.out.println("Slow");
            else if(juggernautMovementSpeed == 1)
              System.out.println("Normal");
            else if(juggernautMovementSpeed == 2)
              System.out.println("Fast");
            System.out.println("7) Juggernaut Damage Resistence: " + gmOp1 + "\nR) Return to Main");
            
            input = "";
            input = in.nextLine();
            
            if(input.equals("0"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Betrayal Point Loss? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp4 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp4 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("1"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Juggernaut Extra Damage? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp3 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp3 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("2"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Juggernaut Infinite Ammo? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp2 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp2 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("3"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Juggernaut Overshield? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp6 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp6 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("4"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Juggernaut Active Camo? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp5 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp5 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("5"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Juggernaut Motion Sensor? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp7 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp7 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("6"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Slow\n1) Normal\n2) Fast");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 2)
                  {
                    juggernautMovementSpeed = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("7"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Juggernaut Damage Resistence? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp1 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp1 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("R") || input.equals("r"))
            {
              gtRun = false;
            }else
              System.out.println("Sorry, couldn't process what option you have selected.");
          }else if(gametypeGamemode.equals("Territories")) //Territories
          {
            System.out.println("\nEditing " + gametypeName + " - Territories Options...\n\n0) Territory Count: " + territoryCount + " Territories\n1) Territory Contest Time: " + (territoriesContestTime / 3600) + " hours, " + ((territoriesContestTime - ((territoriesContestTime / 3600) * 3600)) / 60) + " minutes, and " + (territoriesContestTime - (((territoriesContestTime / 3600) * 3600) + (((territoriesContestTime - ((territoriesContestTime / 3600) * 3600)) / 60) * 60))) + " seconds\n2) Territory Control Time: " + (territoriesControlTime / 3600) + " hours, " + ((territoriesControlTime - ((territoriesControlTime / 3600) * 3600)) / 60) + " minutes, and " + (territoriesControlTime - (((territoriesControlTime / 3600) * 3600) + (((territoriesControlTime - ((territoriesControlTime / 3600) * 3600)) / 60) * 60))) + " seconds\nR) Return to Main");
            
            input = "";
            input = in.nextLine();
            
            if(input.equals("0"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Enter the number of territories in play (1-8):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 1 && intPut <= 8)
                  {
                    territoryCount = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid number of territories.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("1"))
            {
              input = "";
              boolean inputValid = true;
              int hours = 0;
              int minutes = 0;
              int seconds = 0;
              
              do
              {
                System.out.println("Please enter how many hours would you like to contest the territory (0-9):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 9)
                  {
                    hours = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of hours.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many minutes would you like to contest the territory (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9)
                    {
                      if(intPut > 6)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        minutes = intPut;
                    }else
                      minutes = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of minutes.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many seconds would you like to contest the territory (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9 && minutes == 6)
                    {
                      if(intPut > 7)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        seconds = intPut;
                    }else
                      seconds = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of seconds.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              territoriesContestTime = (short) ((hours * 3600) + (minutes * 60) + seconds);
            }else if(input.equals("2"))
            {
              input = "";
              boolean inputValid = true;
              int hours = 0;
              int minutes = 0;
              int seconds = 0;
              
              do
              {
                System.out.println("Please enter how many hours would you like to control the territory (0-9):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 9)
                  {
                    hours = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of hours.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many minutes would you like to control the territory (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9)
                    {
                      if(intPut > 6)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        minutes = intPut;
                    }else
                      minutes = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of minutes.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many seconds would you like to control the territory (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9 && minutes == 6)
                    {
                      if(intPut > 7)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        seconds = intPut;
                    }else
                      seconds = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of seconds.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              territoriesControlTime = (short) ((hours * 3600) + (minutes * 60) + seconds);
            }else if(input.equals("R") || input.equals("r"))
            {
              gtRun = false;
            }else
              System.out.println("Sorry, couldn't process what option you have selected.");
          }else if(gametypeGamemode.equals("Assault")) //Assault
          {
            System.out.print("\nEditing " + gametypeName + " - Assault Options...\n\n0) Bomb Type: ");
            if(assaultBombType == 0)
              System.out.println("Bomb Per Team");
            else if(assaultBombType == 1)
              System.out.println("Single Bomb");
            else if(assaultBombType == 2)
              System.out.println("Nuetral Bomb");
            System.out.print("1) Enemy Bomb Indicator: ");
            if(assaultBombIndicator == 0)
              System.out.println("Always On");
            else if(assaultBombIndicator == 1)
              System.out.println("When Dropped");
            else if(assaultBombIndicator == 2)
              System.out.println("When Armed");
            else if(assaultBombIndicator == 3)
              System.out.println("Off");
            System.out.print("2) Sudden Death: " + gmOp6 + "\n3) Bomb Touch Return: " + gmOp5 + "\n4) Bomb Reset Time: " + (assaultBombResetTime / 3600) + " hours, " + ((assaultBombResetTime - ((assaultBombResetTime / 3600) * 3600)) / 60) + " minutes, and " + (assaultBombResetTime - (((assaultBombResetTime / 3600) * 3600) + (((assaultBombResetTime - ((assaultBombResetTime / 3600) * 3600)) / 60) * 60))) + " seconds\n5) Bomb Arm Time: " + assaultBombArmTime + " seconds\n6) Sticky Arming: " + gmOp2 + "\n7) Slow With Bomb: ");
            if(assaultSlowWithBomb == 0)
              System.out.println("Slow");
            else if(assaultSlowWithBomb == 1)
              System.out.println("Normal");
            else if(assaultSlowWithBomb == 2)
              System.out.println("Fast");
            System.out.println("8) Bomb Damage Resistence: " + gmOp1 + "\n9) Bomb Active Camo: " + gmOp0 + "\n10) Bomb Vehicle Operation: " + gmOp3 + "\nR) Return to Main");
            
            input = "";
            input = in.nextLine();
            
            if(input.equals("0"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Bomb Per Team\n1) Single Bomb\n2) Neutral Bomb");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 2)
                  {
                    assaultBombType = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("1"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Always On\n1) When Dropped\n2) When Armed\n3) Off");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 3)
                  {
                    assaultBombIndicator = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("2"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Sudden Death? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp6 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp6 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("3"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Bomb Touch Return? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp5 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp5 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("4"))
            {
              input = "";
              boolean inputValid = true;
              int hours = 0;
              int minutes = 0;
              int seconds = 0;
              
              do
              {
                System.out.println("Please enter how many hours would you like for the bomb to respawn (0-9):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 9)
                  {
                    hours = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of hours.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many minutes would you like for the bomb to respawn (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9)
                    {
                      if(intPut > 6)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        minutes = intPut;
                    }else
                      minutes = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of minutes.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              do
              {
                System.out.println("Please enter how many seconds would you like for the bomb to respawn (0-59):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 59)
                  {
                    if(hours == 9 && minutes == 6)
                    {
                      if(intPut > 7)
                      {
                        System.out.println("Time limit cannot exceed 9 hours, 6 minutes, and 7 seconds.");
                        inputValid = false;
                      }else
                        seconds = intPut;
                    }else
                      seconds = intPut;
                  }else
                  {
                    System.out.println("Please select a valid amount of seconds.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
              
              assaultBombResetTime = (short) ((hours * 3600) + (minutes * 60) + seconds);
            }else if(input.equals("5"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Enter the number of seconds to arm the bomb (0-15):");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 3 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 15)
                  {
                    assaultBombArmTime = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid number of seconds.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("6"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Sticky Arming? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp2 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp2 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("7"))
            {
              boolean inputValid = true;
              do
              {
                System.out.println("Select a value:\n0) Slow\n1) Normal\n2) Fast");
                input = in.nextLine();
                
                inputValid = true;
                for(int i = 0; i < input.length(); i++)
                {
                  if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                    inputValid = false;
                }
                
                if(inputValid && input.length() < 2 && input.length() > 0)
                {
                  int intPut = Integer.parseInt(input);
                  if(intPut >= 0 && intPut <= 2)
                  {
                    assaultSlowWithBomb = (byte) intPut;
                  }else
                  {
                    System.out.println("Please select a valid option.");
                    inputValid = false;
                  }
                }else
                {
                  System.out.println("Please input a valid number.");
                  inputValid = false;
                }
              }while(!inputValid);
            }else if(input.equals("8"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Bomb Carrier Damage Resistence? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp1 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp1 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("9"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Bomb Carrier Active Camo? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp0 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp0 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("10"))
            {
              boolean inputValid = true;
              do
              {
                inputValid = true;
                System.out.println("Bomb Carrier Vehicle Operation? (On/Off):");
                input = in.nextLine();
                
                if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                  gmOp3 = true;
                else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                  gmOp3 = false;
                else
                {
                  inputValid = false;
                  System.out.println("Sorry, couldn't process what option you have selected.");
                }
              }while(!inputValid);
            }else if(input.equals("R") || input.equals("r"))
            {
              gtRun = false;
            }else
              System.out.println("Sorry, couldn't process what option you have selected.");
          }
        }
      }else if(input.equals("4"))
      {
        boolean vehicleRun = true;
        while(vehicleRun)
        {
          System.out.print("\nEditing " + gametypeName + " - Vehicle Options...\n\n0) Vehicle Respawn Time: ");
          if(vehicleRespawn == 0)
            System.out.println("Map Default");
          else if(vehicleRespawn == 1)
            System.out.println("No Respawn");
          else if(vehicleRespawn == 3)
            System.out.println("Half Time");
          System.out.print("1) Primary Light Vehicle: ");
          if(primaryVehicle == 0)
            System.out.println("Map Default");
          else if(primaryVehicle == 1)
            System.out.println("Warthog");
          else if(primaryVehicle == 2)
            System.out.println("Guass Warthog");
          else if(primaryVehicle == 3)
            System.out.println("Ghost");
          else if(primaryVehicle == 4)
            System.out.println("None [?]");
          else if(primaryVehicle == 5)
            System.out.println("Spectre");
          else if(primaryVehicle == 6)
            System.out.println("Random");
          else if(primaryVehicle == 7)
            System.out.println("None");
          System.out.print("2) Secondary Light Vehicle: ");
          if(secondaryVehicle == 0)
            System.out.println("Map Default");
          else if(secondaryVehicle == 1)
            System.out.println("Warthog");
          else if(secondaryVehicle == 2)
            System.out.println("Guass Warthog");
          else if(secondaryVehicle == 3)
            System.out.println("Ghost");
          else if(secondaryVehicle == 4)
            System.out.println("None [?]");
          else if(secondaryVehicle == 5)
            System.out.println("Spectre");
          else if(secondaryVehicle == 6)
            System.out.println("Random");
          else if(secondaryVehicle == 7)
            System.out.println("None");
          System.out.print("3) Primary Heavy Vehicle: ");
          if(primaryHeavyVehicle == 0)
            System.out.println("Map Default");
          else if(primaryHeavyVehicle == 1)
            System.out.println("Scorpion Tank");
          else if(primaryHeavyVehicle == 2)
            System.out.println("Wraith");
          else if(primaryHeavyVehicle == 3)
            System.out.println("Random");
          else if(primaryHeavyVehicle == 4)
            System.out.println("None");
          System.out.print("4) Banshee: ");
          if(banshee == 0)
            System.out.println("Map Default");
          else if(banshee == 1)
            System.out.println("On");
          else if(banshee == 2 || banshee == 3)
            System.out.println("On [?]");
          else if(banshee == 4)
            System.out.println("Off");
          System.out.print("5) Primary Turret: ");
          if(primaryTurret == 0)
            System.out.println("Map Default");
          else if(primaryTurret == 1)
            System.out.println("Large Machine Gun");
          else if(primaryTurret == 2 || primaryTurret == 3)
            System.out.println("None [?]");
          else if(primaryTurret == 4)
            System.out.println("Large Plasma");
          else if(primaryTurret == 5)
            System.out.println("Random");
          else if(primaryTurret == 6)
            System.out.println("None");
          System.out.print("6) Secondary Turret: ");
          if(secondaryTurret == 0)
            System.out.println("Map Default");
          else if(secondaryTurret == 1)
            System.out.println("Large Machine Gun");
          else if(secondaryTurret == 2 || secondaryTurret == 3)
            System.out.println("None [?]");
          else if(secondaryTurret == 4)
            System.out.println("Large Plasma");
          else if(secondaryTurret == 5)
            System.out.println("Random");
          else if(secondaryTurret == 6)
            System.out.println("None");
          System.out.println("R) Return to Main");
          
          input = "";
          input = in.nextLine();
          
          if(input.equals("0"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Map Default\n1) No Respawn\n3) Half Time");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut == 0 || intPut == 1 || intPut == 3)
                {
                  vehicleRespawn = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("1"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Map Default\n1) Warthog\n2) Guass Warthog\n3) Ghost\n4) None [?]\n5) Spectre\n6) Random\n7) None");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 7)
                {
                  primaryVehicle = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("2"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Map Default\n1) Warthog\n2) Guass Warthog\n3) Ghost\n4) None [?]\n5) Spectre\n6) Random\n7) None");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 7)
                {
                  secondaryVehicle = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("3"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Map Default\n1) Scorpion Tank\n2) Wraith\n3) Random\n4) None");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 4)
                {
                  primaryHeavyVehicle = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("4"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Map Default\n1) On\n2) On [?]\n3) On [?]\n4) Off");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 4)
                {
                  banshee = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("5"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Map Default\n1) Large Machine Gun\n2) None [?]\n3) None [?]\n4) Large Plasma\n5) Random\n6) None");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 6)
                {
                  primaryTurret = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("6"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Map Default\n1) Large Machine Gun\n2) None [?]\n3) None [?]\n4) Large Plasma\n5) Random\n6) None");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 6)
                {
                  secondaryTurret = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("R") || input.equals("r"))
          {
            vehicleRun = false;
          }else
            System.out.println("Sorry, couldn't process what option you have selected.");
        }
      }else if(input.equals("5"))
      {
        boolean equipRun = true;
        while(equipRun)
        {
          System.out.print("\nEditing " + gametypeName + " - Equipment Options...\n\n0) Primary Weapon: ");
          if(primaryWeapon == 0)
            System.out.println("Map Default");
          else if(primaryWeapon == 1)
            System.out.println("None");
          else if(primaryWeapon == 2)
            System.out.println("Random");
          else if(primaryWeapon == 3)
            System.out.println("Battle Rifle");
          else if(primaryWeapon == 4)
            System.out.println("Magnum");
          else if(primaryWeapon == 5)
            System.out.println("SMG");
          else if(primaryWeapon == 6)
            System.out.println("Plasma Pistol");
          else if(primaryWeapon == 7)
            System.out.println("Plasma Rifle");
          else if(primaryWeapon == 8)
            System.out.println("Rocket Launcher");
          else if(primaryWeapon == 9)
            System.out.println("Shotgun");
          else if(primaryWeapon == 10)
            System.out.println("Sniper Rifle");
          else if(primaryWeapon == 11)
            System.out.println("Brute Shot");
          else if(primaryWeapon == 12)
            System.out.println("Needler");
          else if(primaryWeapon == 13)
            System.out.println("Carbine");
          else if(primaryWeapon == 14)
            System.out.println("Beam Rifle");
          else if(primaryWeapon == 15)
            System.out.println("None [?]");
          else if(primaryWeapon == 16)
            System.out.println("None [?]");
          else if(primaryWeapon == 17)
            System.out.println("Energy Sword");
          else if(primaryWeapon == 18)
            System.out.println("Brute Plasma Rifle");
          else if(primaryWeapon == 19)
            System.out.println("Sentinel Beam");
          System.out.print("1) Secondary Weapon: ");
          if(secondaryWeapon == 0)
            System.out.println("Map Default");
          else if(secondaryWeapon == 1)
            System.out.println("None");
          else if(secondaryWeapon == 2)
            System.out.println("Random");
          else if(secondaryWeapon == 3)
            System.out.println("Battle Rifle");
          else if(secondaryWeapon == 4)
            System.out.println("Magnum");
          else if(secondaryWeapon == 5)
            System.out.println("SMG");
          else if(secondaryWeapon == 6)
            System.out.println("Plasma Pistol");
          else if(secondaryWeapon == 7)
            System.out.println("Plasma Rifle");
          else if(secondaryWeapon == 8)
            System.out.println("Rocket Launcher");
          else if(secondaryWeapon == 9)
            System.out.println("Shotgun");
          else if(secondaryWeapon == 10)
            System.out.println("Sniper Rifle");
          else if(secondaryWeapon == 11)
            System.out.println("Brute Shot");
          else if(secondaryWeapon == 12)
            System.out.println("Needler");
          else if(secondaryWeapon == 13)
            System.out.println("Carbine");
          else if(secondaryWeapon == 14)
            System.out.println("Beam Rifle");
          else if(secondaryWeapon == 15)
            System.out.println("None [?]");
          else if(secondaryWeapon == 16)
            System.out.println("None [?]");
          else if(secondaryWeapon == 17)
            System.out.println("Energy Sword");
          else if(secondaryWeapon == 18)
            System.out.println("Brute Plasma Rifle");
          else if(secondaryWeapon == 19)
            System.out.println("Sentinel Beam");
          System.out.print("2) Starting Grenades: " + startingGrenades + "\n3) Weapons on Map: ");
          if(weaponsOnMap == 0)
            System.out.println("Map Default");
          else if(weaponsOnMap == 1)
            System.out.println("None");
          else if(weaponsOnMap == 2)
            System.out.println("Rockets");
          else if(weaponsOnMap == 3)
            System.out.println("Shotguns");
          else if(weaponsOnMap == 4)
            System.out.println("Swords");
          else if(weaponsOnMap == 5)
            System.out.println("Brute Shots");
          else if(weaponsOnMap == 6)
            System.out.println("Halo Classic");
          else if(weaponsOnMap == 7)
            System.out.println("New Classic");
          else if(weaponsOnMap == 8)
            System.out.println("Heavy Weapons");
          else if(weaponsOnMap == 9)
            System.out.println("All Duals");
          else if(weaponsOnMap == 10)
            System.out.println("No Duals");
          else if(weaponsOnMap == 11)
            System.out.println("Rifles");
          else if(weaponsOnMap == 12)
            System.out.println("Sniping");
          else if(weaponsOnMap == 13)
            System.out.println("No Sniping");
          else if(weaponsOnMap == 14)
            System.out.println("Pistols");
          else if(weaponsOnMap == 15)
            System.out.println("Plasma");
          else if(weaponsOnMap == 16)
            System.out.println("Human");
          else if(weaponsOnMap == 17)
            System.out.println("Covenant");
          else if(weaponsOnMap == 18)
            System.out.println("Sentinel Beams");
          else if(weaponsOnMap == 19)
            System.out.println("Random Set");
          System.out.print("4) Weapon Respawn Time: ");
          if(weaponRespawnTime == 0)
            System.out.println("Map Default");
          else if(weaponRespawnTime == 1)
            System.out.println("No Respawn");
          else if(weaponRespawnTime == 2)
            System.out.println("Half Time");
          else if(weaponRespawnTime == 3)
            System.out.println("Double Time");
          System.out.println("5) Grenades On Map: " + grenadesOnMap + "\n6) Overshields: " + overshieldOnMap + "\n7) Active Camo: " + activeCamoOnMap + "\nR) Return to Main");
          
          input = "";
          input = in.nextLine();
          
          if(input.equals("0"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n 0) Map Default\n 1) None\n 2) Random\n 3) Battle Rifle\n 4) Magnum\n 5) SMG\n 6) Plasma Pistol\n 7) Plasma Rifle\n 8) Rocket Launcher\n 9) Shotgun\n10) Sniper Rifle\n11) Brute Shot\n12) Needler\n13) Carbine\n14) Beam Rifle\n15) None [?]\n16) None [?]\n17) Energy Sword\n18) Brute Plasma Rifle\n19) Sentinel Beam");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 3 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 19)
                {
                  primaryWeapon = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("1"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n 0) Map Default\n 1) None\n 2) Random\n 3) Battle Rifle\n 4) Magnum\n 5) SMG\n 6) Plasma Pistol\n 7) Plasma Rifle\n 8) Rocket Launcher\n 9) Shotgun\n10) Sniper Rifle\n11) Brute Shot\n12) Needler\n13) Carbine\n14) Beam Rifle\n15) None [?]\n16) None [?]\n17) Energy Sword\n18) Brute Plasma Rifle\n19) Sentinel Beam");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 3 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 19)
                {
                  secondaryWeapon = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("2"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Starting Grenades? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                startingGrenades = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                startingGrenades = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("3"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n 0) Map Default\n 1) None\n 2) Rockets\n 3) Shotguns\n 4) Swords\n 5) Brute Shots\n 6) Halo Classic\n 7) New Classic\n 8) Heavy Weapons\n 9) All Duals\n10) No Duals\n11) Rifles\n12) Sniping\n13) No Sniping\n14) Pistols\n15) Plasma\n16) Human\n17) Covenant\n18) Sentinel Beams\n19) Random Set");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 3 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 19)
                {
                  weaponsOnMap = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("4"))
          {
            boolean inputValid = true;
            do
            {
              System.out.println("Select a value:\n0) Map Default\n1) No Respawn\n2) Half Time\n3) Double Time");
              input = in.nextLine();
              
              inputValid = true;
              for(int i = 0; i < input.length(); i++)
              {
                if(!(input.charAt(i) == '1' || input.charAt(i) == '2' || input.charAt(i) == '3' || input.charAt(i) == '4' || input.charAt(i) == '5' || input.charAt(i) == '6' || input.charAt(i) == '7' || input.charAt(i) == '8' || input.charAt(i) == '9' || input.charAt(i) == '0'))
                  inputValid = false;
              }
              
              if(inputValid && input.length() < 2 && input.length() > 0)
              {
                int intPut = Integer.parseInt(input);
                if(intPut >= 0 && intPut <= 3)
                {
                  weaponRespawnTime = (byte) intPut;
                }else
                {
                  System.out.println("Please select a valid option.");
                  inputValid = false;
                }
              }else
              {
                System.out.println("Please input a valid number.");
                inputValid = false;
              }
            }while(!inputValid);
          }else if(input.equals("5"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Grenades on Map? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                grenadesOnMap = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                grenadesOnMap = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("6"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Overshields on Map? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                overshieldOnMap = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                overshieldOnMap = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("7"))
          {
            boolean inputValid = true;
            do
            {
              inputValid = true;
              System.out.println("Active Camo on Map? (On/Off):");
              input = in.nextLine();
              
              if(input.toLowerCase().equals("on") || input.toLowerCase().equals("yes") || input.toLowerCase().equals("y"))
                activeCamoOnMap = true;
              else if(input.toLowerCase().equals("off") || input.toLowerCase().equals("no") || input.toLowerCase().equals("n"))
                activeCamoOnMap = false;
              else
              {
                inputValid = false;
                System.out.println("Sorry, couldn't process what option you have selected.");
              }
            }while(!inputValid);
          }else if(input.equals("R") || input.equals("r"))
          {
            equipRun = false;
          }else
            System.out.println("Sorry, couldn't process what option you have selected.");
        }
      }else if(input.equals("S") || input.equals("s"))
      {
        run = false;
      }else if(input.equals("Q") || input.equals("q"))
      {
        run = false;
        save = false;
      }else
        System.out.println("Sorry, couldn't process what option you have selected.");
    }
    
    //saving gametype
    if(save)
    {
      //We ree-create the contents of the metadata to save it back, just in case we fixed a gametype name mismatch earlier. 
      metadata = gametypeGamemode + ": " + gametypeName;
      
      //byte array that will hold the raw contents of the new metadata file, this is what we will save when we are done
      byte[] metadataBytes;
      
      //We format the metadata properly for each version of Halo 2
      if(!h2x)
      {
        //we pad out the metadata string with null characters until we are at 128 characters, which when converted to raw bytes, will be 256 bytes
        while(metadata.length() != 128)
          metadata += NULLCHAR;
        
        //we create initialize our metadataBytes variable to 256 bytes
        metadataBytes = new byte[256];
        
        //we convert our metadata string into raw bytes for saving later
        try
        {
          metadataBytes = metadata.getBytes("UTF-16LE");
        }catch(UnsupportedEncodingException e)
        {
          e.printStackTrace();
        }
      }else
      {
        //we add "Name=" to the beginning of the metadata
        metadata = "Name=" + metadata;
        
        //we initalize metadataBytes to the lenght of the string + the needed file header and footer
        metadataBytes = new byte[(metadata.length() * 2) + 6];
        
        //sets the first two bytes of the header to FF and FE, the values of the file header
        metadataBytes[0] = -1;
        metadataBytes[1] = -2;
        
        //we convert the metadata string to raw bytes, them add them to the array after the file header
        try
        {
          byte[] temp = metadata.getBytes("UTF-16LE");
          
          for(int i = 0; i < temp.length; i++)
          {
            metadataBytes[i + 2] = temp[i];
          }
        }catch(UnsupportedEncodingException e)
        {
          e.printStackTrace();
        }
        
        //we set the last four bytes of the file to 0D, 00, 0A, 00 for the file footer. Yes, this actually does represent actual text, but it remains static for all SaveMeta.xbx files, so I don't really care about it.
        metadataBytes[metadataBytes.length - 4] = 13;
        metadataBytes[metadataBytes.length - 3] = 0;
        metadataBytes[metadataBytes.length - 2] = 10;
        metadataBytes[metadataBytes.length - 1] = 0;
      }
      
      //we now save the raw bytes of the metadata. If the original save file was H2X, we save it with the H2V file name, else we save it with the H2X file name.
      try
      {
        File file;
        if(!h2x)
          file = new File(gametypeFolderPath + File.separator + "savemeta.bin");
        else
          file = new File(gametypeFolderPath + File.separator + "SaveMeta.xbx");
        FileOutputStream fileStream = new FileOutputStream(file);
        for(int i = 0; i < metadataBytes.length; i++)
          fileStream.write(metadataBytes[i]);
      }catch(IOException e)
      {
        e.printStackTrace();
      }
      
      
      //Now we put all of our well named variables back into gametypeBytes[] so we can save the byte array back into a file
      
      //For the sake of ease, I just converted these boolean strings into a character array to easily change individual characters. We'll make it back into a string later.
      char gtBools1CharArray[] = gtBools1.toCharArray();
      char gtBools2CharArray[] = gtBools2.toCharArray();
      char gmBoolsCharArray[] = gmBools.toCharArray();
      
      //For each boolean variable, I set the corresponding char in the array to 1 or 0.
      if(friendlyFire == true)
        gtBools1CharArray[0] = '1';
      else
        gtBools1CharArray[0] = '0';
      
      if(teamChanging == true)
        gtBools1CharArray[1] = '1';
      else
        gtBools1CharArray[1] = '0';
      
      if(resolveTies == true)
        gtBools1CharArray[3] = '1';
      else
        gtBools1CharArray[3] = '0';
      
      if(roundsResetMap == true)
        gtBools1CharArray[4] = '1';
      else
        gtBools1CharArray[4] = '0';
      
      if(activeCamo == true)
        gtBools1CharArray[5] = '1';
      else
        gtBools1CharArray[5] = '0';
      
      if(motionSensor == true)
        gtBools1CharArray[6] = '1';
      else
        gtBools1CharArray[6] = '0';
      
      if(teamPlay == true)
        gtBools1CharArray[7] = '1';
      else
        gtBools1CharArray[7] = '0';
      
      
      if(forceEvenTeams == true)
        gtBools2CharArray[1] = '1';
      else
        gtBools2CharArray[1] = '0';
      
      if(damageResistence == true)
        gtBools2CharArray[2] = '1';
      else
        gtBools2CharArray[2] = '0';
      
      if(extraDamage == true)
        gtBools2CharArray[3] = '1';
      else
        gtBools2CharArray[3] = '0';
      
      if(startingGrenades == true)
        gtBools2CharArray[4] = '1';
      else
        gtBools2CharArray[4] = '0';
      
      if(grenadesOnMap == true)
        gtBools2CharArray[5] = '1';
      else
        gtBools2CharArray[5] = '0';
      
      if(activeCamoOnMap == true)
        gtBools2CharArray[6] = '1';
      else
        gtBools2CharArray[6] = '0';
      
      if(overshieldOnMap == true)
        gtBools2CharArray[7] = '1';
      else
        gtBools2CharArray[7] = '0';
      
      
      if(gmOp0 == true)
        gmBoolsCharArray[0] = '1';
      else
        gmBoolsCharArray[0] = '0';
      
      if(gmOp1 == true)
        gmBoolsCharArray[1] = '1';
      else
        gmBoolsCharArray[1] = '0';
      
      if(gmOp2 == true)
        gmBoolsCharArray[2] = '1';
      else
        gmBoolsCharArray[2] = '0';
      
      if(gmOp3 == true)
        gmBoolsCharArray[3] = '1';
      else
        gmBoolsCharArray[3] = '0';
      
      if(gmOp4 == true)
        gmBoolsCharArray[4] = '1';
      else
        gmBoolsCharArray[4] = '0';
      
      if(gmOp5 == true)
        gmBoolsCharArray[5] = '1';
      else
        gmBoolsCharArray[5] = '0';
      
      if(gmOp6 == true)
        gmBoolsCharArray[6] = '1';
      else
        gmBoolsCharArray[6] = '0';
      
      if(gmOp7 == true)
        gmBoolsCharArray[7] = '1';
      else
        gmBoolsCharArray[7] = '0';
      
      //Now, I clear the boolean strings...
      gtBools1 = "";
      gtBools2 = "";
      gmBools = "";
      
      //...and put the char arrays back into the strings
      for(int i = 0; i < gtBools1CharArray.length; i++)
        gtBools1 = gtBools1 + gtBools1CharArray[i];
      
      for(int i = 0; i < gtBools2CharArray.length; i++)
        gtBools2 = gtBools2 + gtBools2CharArray[i];
      
      for(int i = 0; i < gmBoolsCharArray.length; i++)
        gmBools = gmBools + gmBoolsCharArray[i];
      
      //Now that the strings are in orderr, I parse them into an int, and type cast them back to a byte
      gametypeBytes[72] = (byte) Integer.parseInt(gtBools1, 2);
      gametypeBytes[73] = (byte) Integer.parseInt(gtBools2, 2);
      gametypeBytes[240] = (byte) Integer.parseInt(gmBools, 2);
      
      //Match Options
      gametypeBytes[76] = numberOfRounds;
      byteBuffer.putShort(0, score);
      gametypeBytes[80] = byteBuffer.get(0);
      gametypeBytes[81] = byteBuffer.get(1);
      byteBuffer.putShort(0, timeLimit);
      gametypeBytes[84] = byteBuffer.get(0);
      gametypeBytes[85] = byteBuffer.get(1);
      
      //Player Options
      gametypeBytes[120] = maxPlayers;
      gametypeBytes[124] = (byte) lives;
      gametypeBytes[128] = (byte) respawnTime;
      gametypeBytes[132] = (byte) suicidePenalty;
      gametypeBytes[136] = shieldType;
      
      //Team Options
      gametypeBytes[164] = teamScoring;
      gametypeBytes[168] = teamRespawnModifier;
      gametypeBytes[172] = (byte) betrayalPenalty;
      
      
      //Gamemode Options
      if(gametypeGamemode.equals("King of the Hill"))
      {
        byteBuffer.putShort(0, kothMovingHillTime);
        gametypeBytes[244] = byteBuffer.get(0);
        gametypeBytes[245] = byteBuffer.get(1);
      }else if(gametypeGamemode.equals("Oddball"))
      {
        gametypeBytes[244] = oddballBallCount;
        gametypeBytes[246] = oddballBallDamage;
        gametypeBytes[248] = oddballMovementSpeed;
        gametypeBytes[250] = oddballBallIndicator;
      }else if(gametypeGamemode.equals("Juggernaut"))
      {
        gametypeBytes[244] = juggernautMovementSpeed;
      }else if(gametypeGamemode.equals("Capture the Flag"))
      {
        byteBuffer.putShort(0, ctfFlagResetTime);
        gametypeBytes[244] = byteBuffer.get(0);
        gametypeBytes[245] = byteBuffer.get(1);
        gametypeBytes[248] = (byte) ctfSlowWithFlag;
        gametypeBytes[260] = ctfFlagType;
        gametypeBytes[252] = ctfFlagDamage;
        gametypeBytes[256] = ctfFlagIndicator;
      }else if(gametypeGamemode.equals("Assault"))
      {
        byteBuffer.putShort(0, assaultBombResetTime);
        gametypeBytes[244] = byteBuffer.get(0);
        gametypeBytes[245] = byteBuffer.get(1);
        gametypeBytes[248] = assaultSlowWithBomb;
        gametypeBytes[260] = assaultBombType;
        gametypeBytes[256] = assaultBombIndicator;
        gametypeBytes[264] = assaultBombArmTime;
        gametypeBytes[253] = assaultBombDamage;
      }else if(gametypeGamemode.equals("Territories"))
      {
        byteBuffer.putShort(0, territoriesContestTime);
        gametypeBytes[242] = byteBuffer.get(0);
        gametypeBytes[243] = byteBuffer.get(1);
        byteBuffer.putShort(0, territoriesControlTime);
        gametypeBytes[244] = byteBuffer.get(0);
        gametypeBytes[245] = byteBuffer.get(1);
        gametypeBytes[240] = territoryCount;
      }
      
      //Vehicle Options
      gametypeBytes[204] = vehicleRespawn;
      gametypeBytes[205] = primaryVehicle;
      gametypeBytes[206] = secondaryVehicle;
      gametypeBytes[207] = primaryHeavyVehicle;
      gametypeBytes[208] = banshee;
      gametypeBytes[210] = primaryTurret;
      gametypeBytes[211] = secondaryTurret;
      
      //Weapon Options
      gametypeBytes[214] = primaryWeapon;
      gametypeBytes[215] = secondaryWeapon;
      gametypeBytes[212] = weaponsOnMap;
      gametypeBytes[213] = weaponRespawnTime;
      
      //We create the gametypeHashBytes array for the cryptographic hash for the gametype to go into.
      byte[] gametypeHashBytes;
      
      //if the gametype is H2X, we generate the cryptographic hash for the gametype, otherwise we just initalize the array to a size of 0, so it saves nothing for a hash later, since H2V doesn't use hashs.
      if(h2x)
      {
        //We create an int array for Halo 2's secret key to load into. I already generated what Halo 2's secret key is and saved it as a file in the assets folder.
        //I did this instead of generating every time, because thats just a waste of time. I also saved it to a file instead of making an array literal in the source, so that you don't have to edit source and
        //recompile in case your editing a gametype for a Halo 2 mod, such as H2CE, that happens to have a different xbe key (IDK if H2CE actually does) and so will have a different secret key. Just calculate the correct
        //secret key for that Halo 2 mod and save the key's raw bytes into a file using a hex editor, and replace the original secretkey file with the new file you just made. When you want to edit normal Halo 2 gametypes, just swap the original back in.
        //You may have better milage just manually editing gametypes for H2X mods tho, since if they mod weapons or map sets, this program may not be 100% compatiable with that mod. Just place that gametype in your H2V folder after and "convert" it back to H2X to regenerate the hash.
        int[] secretKey = new int[16];
        
        //We load secretkey from the assests folder
        try
        {
          int next = -2;
          File file = new File("." + File.separator + "assets" + File.separator + "secretkey");
          FileInputStream fileStream = new FileInputStream(file);
          for(int i = 0; i < secretKey.length; i++)
          {
            next = fileStream.read();
            if(next >= 0)
              secretKey[i] = next;
          }
          fileStream.close();
        }
        catch(IOException e)
        {
          e.printStackTrace();
        }
        
        //We create a byte array to put the secret key in, because InputStreamReader
        byte[] secretKeyBytes = new byte[16];
        
        //We fill in each byte in the array with the byte value of the corresponding int from the int array
        for(int i = 0; i < secretKeyBytes.length; i++)
          secretKeyBytes[i] = Integer.valueOf(secretKey[i]).byteValue();
        
        //We create a HmacUtils object to generate the hash with, and feed it the correct algorithm to use and the byte array of the secret key to use
        HmacUtils gametypeHasher = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, secretKeyBytes);
        
        //We finally initilize the gametypeHash array to a size of 20 bytes
        gametypeHashBytes = new byte[20];
        
        //We generate the hash of the gametype and save it to the gametype hash array
        gametypeHashBytes = gametypeHasher.hmac(gametypeBytes);
      }else
        gametypeHashBytes = new byte[0]; //H2V doesn't make its save data tamper proof with cryptographic hashes like the Xbox and therefor H2X does, so we don't need this array. This is probably why H2V encrypts its save data tho :P
      
      //I switch this sting from clean to the file name versions
      if(gametypeGamemode.equals("Slayer"))
        gametypeGamemode = "slayer";
      if(gametypeGamemode.equals("Capture the Flag"))
        gametypeGamemode = "ctf";
      if(gametypeGamemode.equals("Oddball"))
        gametypeGamemode = "oddball";
      if(gametypeGamemode.equals("King of the Hill"))
        gametypeGamemode = "koth";
      if(gametypeGamemode.equals("Juggernaut"))
        gametypeGamemode = "juggernaut";
      if(gametypeGamemode.equals("Territories"))
        gametypeGamemode = "territories";
      if(gametypeGamemode.equals("Assault"))
        gametypeGamemode = "assault";
      
      //We save the gametype we loaded up into the new gametype folder. If we generated a gametype hash, we save that too at the end of the file.
      try
      {
        File file = new File(gametypeFolderPath + File.separator + gametypeGamemode);
        FileOutputStream fileStream = new FileOutputStream(file);
        for(int i = 0; i < gametypeBytes.length; i++)
          fileStream.write(gametypeBytes[i]);
        for(int i = 0; i < gametypeHashBytes.length; i++)
          fileStream.write(gametypeHashBytes[i]);
      }catch(IOException e)
      {
        e.printStackTrace();
      }
      
      System.out.println("Changes to " + gametypeName + " (" + gametypeGamemode + ") have been saved successfully~!!\n");
    }
  }
  
  //String gametypeFolderPath should be the folder of the gametype to be converted
  //boolean h2x should be true if the gametype we are converting is h2x (h2x -> h2v conversion), otherwise it should be false
  //String destinationFolderPath should be the path to the gametype folder we are saving it to, Ex. ./4d530064/XXXXXXXXXXXX when converting h2v -> h2x, where X is a hexidecimal number 0-F
  private static void convertGametype(String gametypeFolderPath, boolean h2x, String destinationFolderPath)
  {
    //File object for the gametypes metadata
    File gametypeMetadata;
    
    //File object of the folder we are saving the converted gametype to, then we generate any needed folders so we can save files to this folder later
    File gametypeSaveFolder = new File(destinationFolderPath);
    gametypeSaveFolder.mkdirs();
    
    //initializing the metadata object with the path to the right file
    if(h2x)
      gametypeMetadata = new File(gametypeFolderPath + File.separator + "SaveMeta.xbx");
    else
      gametypeMetadata = new File(gametypeFolderPath + File.separator + "savemeta.bin");
    
    //The String to hold the metadata once we are done loading it
    String metadata = "";
    try
    {
      //The String we will temporarily store the metadata in until we are sure its good
      String line = "";
      
      //Creating a FileInputStream, so that we can specificy the characterset when making the InputStreamReader, so the BufferedReader loads the files text correctly.
      FileInputStream is = new FileInputStream(gametypeMetadata);
      InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-16LE"));
      BufferedReader bufferedReader = new BufferedReader(isr);
      
      //Read the whole file into the String
      line = bufferedReader.readLine();
      
      //We check to make sure the String has actual data in it, and if it does we put it in the metadata String
      if(line != null)
        metadata = line;
    }catch(IOException e)
    {
      //Generic Error Catching because if the file we loaded failed in any way, the above code checking for the gametype is to blame :P
      e.printStackTrace();
    }
    
    //If this is a H2X gametype, Truncating the String to remove the file header and "Name=" String at the start of the file.
    //Thankfully, the file ends in 0x0D, which is a line break, so readLine() ends the String at the end of the useful metadata.
    //else, Truncating the String to remove all of the null characters at the end because thanks H2V!!/s
    if(h2x)
      metadata = metadata.substring(6);
    else
      metadata = metadata.substring(0, metadata.indexOf(NULLCHAR));
    
    //Extracting the gamemode out of the metadata
    String gamemode = metadata.substring(0, metadata.indexOf(':'));
    String gametypeName = metadata.substring(metadata.indexOf(':') + 2);
    
    //Swaps the gamemode name used in the metadata with the name used in the file name, so we know what file name to load and save from later.
    if(gamemode.equals("Slayer"))
      gamemode = "slayer";
    if(gamemode.equals("Capture the Flag"))
      gamemode = "ctf";
    if(gamemode.equals("Oddball"))
      gamemode = "oddball";
    if(gamemode.equals("King of the Hill"))
      gamemode = "koth";
    if(gamemode.equals("Juggernaut"))
      gamemode = "juggernaut";
    if(gamemode.equals("Territories"))
      gamemode = "territories";
    if(gamemode.equals("Assault"))
      gamemode = "assault";
    
    //byte array that will hold the raw contents of the new metadata file, this is what we will save when we are done
    byte[] metadataBytes;
    
    //if the original save is H2X, we create a savemeta for H2V, else we make a savemeta for H2X
    if(h2x)
    {
      //we pad out the metadata string with null characters until we are at 128 characters, which when converted to raw bytes, will be 256 bytes
      while(metadata.length() != 128)
        metadata += NULLCHAR;
      
      //we create initialize our metadataBytes variable to 256 bytes
      metadataBytes = new byte[256];
      
      //we convert our metadata string into raw bytes for saving later
      try
      {
        metadataBytes = metadata.getBytes("UTF-16LE");
      }catch(UnsupportedEncodingException e)
      {
        e.printStackTrace();
      }
    }else
    {
      //we add "Name=" to the beginning of the metadata
      metadata = "Name=" + metadata;
      
      //we initalize metadataBytes to the lenght of the string + the needed file header and footer
      metadataBytes = new byte[(metadata.length() * 2) + 6];
      
      //sets the first two bytes of the header to FF and FE, the values of the file header
      metadataBytes[0] = -1;
      metadataBytes[1] = -2;
      
      //we convert the metadata string to raw bytes, them add them to the array after the file header
      try
      {
        byte[] temp = metadata.getBytes("UTF-16LE");
        
        for(int i = 0; i < temp.length; i++)
        {
          metadataBytes[i + 2] = temp[i];
        }
      }catch(UnsupportedEncodingException e)
      {
        e.printStackTrace();
      }
      
      //we set the last four bytes of the file to 0D, 00, 0A, 00 for the file footer. Yes, this actually does represent actual text, but it remains static for all SaveMeta.xbx files, so I don't really care about it.
      metadataBytes[metadataBytes.length - 4] = 13;
      metadataBytes[metadataBytes.length - 3] = 0;
      metadataBytes[metadataBytes.length - 2] = 10;
      metadataBytes[metadataBytes.length - 1] = 0;
    }
    
    //we now save the raw bytes of the metadata. If the original save file was H2X, we save it with the H2V file name, else we save it with the H2X file name.
    try
    {
      File file;
      if(h2x)
        file = new File(destinationFolderPath + File.separator + "savemeta.bin");
      else
        file = new File(destinationFolderPath + File.separator + "SaveMeta.xbx");
      FileOutputStream fileStream = new FileOutputStream(file);
      for(int i = 0; i < metadataBytes.length; i++)
        fileStream.write(metadataBytes[i]);
    }catch(IOException e)
    {
      e.printStackTrace();
    }
    
    //We create an int array to load the gametype into
    int[] gametype = new int[304];
    
    //we load in the raw byte values of the gametype file. We stop after 304 bytes because thats the end of the gametype itself, so if its a H2X gametype we don't read in the cryptographic hash, as we don't need it.
    try
    {
      int next = -2;
      File file = new File(gametypeFolderPath + File.separator + gamemode);
      FileInputStream fileStream = new FileInputStream(file);
      for(int i = 0; i < gametype.length; i++)
      {
        next = fileStream.read();
        if(next >= 0)
          gametype[i] = next;
      }
      fileStream.close();
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }
    
    //Now we create an actual byte array to store the gametype in. We only made an int array because FileInputStream will only return ints.
    byte[] gametypeBytes = new byte[304];
    
    //We fill in each byte in the array with the byte value of the corresponding int from the int array
    for(int i = 0; i < gametypeBytes.length; i++)
      gametypeBytes[i] = Integer.valueOf(gametype[i]).byteValue();
    
    //We check to make sure that the file header in the gametype is valid. If it isn't, we check if its an H2V gametype. If it is, we assume its encrypted and ask them to load it Project Cartographer first. If it isn't, we just assume its corrupted.
    if(!(gametypeBytes[0] == 0 && gametypeBytes[1] == 0 && gametypeBytes[2] == 0 && gametypeBytes[3] == -1))
    {
      if(!h2x)
        System.err.println("Error! Encrypted H2V gametype detected! Please delete the converted gametype, as it WILL NOT WORK!! This program does not handle decrypting H2V gametypes. Please use this tool to decrypt your gametypes, then try loading them in H2GEM again: https://halo2.online/threads/halo-2-saved-game-profiles-and-variants-transfer.2634/ You can also load it up in Project Cartographer to decrypt it.");
      else
        System.err.println("Error! Corrupted Gametype detected! Please delete the converted gametype, as it WILL NOT WORK!! Please either delete the source gametype, or manually verify the file is uncorrupted before converting it again.");
    }
    
    //We create the gametypeHashBytes array for the cryptographic hash for the gametype to go into.
    byte[] gametypeHashBytes;
    
    //if the original gametype was H2V, we generate the cryptographic hash for the gametype, otherwise we just initalize the array to a size of 0, so it saves nothing for a hash later, since H2V doesn't use hashs.
    if(!h2x)
    {
      //We create an int array for Halo 2's secret key to load into. I already generated what Halo 2's secret key is and saved it as a file in the assets folder.
      //I did this instead of generating every time, because thats just a waste of time. I also saved it to a file instead of making an array literal in the source, so that you don't have to edit source and
      //recompile in case your editing a gametype for a Halo 2 mod, such as H2CE, that happens to have a different xbe key (IDK if H2CE actually does) and so will have a different secret key. Just calculate the correct
      //secret key for that Halo 2 mod and save the key's raw bytes into a file using a hex editor, and replace the original secretkey file with the new file you just made. When you want to edit normal Halo 2 gametypes, just swap the original back in.
      //You may have better milage just manually editing gametypes for H2X mods tho, since if they mod weapons or map sets, this program may not be 100% compatiable with that mod. Just place that gametype in your H2V folder after and "convert" it back to H2X to regenerate the hash.
      int[] secretKey = new int[16];
      
      //We load secretkey from the assests folder
      try
      {
        int next = -2;
        File file = new File("." + File.separator + "assets" + File.separator + "secretkey");
        FileInputStream fileStream = new FileInputStream(file);
        for(int i = 0; i < secretKey.length; i++)
        {
          next = fileStream.read();
          if(next >= 0)
            secretKey[i] = next;
        }
        fileStream.close();
      }
      catch(IOException e)
      {
        e.printStackTrace();
      }
      
      //We create a byte array to put the secret key in, because InputStreamReader
      byte[] secretKeyBytes = new byte[16];
      
      //We fill in each byte in the array with the byte value of the corresponding int from the int array
      for(int i = 0; i < secretKeyBytes.length; i++)
        secretKeyBytes[i] = Integer.valueOf(secretKey[i]).byteValue();
      
      //We create a HmacUtils object to generate the hash with, and feed it the correct algorithm to use and the byte array of the secret key to use
      HmacUtils gametypeHasher = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, secretKeyBytes);
      
      //We finally initilize the gametypeHash array to a size of 20 bytes
      gametypeHashBytes = new byte[20];
      
      //We generate the hash of the gametype and save it to the gametype hash array
      gametypeHashBytes = gametypeHasher.hmac(gametypeBytes);
    }else
      gametypeHashBytes = new byte[0]; //H2V doesn't make its save data tamper proof with cryptographic hashes like the Xbox and therefor H2X does, so we don't need this array. This is probably why H2V encrypts its save data tho :P
    
    //We save the gametype we loaded up into the new gametype folder. If we generated a gametype hash, we save that too at the end of the file.
    try
    {
      File file = new File(destinationFolderPath + File.separator + gamemode);
      FileOutputStream fileStream = new FileOutputStream(file);
      for(int i = 0; i < gametypeBytes.length; i++)
        fileStream.write(gametypeBytes[i]);
      for(int i = 0; i < gametypeHashBytes.length; i++)
        fileStream.write(gametypeHashBytes[i]);
    }catch(IOException e)
    {
      e.printStackTrace();
    }
    
    //If we are converting to a H2X save file, we create a copy of SaveImage.xbx for the assets folder in the gametype folder.
    //Halo 2 doesn't itself use this file, but the Xbox dashboard does, hence why I only copy this for H2X gametypes, but not H2V gametypes.
    if(!h2x)
    {
      //int array to load the saveImage into
      int[] saveImage = new int[4096];
      
      //we load saveImage
      try
      {
        int next = -2;
        File file = new File("." + File.separator + "assets" + File.separator + "SaveImage.xbx");
        FileInputStream fileStream = new FileInputStream(file);
        for(int i = 0; i < saveImage.length; i++)
        {
          next = fileStream.read();
          if(next >= 0)
            saveImage[i] = next;
        }
        fileStream.close();
      }
      catch(IOException e)
      {
        e.printStackTrace();
      }
      
      //we create a byte array to put saveImage so we can save it
      byte[] saveImageBytes = new byte[4096];
      
      //We fill in each byte in the array with the byte value of the corresponding int from the int array
      for(int i = 0; i < saveImageBytes.length; i++)
        saveImageBytes[i] = Integer.valueOf(saveImage[i]).byteValue();
      
      //and finally we save the byte array to file
      try
      {
        File file = new File(destinationFolderPath + File.separator + "SaveImage.xbx");
        FileOutputStream fileStream = new FileOutputStream(file);
        for(int i = 0; i < saveImageBytes.length; i++)
          fileStream.write(saveImageBytes[i]);
      }catch(IOException e)
      {
        e.printStackTrace();
      }
    }
  }
}