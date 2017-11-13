package objects;

public class Card 
{
	private int face; // the card's face value, between 1 and 13 inclusive
	private int suit; // the card's suit; one of the class constants

	// class constants for the suits
	public static final int HEARTS = 1;
	public static final int DIAMONDS = 2;
	public static final int CLUBS = 3;
	public static final int SPADES = 4;

	// Construct Card object with given face and suit values, assume valid inputs
	public Card(int face, int suit) 
	{
		this.face = face;
		this.suit = suit;
	}

	//Returns the face value of the card (between 1 and 13, inclusive)
	public int getFace() 
	{
		return face;
	}

	// Returns the suit of the card (HEARTS, DIAMONDS, CLUBS, or SPADES)
	public int getSuit() 
	{
		return suit;
	}

	// Returns a String representation of a Card object
	public String toString() 
	{
		String s = "";
		switch (face) {
			case 1:	s += "ACE";
					break;
			case 2: s += "TWO";
					break;
			case 3: s += "THREE";
					break;
			case 4: s += "FOUR";
					break;
			case 5: s += "FIVE";
					break;
			case 6: s += "SIX";
					break;
			case 7: s += "SEVEN";
					break;
			case 8: s += "EIGHT";
					break;
			case 9: s += "NINE";
					break;
			case 10: s += "TEN";
					break;
			case 11:	s += "JACK";
					break;
			case 12:	s += "QUEEN";
					break;
			case 13:	s += "KING";
					break;
		}

		switch (suit) {
			case HEARTS:	s += " of HEARTS";
					break;
			case DIAMONDS:	s += " of DIAMONDS";
					break;
			case CLUBS:	s += " of CLUBS";
					break;
			case SPADES:	s += " of SPADES";
					break;
		}

		return s;
	}

}