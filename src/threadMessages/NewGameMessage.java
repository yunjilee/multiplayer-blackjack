package threadMessages;

import java.io.Serializable;

public class NewGameMessage implements Serializable 
{
	public static final long serialVersionUID = 1;
	private String gameName;
	private int numPlayers;
	private String playerName;
		
	public NewGameMessage(int numPlayers, String gameName, String playerName) 
	{
		this.numPlayers = numPlayers;
		this.gameName = gameName;
		this.playerName = playerName;
	}
	
	public String getGameName() {
		return gameName;
	}

	public int getNumPlayers() {
		return numPlayers;
	}

	public String getPlayerName() {
		return playerName;
	}
}
