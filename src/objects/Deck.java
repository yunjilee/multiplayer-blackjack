package objects;
import java.util.Random;

public class Deck 
{
	private Card[] cards; // array holding all 52 cards
	private int cardsInDeck; // the current number of cards in the deck

	public static final int DECK_SIZE = 52; // size of standard deck of cards

	// Construct new deck with 52 cards
	public Deck() 
	{
		cards = new Card[DECK_SIZE];
		cardsInDeck = DECK_SIZE;
		for (int i = 0; i < 13; i++) 
		{
			cards[i] = new Card(i + 1, Card.HEARTS);
			cards[i + 13] = new Card(i + 1, Card.DIAMONDS);
			cards[i + 26] = new Card(i + 1, Card.CLUBS);
			cards[i + 39] = new Card(i + 1, Card.SPADES);
		}
	}

	// Returns the number of cards in the deck.
	public int cardsInDeck() { return cardsInDeck; }

	// Deal one card from deck, dec. number of cards in deck by one
	public Card deal() 
	{
		if (cardsInDeck == 0) // check for an empty deck
			return null;

		cardsInDeck--;
		return cards[cardsInDeck];
	}

	// Shuffles (randomly reorders order of) deck
	public void shuffle() 
	{
		int newI;
		Card temp;
		Random randIndex = new Random();

		for (int i = 0; i < cardsInDeck; i++) {

			// pick a random index between 0 and cardsInDeck - 1
			newI = randIndex.nextInt(cardsInDeck);

			// swap cards[i] and cards[newI]
			temp = cards[i];
			cards[i] = cards[newI];
			cards[newI] = temp;
		}
	}

	// Reset deck
	public void reset() 
	{
		cardsInDeck = DECK_SIZE;
	}

	// Return String representation of current Deck object
	public String toString() 
	{
		// check for an empty deck
		if (cardsInDeck == 0)
			return "<empty>";

		String s = cards[0].toString() + ' ';
		for (int i = 1; i < cardsInDeck; i++) 
		{
			if (i % 13 == 0)
				s += '\n';
			s += cards[i].toString() + ' ';
		}
		return s;
	}

}