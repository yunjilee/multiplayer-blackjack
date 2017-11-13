package objects;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import gameMessages.EndGameMessage;
import gameMessages.GetBetMessage;
import gameMessages.GetStayOrHitMessage;
import threadMessages.Message;
import threadMessages.ValidationMessage;

public class GameServer 
{
	private Vector<PlayerThread> playerThreads; // Vector of all players
	private Map<String, Game> serverGames = new HashMap<String, Game>(); // Map of game name to Game object
	
	public GameServer(int port) throws IOException
	{
		try 
		{
			ServerSocket ss = new ServerSocket(port); // ServerSocket listens on port on server machine for incoming connections
			System.out.println("Successfully started the Black Jack server on port " + port);
			playerThreads = new Vector<PlayerThread>();
			while(true) // Server thread should always be open
			{
				Socket s = ss.accept(); // Blocking, on client connect attempt, Socket connecting server to client returned
				System.out.println("Player connected: " + s.getInetAddress()); // A client has connected
				PlayerThread st = new PlayerThread(s, this); // Create new thread, connect socket to client
				playerThreads.add(st);
			}
		} catch (IOException ioe) {
			throw ioe;
		}
	}
	
	public void broadcastToOthers(Message m, PlayerThread p, Game g)
	{
		// send messages to everyone except this player
		if(m != null)
		{
			for(int i = 0; i < g.getPlayersInOrder().size(); i++) // loop through players in order of join time
			{
				if(p != g.getPlayersInOrder().get(i))
				{
					g.getPlayersInOrder().get(i).sendMessage(m); // send out message
				}
			}
		}
	}
	
	public void broadcastToPlayer(Message m, PlayerThread p, Game g)
	{
		// send message to only this player
		if(m != null)
		{
			p.sendMessage(m); // send out message
		}
	}
	
	public void broadcastToAll(Message m, Game g)
	{
		// send message to all players
		if(m != null)
		{
			for(int i = 0; i < g.getPlayersInOrder().size(); i++) // loop through players in order of join time
			{
				g.getPlayersInOrder().get(i).sendMessage(m); // send out message
			}
		}
	}
	
	public void sendEndGameMessages(EndGameMessage egm, Game g)
	{
		// send end game message to all players
		if(egm != null)
		{
			for(int i = 0; i < g.getPlayersInOrder().size(); i++) // loop through players in order of join time
			{
				g.getPlayersInOrder().get(i).sendMessage(egm); // send out message
			}
		}
		
		// remove game from map
		serverGames.remove(g.getGameName()); 
	}
	
	/* Function to validate game name */
	public boolean validateGameName(ValidationMessage gameName, PlayerThread pt)
	{
		if(serverGames.containsKey(gameName.getMessage()))
		{
			return false; // Game name already exists
		}
		return true; // Game name is valid
	}
	
	/* Function to validate player name */
	public boolean validatePlayerName(ValidationMessage playerName, String gameName)
	{
		Map<String, PlayerThread> players = serverGames.get(gameName).getPlayers();

		if(players.containsKey(playerName.getMessage()))
		{
			return false; // Player name already exists
		}
		return true; // Player name is valid
	}
	
	/* Function to validate ongoing game name */
	public boolean validateOngoingGameName(ValidationMessage gameName)
	{
		if(serverGames.containsKey(gameName.getMessage())) // Game with name exists
		{
			if(serverGames.get(gameName.getMessage()).getPlayers().size() < serverGames.get(gameName.getMessage()).getNumPlayers()) // Game is waiting for players
			{
				return true;
			}
		}
		return false; // Invalid game
	}
	
	/* Function to create game */
	public Game createGame(int numPlayers, String gameName, String userName, PlayerThread playerThread)
	{
		Game g = new Game(numPlayers, gameName, userName, playerThread);
		serverGames.put(gameName, g); // Add new game to map
		
		return g;
	}
	
	/* Function to add player to game */
	public void addPlayer(String playerName, String gameName, PlayerThread playerThread)
	{
		serverGames.get(gameName).insertPlayer(playerName, playerThread);
		playerThread.setPlayerGame(serverGames.get(gameName));
		
		// Send (only) creator a notification that another player has joined
		String msgString = playerName + " joined the game";
		if(serverGames.get(gameName).getPlayers().size() < serverGames.get(gameName).getNumPlayers())
		{
			msgString += "\nWaiting for " + (serverGames.get(gameName).getNumPlayers() - serverGames.get(gameName).getPlayers().size()) + " other player(s) to join...";
		}
		Message msg = new Message(msgString);
		serverGames.get(gameName).getCreatorThread().sendMessage(msg);
		
		checkIfCanStart(playerName, gameName, playerThread);
	}
	
