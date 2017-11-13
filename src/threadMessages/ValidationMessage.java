package threadMessages;

import java.io.Serializable;

public class ValidationMessage implements Serializable 
{
	public static final long serialVersionUID = 1;
	private String message;
	private int messageType; // 1: New Game Name 2: Ongoing Game Name 3: User Name 
	
	public ValidationMessage(String message, int messageType) 
	{
		this.message = message;
		this.messageType = messageType;
	}
	
	public String getMessage() {
		return message;
	}
	public int getMessageType() {
		return messageType;
	}
}
