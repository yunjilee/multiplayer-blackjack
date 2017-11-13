package objects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import gameMessages.EndGameMessage;
import gameMessages.GetBetMessage;
import gameMessages.GetStayOrHitMessage;
import gameMessages.ReturnBetMessage;
import gameMessages.ReturnStayOrHitMessage;
import threadMessages.AddPlayerMessage;
import threadMessages.Message;
import threadMessages.NewGameMessage;
import threadMessages.ResponseMessage;
import threadMessages.ValidationMessage;

public class GameClient extends Thread 
{
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	public GameClient(String hostname, int port) throws IOException
	{
		try {
			Socket s = new Socket(hostname, port);
			ois = new ObjectInputStream(s.getInputStream());
			oos = new ObjectOutputStream(s.getOutputStream());			
		} catch (IOException ioe) {
			throw ioe;
		}
	}
	public void run() 
	{
		// Starts once validation of all required fields is done
		boolean endGame = false;
		
		while(!endGame) 
		{
			try {
				Object o = ois.readObject();
				if(o instanceof Message) // Start new round
				{
					Message m = (Message)o;
					System.out.println(m.getMessage());
				}
				else if(o instanceof GetBetMessage) // Start new round
				{
					int bet = 0;
					Scanner in = new Scanner(System.in);	
					while(true)
					{
						if(in.hasNextInt())
						{
							bet = in.nextInt();
							break;
						}
					}
					ReturnBetMessage rbm = new ReturnBetMessage(bet); // return int to server
					oos.writeObject(rbm);
					oos.flush();
				}
				else if(o instanceof GetStayOrHitMessage) // Get player choice to stay or hit
				{
					System.out.println("Enter either '1' or 'stay' to stay. Enter either '2' or 'hit' to hit.");
					String choice;
					Scanner in = new Scanner(System.in);
					while(true)
					{
						if(in.hasNextLine())
						{
							choice = in.nextLine();
							break;
						}
					}
					ReturnStayOrHitMessage rsohm = new ReturnStayOrHitMessage(choice); // return String to server
					oos.writeObject(rsohm);
					oos.flush();
				}
				else if(o instanceof EndGameMessage)
				{
					System.out.println("Terminating client program.");
					EndGameMessage egm = (EndGameMessage)o;
					oos.writeObject(egm);
					endGame = true;
				}
			} catch (IOException ioe) {
				System.out.println("ioe in GameClient.run(): " + ioe.getMessage());
			} catch (ClassNotFoundException cnfe) {
				System.out.println("cnfe in GameClient.run(): " + cnfe.getMessage());
			}
		}
	}
	
	// Client's Main Method
	public static void main(String [] args)
	{
		String ipAddress = "";
		int portNumber = 0;
		boolean validFields = false;
		GameClient gc = null;
		
		System.out.println("Welcome to Black Jack!");
		
		while(!validFields)
		{
			System.out.println("Please enter the ipaddress");
			Scanner in = new Scanner(System.in);
			
			if(in.hasNextLine())
			{
				ipAddress = in.nextLine();
			}
			System.out.println("Please enter the port");
			if(in.hasNextInt())
			{
				portNumber = in.nextInt();
			}
			try 
			{
				gc = new GameClient(ipAddress, portNumber);
				validFields = true;
			} catch (IOException ioe) {
				System.out.println("Unable to connect to server with provided fields");
				continue;
			}
		}
		
		boolean validOption = false; // flag for whether option is valid
		int option = 0;
		while(!validOption)
		{
			System.out.println("Please choose from the options below");
			System.out.println("1) Start Game");
			System.out.println("2) Join Game");
			
			Scanner in = new Scanner(System.in);	
			if(in.hasNextInt())
			{
				option = in.nextInt();
				if(option < 1 || option > 2)
				{
					System.out.println("Invalid option");
					continue;
				}
				validOption = true;
			}
		}
		
		if(option == 1)
		{
			gc.startGame(); // START GAME
		}
		else if(option == 2)
		{
			gc.joinGame(); // JOIN EXISTING GAME
		}
		
		gc.start(); // start thread
	}
	