	/* Function to check if can start game */
	public void checkIfCanStart(String playerName, String gameName, PlayerThread playerThread)
	{
		// Check if enough players, if so start game (send commence message to all players in game)
		String startMsgString = "Let the game commence. Good luck to all players!";
		Message startMsg = new Message(startMsgString);
		if(serverGames.get(gameName).getPlayers().size() == serverGames.get(gameName).getNumPlayers())
		{
			for(PlayerThread threads : serverGames.get(gameName).getPlayers().values()) 
			{
				threads.sendMessage(startMsg);
			}
			startRound(gameName);
		}
	}
	
	// GAMEPLAY METHODS
	/* Function to start a new round */
	public void startRound(String gameName)
	{
		Game currGame = serverGames.get(gameName);
		currGame.getGameDeck().shuffle(); // Shuffle cards
		String s = "ROUND " + currGame.getRound() + "\n";
		s += "Dealer is shuffling cards...";
		Message m = new Message(s);
		for(PlayerThread threads : currGame.getPlayers().values()) 
		{
			if(currGame.getRound() == 1) // set all players' chips to 500 initially
			{
				threads.setChipTotal(500);
			}
			threads.sendMessage(m);
		}
		
		// BETTING STAGE
		getPlayerBet(0, currGame.getPlayersInOrder().get(0), currGame);
	
		/* followed by... */
		// ASSIGN CARDS - happens after betting stage done
		// PLAYERS ADD CARDS
		// DEALER ADDS CARDS
		// DEAL CHIPS
		// END GAME OR RESTART
	}
	
	/* Function to get bets */
	public void getPlayerBet(int playerIndex, PlayerThread currPlayer, Game currGame)
	{
		// Send messages w/ whose turn it is
		String s = "It is " + currPlayer.getPlayerName() + "'s turn to make a bet.";
		Message m = new Message(s);
		broadcastToOthers(m, currPlayer, currGame);
		s = currPlayer.getPlayerName() + ", it is your turn to make a bet. Your chip total is " + currPlayer.getChipTotal();
		m = new Message(s);
		broadcastToPlayer(m, currPlayer, currGame);
		
		// Get bet from player
		GetBetMessage gbm = new GetBetMessage();
		currPlayer.sendMessage(gbm); // player's betAmount gets set
	}
	
	/* Function to receive bets */
	public void receivePlayerBet(int playerIndex, PlayerThread currPlayer, Game currGame)
	{
		// Send messages w/ bet made
		String s = currPlayer.getPlayerName() + " bet " + currPlayer.getBetAmount() + " chips";
		Message m = new Message(s);
		broadcastToOthers(m, currPlayer, currGame);
		s = "You bet " + currPlayer.getBetAmount() + " chips";
		m = new Message(s);
		broadcastToPlayer(m, currPlayer, currGame);
		
		if(playerIndex < (currGame.getNumPlayers() - 1)) // if not all players have betted yet
		{
			getPlayerBet(currGame.getPlayerIndex(currPlayer) + 1, currGame.getPlayersInOrder().get(playerIndex + 1), currGame);
		}
		else // all players have finished betting
		{
			assignCards(currGame); // move on to ASSIGNING CARDS
		}
	}
	
	/* Function to assign player cards */
	public void assignCards(Game currGame)
	{
		// dealer assigns 2 cards to each player
		for(PlayerThread threads : currGame.getPlayers().values()) // for all players in game
		{
			Card c1 = currGame.getGameDeck().deal(); // get random card
			Card c2 = currGame.getGameDeck().deal(); // get random card
			threads.addToPlayerHand(c1);
			threads.addToPlayerHand(c2);
		}
		
		// dealer assigns 2 cards to themself
		Card c1 = currGame.getGameDeck().deal(); // get random card
		Card c2 = currGame.getGameDeck().deal(); // get random card
		currGame.addToDealerHand(c1);
		currGame.addToDealerHand(c2);
		
		// print status of dealer
		String s = "----------------------------------------------------------------------------\n";
		s += "DEALER\n\n";
		s += "Cards: | ? | " + currGame.getDealerHand().get(1).toString() + " |\n";
		s += "----------------------------------------------------------------------------\n";
		
		// print status of all players
		for(PlayerThread threads : currGame.getPlayersInOrder().values()) // for all players in game
		{
			s += currGame.getPlayerState(threads.getPlayerName(), threads);
		}
		Message m = new Message(s);
		broadcastToAll(m, currGame);
		
		playersAddCards(0, currGame.getPlayersInOrder().get(0), currGame); // let first player add cards
	}
	
