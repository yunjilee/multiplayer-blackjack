package gameMessages;

import java.io.Serializable;

public class ReturnStayOrHitMessage implements Serializable 
{
	public static final long serialVersionUID = 1;
	private String choice;
	
	public ReturnStayOrHitMessage(String choice)
	{
		this.choice = choice;
	}
	
	public String getChoice()
	{
		return choice;
	}
}
