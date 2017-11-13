package objects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

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

// Middleman between server and client (1 created per player)
public class PlayerThread extends Thread
{
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private GameServer gs;
	
	private String playerName; // player's name
	private Game playerGame; // game player is currently in
	private String playerGameName; // name of game player is currently in
	
	// GAMEPLAY VARIABLES
	private int chipTotal = 500;
	private ArrayList<Card> playerHand = new ArrayList<Card>();
	private int betAmount = -1;
	private int cardSum = 0;
	
	private boolean hasAce = false; // if player has ace valued at 11
	private boolean busted = false;
	private boolean blackJack = false;
	
	public boolean hasAce()
	{
		return hasAce;
	}
	public boolean busted()
	{
		return busted;
	}
	public boolean blackJack()
	{
		return blackJack;
	}
	
	public void setHasAce(boolean x) 
	{
		this.hasAce = x;
	}
	public void setBusted(boolean x) 
	{
		this.busted = x;
	}
	public void setBlackJack(boolean x) 
	{
		this.blackJack = x;
	}

	/* Function to reset values after round */
	public void resetValues()
	{
		clearPlayerHand();
		betAmount = -1;
		cardSum = 0;
		hasAce = false;
		busted = false;
		blackJack = false;
	}
	
	public void addToPlayerHand(Card c)
	{
		playerHand.add(c);
		
		int faceValue = c.getFace(); 
		if(faceValue == 1) // Ace
		{
			hasAce = true;
		}
		else if(faceValue == 11 || faceValue == 12 || faceValue == 13) // Jack, Queen, King
		{
			faceValue = 10;
		}
		cardSum += faceValue;
		
		// check cases for when Ace valued at 11
		if(hasAce)
		{
			if((cardSum - 1) + 11 > 21) // if Ace valued at 11 causes bust
			{
				hasAce = false; // treat Ace as 1
			} 
			else if((cardSum - 1) + 11 == 21) // if Ace valued at 11 causes BlackJack
			{
				cardSum = 21;
			}
		}
		
		// update status based on new sum
		if(cardSum == 21)
		{
			blackJack = true;
		}
		else if(cardSum > 21)
		{
			blackJack = false;
			busted = true;
		}
	}
	
	/* Function to clear player hand */
	public void clearPlayerHand()
	{
		playerHand.clear();
	}
	
	public PlayerThread(Socket s, GameServer gs) 
	{
		try {
			this.gs = gs;
			oos = new ObjectOutputStream(s.getOutputStream());
			ois = new ObjectInputStream(s.getInputStream());
			this.start();
		} catch (IOException ioe) {
			System.out.println("ioe in PlayerThread constructor: " + ioe.getMessage());
		}
	}
	
	// Function to send simple String messages
	public void sendMessage(Object o) 
	{
		try {
			if(o instanceof Message)
			{
				Message m = (Message)o;
				oos.writeObject(m); // goes to client
			}
			else if(o instanceof GetBetMessage)
			{
				GetBetMessage gbm = (GetBetMessage)o;
				oos.writeObject(gbm);
			}
			else if(o instanceof GetStayOrHitMessage)
			{
				GetStayOrHitMessage gsohm = (GetStayOrHitMessage)o;
				oos.writeObject(gsohm);
			}
			else if(o instanceof EndGameMessage) // End game
			{
				EndGameMessage egm = (EndGameMessage)o;
				oos.writeObject(egm);
			}
			
			oos.flush();
		} catch (IOException ioe) {
			System.out.println("ioe in sending message in PlayerThread: " + ioe.getMessage());
		}
	}

	public void run() 
	{
		
		while(true) 
		{
			try {
				Object o = ois.readObject();
				
				if(o instanceof ValidationMessage) // Validation
				{
					ValidationMessage vm = (ValidationMessage)o;
					ResponseMessage rm = null;
					boolean isValid = false;
					
					if(vm.getMessageType() == 1) // Validate new game name
					{
						isValid = gs.validateGameName(vm, this);
					}
					else if(vm.getMessageType() == 2) // Validate ongoing game name
					{
						isValid = gs.validateOngoingGameName(vm);
						if(isValid) {
							this.playerGameName = vm.getMessage();
						}
					}
					else if(vm.getMessageType() == 3) // Validate user name
					{
						isValid = gs.validatePlayerName(vm, this.playerGameName);
						if(isValid) {
							this.playerName = vm.getMessage();
						}
					}
					
					rm = new ResponseMessage(isValid);
					oos.writeObject(rm);
					oos.flush();
				}
				else if(o instanceof NewGameMessage) // Create new game
				{
					NewGameMessage ngm = (NewGameMessage)o;
					playerName = ngm.getPlayerName();
					playerGameName = ngm.getGameName();
					playerGame = gs.createGame(ngm.getNumPlayers(), ngm.getGameName(), ngm.getPlayerName(), this);
					gs.checkIfCanStart(playerName, playerGameName, playerGame.getPlayers().get(playerName));
				}
				
				else if(o instanceof AddPlayerMessage) // Add a player to an existing game
				{
					AddPlayerMessage agm = (AddPlayerMessage)o;
					playerGameName = agm.getGameName();
					playerName = agm.getPlayerName();
					gs.addPlayer(agm.getPlayerName(), agm.getGameName(), this);
				}
				
				// GAMEPLAY MESSAGES
				else if(o instanceof ReturnBetMessage) // Get bet amount from user
				{
					ReturnBetMessage rbm = (ReturnBetMessage)o;
					this.betAmount = rbm.getBet(); // set player's betAmount					
					gs.receivePlayerBet(playerGame.getPlayerIndex(this), this, playerGame);
				}
				else if(o instanceof ReturnStayOrHitMessage) // Get choice from user
				{
					ReturnStayOrHitMessage rsohm = (ReturnStayOrHitMessage)o;
					gs.receiveStayOrHit(playerGame.getPlayerIndex(this), this, playerGame, rsohm.getChoice());
				}
				else if(o instanceof EndGameMessage) // End game
				{
					break;
				}
			} catch (IOException ioe) {
				System.out.println("ioe in PlayerThread.run(): " + ioe.getMessage());
			} catch (ClassNotFoundException cnfe) {
				System.out.println("cnfe in PlayerThread.run(): " + cnfe.getMessage());
			}
		}
	}
	
	
	// VARIABLE ACCESSORS
	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public Game getPlayerGame() {
		return playerGame;
	}

	public void setPlayerGame(Game playerGame) {
		this.playerGame = playerGame;
	}

	public String getPlayerGameName() {
		return playerGameName;
	}

	public void setPlayerGameName(String playerGameName) {
		this.playerGameName = playerGameName;
	}

	public int getChipTotal() {
		return chipTotal;
	}

	public void setChipTotal(int chipTotal) {
		this.chipTotal = chipTotal;
	}

	public ArrayList<Card> getPlayerHand() {
		return playerHand;
	}

	public void setPlayerHand(ArrayList<Card> playerHand) {
		this.playerHand = playerHand;
	}

	public int getBetAmount() {
		return betAmount;
	}

	public void setBetAmount(int betAmount) {
		this.betAmount = betAmount;
	}

	public int getCardSum() {
		return cardSum;
	}

	public void setCardSum(int cardSum) {
		this.cardSum = cardSum;
	}
}