	// PLAYERS ADD CARDS
	public void playersAddCards(int playerIndex, PlayerThread currPlayer, Game currGame)
	{
		// Send messages w/ whose turn it is
		String s = "It is " + currPlayer.getPlayerName() + "'s turn to add cards to their hand.";
		Message m = new Message(s);
		broadcastToOthers(m, currPlayer, currGame);
		s = currPlayer.getPlayerName() + ", it is your turn to add cards to your hand.";
		m = new Message(s);
		broadcastToPlayer(m, currPlayer, currGame);
		
		askStayOrHit(playerIndex, currPlayer, currGame);
	}
	
	public void askStayOrHit(int playerIndex, PlayerThread currPlayer, Game currGame)
	{
		// Get choice from player
		GetStayOrHitMessage gsohm = new GetStayOrHitMessage();
		currPlayer.sendMessage(gsohm); // send message to player asking stay or hit
	}
	
	public void receiveStayOrHit(int playerIndex, PlayerThread currPlayer, Game currGame, String choice)
	{		
		boolean nextPlayer = false;
		
		if(choice.equals("1") || choice.equals("stay"))
		{
			// Send message to all players with player's choice
			String s = "You stayed.";
			Message m = new Message(s);
			broadcastToPlayer(m, currPlayer, currGame);
			s = currPlayer.getPlayerName() + " stayed.";
			m = new Message(s);
			broadcastToOthers(m, currPlayer, currGame);
			
			nextPlayer = true;
		}
		else if(choice.equals("2") || choice.equals("hit"))
		{
			Card c = currGame.getGameDeck().deal();
			currPlayer.addToPlayerHand(c);
			
			// Send message to all players with player's choice
			String s = "You hit. You were dealt the " + c.toString();
			Message m = new Message(s);
			broadcastToPlayer(m, currPlayer, currGame);
			s = currPlayer.getPlayerName() + " hit. They were dealt the " + c.toString();
			m = new Message(s);
			broadcastToOthers(m, currPlayer, currGame);
			
			if(currPlayer.busted())
			{
				s = "You busted! You lose " + currPlayer.getBetAmount() + " chips";
				m = new Message(s);
				broadcastToPlayer(m, currPlayer, currGame);
				s = currPlayer.getPlayerName() + " busted! They lose " + currPlayer.getBetAmount() + " chips";
				m = new Message(s);
				broadcastToOthers(m, currPlayer, currGame);
				
				nextPlayer = true;
			}
			else
			{
				askStayOrHit(playerIndex, currPlayer, currGame);
			}
		}
		
		if(nextPlayer)
		{
			// Print player's status to all players
			String s = currGame.getPlayerState(currPlayer.getPlayerName(), currPlayer);
			Message m = new Message(s);
			broadcastToAll(m, currGame);
			
			if(playerIndex < (currGame.getNumPlayers() - 1)) // if not all players have added cards yet
			{
				playersAddCards(playerIndex + 1, currGame.getPlayersInOrder().get(playerIndex + 1), currGame);
			}
			else // all players have finished adding cards
			{
				// set players' final ace values
				for(PlayerThread threads : currGame.getPlayers().values()) 
				{
					if(threads.hasAce())
					{
						if(threads.getCardSum() + 10 <= 21) // if Ace valued at 11 doesn't cause bust
						{
							threads.setCardSum(threads.getCardSum() + 10); // then value Ace at 11
						}
					}
				}
				dealerAddsCards(currGame); // move on to DEALER ADDS CARDS
			}
		}
	}
	
