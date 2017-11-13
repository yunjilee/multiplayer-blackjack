package threadMessages;

import java.io.Serializable;

public class ResponseMessage implements Serializable 
{
	public static final long serialVersionUID = 1;
	boolean isValid;
	String joinedUser;
	
	// Returns validation status
	public ResponseMessage(boolean isValid) {
		this.isValid = isValid;
	}
	
	// Returns name of user that has joined game
	public ResponseMessage(String joinedUser) {
		this.joinedUser = joinedUser;
	}
	
	public boolean getIsValid() {
		return isValid;
	}
	
	public String getJoinedUser() {
		return joinedUser;
	}
}