	public void startGame()
	{
		int numPlayers = 0;
		String gameName = "";
		boolean validGameName = false;
		String playerName = "";
		
		/* 1) Ask the user to choose the number of players in the game. This will be a number between 1 and 3, inclusively. */
		while(numPlayers < 1 || numPlayers > 3)
		{
			System.out.println("Please choose the number of players in the game (1-3)");
			
			Scanner in = new Scanner(System.in);	
			if(in.hasNextInt())
			{
				numPlayers = in.nextInt();
				if(numPlayers < 1 || numPlayers > 3)
				{
					System.out.println("Invalid number of players");
					continue;
				}
			}
		}
		
		/* 2) The user should then be prompted for a unique name for their game. */
		while(!validGameName)
		{
			System.out.println("Please choose a name for your game");
			
			Scanner in = new Scanner(System.in);	
			if(in.hasNextLine())
			{
				gameName = in.nextLine();
				ValidationMessage m = new ValidationMessage(gameName, 1);
				ResponseMessage rm = null;
				try {
					oos.writeObject(m);
					oos.flush();
				} catch (IOException ioe) {
					System.out.println("ioe in sending message in GameClient: " + ioe.getMessage());
				}
				
				try {
					rm = (ResponseMessage)ois.readObject();
					validGameName = rm.getIsValid();	
				} catch (ClassNotFoundException cnfe) {
					System.out.println("cnfe in reading message in GameClient: " + cnfe.getMessage());
				} catch (IOException ioe) {
					System.out.println("ioe in reading message in GameClient: " + ioe.getMessage());
				}
	
				if(!validGameName)
				{
					System.out.println("Invalid choice. This game name has already been chosen by another user.");
					continue;
				}
			}
		}
		
		/* 3) The user should then be prompted for their username, which can be anything except the
		empty string. */
		while(playerName == "" || playerName == null)
		{
			System.out.println("Please choose a username");
			Scanner in = new Scanner(System.in);	
			if(in.hasNextLine())
			{
				playerName = in.next();
			}
		}
		
		/* Add new game object with inputted information */
		NewGameMessage ngm = new NewGameMessage(numPlayers, gameName, playerName);
		try {
			oos.writeObject(ngm);
			oos.flush();
		} catch (IOException ioe) {
			System.out.println("ioe in sending new game information in GameClient: " + ioe.getMessage());
		}
		
		/* 4) The user should then see a message with the number of players that must join before the
		game can start. */
		System.out.println("Waiting for " + (numPlayers - 1) + " other player(s) to join...");
	}
	
	public void joinGame()
	{
		boolean validGameName = false;
		boolean validPlayerName = false;
		String gameName = "";
		String playerName = "";
		
		/* 1) The user should be prompted for the name of the game they wish to join. */
		while(!validGameName)
		{
			System.out.println("Please enter the name of the game you wish to join");
			Scanner in = new Scanner(System.in);	
			
			if(in.hasNextLine())
			{
				gameName = in.nextLine();
				ValidationMessage m = new ValidationMessage(gameName, 2);
				ResponseMessage rm = null;
				try {
					oos.writeObject(m);
					oos.flush();
				} catch (IOException ioe) {
					System.out.println("ioe in sending game name message in GameClient: " + ioe.getMessage());
				}
				
				try {
					rm = (ResponseMessage)ois.readObject();
					validGameName = rm.getIsValid();	
				} catch (ClassNotFoundException cnfe) {
					System.out.println("cnfe: " + cnfe.getMessage());
				} catch (IOException ioe) {
					System.out.println("ioe in reading valid game name message in GameClient: " + ioe.getMessage());
				}
	
				if(!validGameName)
				{
					System.out.println("Invalid choice. There are no ongoing games with this name, or game has already started");
					continue;
				}
			}
		}
		
		/* 2) Have the user enter their choice of username. */
		while(!validPlayerName)
		{
			System.out.println("Please choose a username");
			Scanner in = new Scanner(System.in);	
			
			if(in.hasNextLine())
			{
				playerName = in.nextLine();
				ValidationMessage m = new ValidationMessage(playerName, 3);
				ResponseMessage rm = null;
				try {
					oos.writeObject(m);
					oos.flush();
				} catch (IOException ioe) {
					System.out.println("ioe in sending player name message in GameClient: " + ioe.getMessage());
				}
				
				try {
					rm = (ResponseMessage)ois.readObject();
					validPlayerName = rm.getIsValid();	
				} catch (ClassNotFoundException cnfe) {
					System.out.println("cnfe: " + cnfe.getMessage());
				} catch (IOException ioe) {
					System.out.println("ioe in reading valid player name message in GameClient: " + ioe.getMessage());
				}
	
				if(!validPlayerName)
				{
					System.out.println("Invalid choice. This username has already been chosen by another player in this game");
					continue;
				}
			}
		}
		
		/* 3) Print a message that the game will begin shortly after other players join. */
		System.out.println("The game will start shortly. Waiting for other players to join...");
		
		/* Add player to game */
		AddPlayerMessage apm = new AddPlayerMessage(playerName, gameName);
		try {
			oos.writeObject(apm);
			oos.flush();
		} catch (IOException ioe) {
			System.out.println("ioe in sending add player information in GameClient: " + ioe.getMessage());
		}
		
	}
}