	// DEALER ADDS CARDS
	public void dealerAddsCards(Game currGame)
	{
		String s = "It is now time for the dealer to play.";
		Message m = new Message(s);
		broadcastToAll(m, currGame);
		
		int dealerHits = 0;
		String dealerCards = "";
		
		while(currGame.getDealerCardSum() < 17) // hit until dealer's sum is >= 17
		{
			if(currGame.isDealerHasAce()) // if dealer has Ace
			{
				if(currGame.getDealerCardSum() + 10 >= 17) // if dealer’s total + 11 >= 17, value Ace at 11 and ‘stay’
				{
					currGame.setDealerCardSum(currGame.getDealerCardSum() + 10); // set dealer's sum to Ace w/ value 11
					break;
				}
				// else if dealer’s total + 11 > 21 (‘bust’), value Ace at 1
			}
			Card c = currGame.getGameDeck().deal();
			currGame.addToDealerHand(c);
			if(dealerHits > 0)
			{
				dealerCards += ", ";
			}
			dealerCards += c.toString();
			dealerHits++;
		}	
		
		s = "The dealer hit " + dealerHits + " time(s).";
		if(dealerHits > 0)
		{
			s += " They were dealt: " + dealerCards;
		}
		m = new Message(s);
		broadcastToAll(m, currGame);
		
		// Print dealer's status to all players
		String ds = "----------------------------------------------------------------------------\n";
		ds += "DEALER\n\n";
		ds += "Status: " + currGame.getDealerStatus();
		ds += "\nCards: | ";
		for(int i = 0; i < currGame.getDealerHand().size(); i++)
		{
			ds += currGame.getDealerHand().get(i).toString() + " | ";
		}
		ds += "\n----------------------------------------------------------------------------\n";
		
		Message dm = new Message(ds);
		broadcastToAll(dm, currGame);
		
		// move on to DEAL CHIPS
		dealOutChips(0, currGame.getCreatorThread(), currGame); // start with first player
	}
	
	// DEAL OUT CHIPS ACCORDINGLY TO PLAYERS
	public void dealOutChips(int playerIndex, PlayerThread currPlayer, Game currGame)
	{		
		String sToPlayer;
		String sToOthers;
		
		// If player was 'busted'
		if(currPlayer.busted())
		{
			currPlayer.setChipTotal(currPlayer.getChipTotal() - currPlayer.getBetAmount()); // loses amount bet
			
			sToPlayer = "You busted. " + currPlayer.getBetAmount() + " chips were deducted from your total";
			sToOthers = currPlayer.getPlayerName() + " busted. " + currPlayer.getBetAmount() + " chips were deducted from " + currPlayer.getPlayerName() + "'s total";
		}
		// If player has 'blackjack' and dealer does not
		else if(currPlayer.blackJack() && !currGame.isDealerBlackJack())
		{
			currPlayer.setChipTotal(currPlayer.getChipTotal() + (2 * currPlayer.getBetAmount()) ); // wins twice of amount bet
			
			sToPlayer = "You had blackjack and the dealer did not. " + (2*currPlayer.getBetAmount()) + " chips were added to your total";
			sToOthers = currPlayer.getPlayerName() + " had blackjack and the dealer did not. " + (2*currPlayer.getBetAmount()) + " chips were added to " + currPlayer.getPlayerName() + "'s total";
		}
		// If both player and dealer have 'blackjack'
		else if(currPlayer.blackJack() && currGame.isDealerBlackJack())
		{
			// Player neither gains nor loses chips
			sToPlayer = "You had blackjack and so did the dealer. Your chip total remains the same";
			sToOthers = currPlayer.getPlayerName() + " had blackjack and so did the dealer. " + currPlayer.getPlayerName() + "'s chip total remains the same";
		}
		// If player hasn't busted and their card sum > dealer's card sum
		else if(currPlayer.getCardSum() > currGame.getDealerCardSum())
		{
			currPlayer.setChipTotal(currPlayer.getChipTotal() + currPlayer.getBetAmount() ); // wins amount bet
			
			sToPlayer = "You had a sum greater than the dealer's. " + currPlayer.getBetAmount() + " chips were added to your total";
			sToOthers = currPlayer.getPlayerName() + " had a sum greater than the dealer's. " + currPlayer.getBetAmount() + " chips were added to " + currPlayer.getPlayerName() + "'s total";
		}
		// If player hasn't busted but dealer has busted
		else if(currGame.isDealerBusted())
		{
			currPlayer.setChipTotal(currPlayer.getChipTotal() + currPlayer.getBetAmount() ); // wins amount bet
			
			sToPlayer = "You didn't bust but dealer did. " + currPlayer.getBetAmount() + " chips were added to your total";
			sToOthers = currPlayer.getPlayerName() + " didn't bust but dealer did. " + currPlayer.getBetAmount() + " chips were added to " + currPlayer.getPlayerName() + "'s total";
		}
		// If player's card sum < dealer's card sum
		else if(currPlayer.getCardSum() < currGame.getDealerCardSum())
		{
			currPlayer.setChipTotal(currPlayer.getChipTotal() - currPlayer.getBetAmount()); // loses amount bet
			
			sToPlayer = "You had a sum less than the dealer's. " + currPlayer.getBetAmount() + " chips were deducted from your total";
			sToOthers = currPlayer.getPlayerName() + " had a sum less than the dealer's. " + currPlayer.getBetAmount() + " chips were deducted from " + currPlayer.getPlayerName() + "'s total";
		}
		// If player's card sum = dealer's card sum
		else if(currPlayer.getCardSum() == currGame.getDealerCardSum())
		{
			// Player neither gains nor loses chips
			
			sToPlayer = "You had a sum equal to the dealer's. Your chip total remains the same";
			sToOthers = currPlayer.getPlayerName() + " had a sum equal to the dealer's. Their chip total remains the same";
		}
		else
		{
			sToPlayer = sToOthers = "UNKNOWN CASE OCCURRED";
		}
		
		// Send messages
		Message mToPlayer = new Message(sToPlayer);
		broadcastToPlayer(mToPlayer, currPlayer, currGame);
		Message mToOthers = new Message(sToOthers);
		broadcastToOthers(mToOthers, currPlayer, currGame);
		
		endOfRound(playerIndex, currPlayer, currGame);
	}
	
