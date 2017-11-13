package threadMessages;

import java.io.Serializable;

public class Message implements Serializable 
{
	public static final long serialVersionUID = 1;
	private String message;
	
	public Message(String message)
	{
		this.message = message;
	}
	
	public String getMessage()
	{
		return message;
	}
}
