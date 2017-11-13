package gameMessages;

import java.io.Serializable;

public class ReturnBetMessage implements Serializable 
{
	public static final long serialVersionUID = 1;
	private int bet;
	
	public ReturnBetMessage(int bet)
	{
		this.bet = bet;
	}
	
	public int getBet()
	{
		return bet;
	}
}