	public void endOfRound(int playerIndex, PlayerThread currPlayer, Game currGame)
	{
		// Reset player values		
		currPlayer.resetValues();
		
		if(playerIndex < (currGame.getNumPlayers() - 1)) // If not all players have been dealt chips yet
		{
			dealOutChips(playerIndex + 1, currGame.getPlayersInOrder().get(playerIndex + 1), currGame);
		}
		else // all players have been dealt chips
		{
			boolean endGame = false;
			PlayerThread loser = null;
			
			// check if end game or new round
			for(PlayerThread threads : currGame.getPlayers().values()) 
			{
				if(threads.getChipTotal() <= 0) // if any player runs out of chips, end game
				{
					loser = threads;
					endGame = true;
				}
			}
			
			if(endGame)
			{
				endGame(currGame, loser);
			}
			else
			{
				// If all players still have chips, start new round
				currGame.incrementRound();
				
				// Reset dealer values
				currGame.resetDealerValues();
				
				startRound(currGame.getGameName());
			}
		}
	}
	
	// END GAME
	public void endGame(Game currGame, PlayerThread loser)
	{
		PlayerThread winner = null;
		String loserName = loser.getPlayerName();
		int maxChips = 0;

		for(PlayerThread threads : currGame.getPlayers().values()) 
		{
			if(threads.getChipTotal() >= maxChips)
			{
				maxChips = threads.getChipTotal();
				winner = threads;
			}
		}
		
		String s = "\nEND OF GAME:";
		if(maxChips == 0) // if all players have 0 chips left, dealer wins
		{
			s += "\nThe winner of this game is the dealer.";
		}
		else
		{
			s += "\nThe winner of this game is " + winner.getPlayerName() + " with " + winner.getChipTotal() + " chips.";
		}
		s += "\nThe loser of this game is " + loserName + " with " + loser.getChipTotal() + " chips.";
		Message m = new Message(s);
		broadcastToAll(m, currGame);
		
		// terminate client program
		EndGameMessage egm = new EndGameMessage();
		sendEndGameMessages(egm, currGame);
	}
	
	
	// MAIN METHOD
	public static void main(String [] args) 
	{
		int portNumber = 0;
		boolean validPort = false;
		
		System.out.println("Welcome to the Black Jack Server!");
		
		while(!validPort)
		{
			System.out.println("Please enter a port");
			Scanner in = new Scanner(System.in);
			
			if(in.hasNextInt())
			{
				portNumber = in.nextInt();
				// Port number must be an unsigned short from 1025 - 65535
				if(portNumber < 1025 || portNumber > 65535)
				{
					System.out.println("Invalid port number.");
					continue;
				}
				try
				{
					GameServer cr = new GameServer(portNumber);
					System.out.println("Successfully started the Black Jack server on port " + portNumber);
					validPort = true;
				} catch (IOException ioe) {
					System.out.println("ioe in GameServer: " + ioe.getMessage());
					continue;
				}
			}
			else
			{
				System.out.println("Invalid port number.");
				continue;
			}
			
			in.close();
		}
	}
}