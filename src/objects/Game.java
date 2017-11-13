package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Game 
{
	private String gameName;
	private int numPlayers;
	private Map<String, PlayerThread> players = new HashMap<String, PlayerThread>(); // map of usernames to threads
	private Map<Integer, PlayerThread> playersInOrder = new HashMap<Integer, PlayerThread>(); // arraylist of users in order joined
	// private boolean isActive = false; // checks if game has started or not
	
	private String creatorName; // name of player that started game
	private PlayerThread creatorThread; // thread of player that started game
	
	// GAMEPLAY VARIABLES
	private Deck gameDeck;
	private ArrayList<Card> dealerHand; // represents dealer
	
	private int dealerCardSum = 0;
	private boolean dealerHasAce = false;
	private boolean dealerBlackJack = false;
	private boolean dealerBusted = false;
	
	private int round = 1; // keeps track of round number
	
	public void resetDealerValues()
	{
		clearDealerHand();
		setDealerCardSum(0);
		setDealerBlackJack(false);
		setDealerBusted(false);
		setDealerHasAce(false);
	}
	
	public void addToDealerHand(Card c)
	{
		dealerHand.add(c);
		
		int faceValue = c.getFace(); 
		if(faceValue == 1) // Ace
		{
			dealerHasAce = true;
		}
		else if(faceValue == 11 || faceValue == 12 || faceValue == 13) // Jack, Queen, King
		{
			faceValue = 10;
		}
		dealerCardSum += faceValue;
		
		// check cases for when Ace valued at 11
		if(dealerHasAce)
		{
			if((dealerCardSum - 1) + 11 > 21) // if Ace valued at 11 causes bust
			{
				dealerHasAce = false; // treat Ace as 1
			} 
			else if((dealerCardSum - 1) + 11 == 21) // if Ace valued at 11 causes BlackJack
			{
				dealerCardSum = 21;
			}
		}
		
		// update status based on new sum
		if(dealerCardSum == 21)
		{
			dealerBlackJack = true;
		}
		else if(dealerCardSum > 21)
		{
			dealerBlackJack = false;
			dealerBusted = true;
		}
	}
	
	/* Function to get player's status */
	public String getDealerStatus()
	{
		String status = Integer.toString( dealerCardSum );
		if(dealerBlackJack) // player has blackjack
		{
			status += " - blackjack";
		}
		else if(dealerBusted)
		{
			status += " - bust";
		}
		return status;
	}
	
	/* Function to clear dealer hand */
	public void clearDealerHand()
	{
		dealerHand.clear();
	}
	
	public String getPlayerState(String playerName, PlayerThread playerThread)
	{
		String s = "----------------------------------------------------------------------------\n";
		s += "Player: " + playerName + "\n\n";
		s += "Status: " + getPlayerStatus(playerThread);
		s += "\nCards: | ";
		for(int i = 0; i < playerThread.getPlayerHand().size(); i++)
		{
			s += playerThread.getPlayerHand().get(i).toString() + " | ";
		}
		s += "\nChip Total: " + playerThread.getChipTotal() + " | Bet Amount: " + playerThread.getBetAmount();
		s += "\n----------------------------------------------------------------------------\n";
		
		return s;
	}
	
	/* Function to get player's status */
	public String getPlayerStatus(PlayerThread playerThread)
	{
		String status = Integer.toString( playerThread.getCardSum() );
		if(playerThread.blackJack()) // player has blackjack
		{
			status += " - blackjack";
		}
		else if(playerThread.busted())
		{
			status += " - bust";
		}
		else if(playerThread.hasAce()) // neither bust/blackjack but has Ace
		{
			status += " or " + Integer.toString(playerThread.getCardSum() + 10); // Ace valued at 11
		}
		return status;
	}

	public Game(int numPlayers, String gameName, String playerName, PlayerThread playerThread)
	{
		this.numPlayers = numPlayers;
		this.gameName = gameName;
		this.players.put(playerName, playerThread); // add to map
		this.playersInOrder.put(0, playerThread); // add to ordered list
		
		// Set game creator variables
		this.creatorName = playerName;
		this.creatorThread = playerThread;
		
		// FOR GAMEPLAY
		this.gameDeck = new Deck();
		this.dealerHand = new ArrayList<Card>();
	}
	
	public int getPlayerIndex(PlayerThread playerThread)
	{		    
	    for(Map.Entry<Integer, PlayerThread> entry: playersInOrder.entrySet())
	    {
            if(playerThread.equals(entry.getValue()))
            {
            		return (int) entry.getKey();
            }
        }
	    return -1;
	}
	
	public void insertPlayer(String n, PlayerThread t) 
	{
		this.players.put(n, t); // add to map
		int x = playersInOrder.size();
		this.playersInOrder.put(x, t); // add to ordered map of users w/ indices
	}
	
	public Map<String, PlayerThread> getPlayers() {
		return players;
	}
	
	public Map<Integer, PlayerThread> getPlayersInOrder() {
		return playersInOrder;
	}
	
	public String getGameName() {
		return gameName;
	}
	
	public int getNumPlayers() {
		return numPlayers;
	}
	
	public String getCreatorName() {
		return creatorName;
	}

	public PlayerThread getCreatorThread() {
		return creatorThread;
	}
	
	public Deck getGameDeck() {
		return gameDeck;
	}

	public void setGameDeck(Deck gameDeck) {
		this.gameDeck = gameDeck;
	}

	public ArrayList<Card> getDealerHand() {
		return dealerHand;
	}

	public void setDealerHand(ArrayList<Card> dealerHand) {
		this.dealerHand = dealerHand;
	}

	public int getDealerCardSum() {
		return dealerCardSum;
	}

	public void setDealerCardSum(int dealerCardSum) {
		this.dealerCardSum = dealerCardSum;
	}

	public boolean isDealerHasAce() {
		return dealerHasAce;
	}

	public void setDealerHasAce(boolean dealerHasAce) {
		this.dealerHasAce = dealerHasAce;
	}

	public boolean isDealerBlackJack() {
		return dealerBlackJack;
	}

	public void setDealerBlackJack(boolean dealerBlackJack) {
		this.dealerBlackJack = dealerBlackJack;
	}

	public boolean isDealerBusted() {
		return dealerBusted;
	}

	public void setDealerBusted(boolean dealerBusted) {
		this.dealerBusted = dealerBusted;
	}

	public int getRound() {
		return round;
	}

	public void incrementRound() {
		this.round++;
	}
}
