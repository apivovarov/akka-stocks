import org.junit.Test;
import utils.OldFakeStockQuote;

import java.util.Random;

import static org.fest.assertions.Assertions.assertThat;

public class OldFakeStockQuoteTest {

    @Test
    public void fakeStockPriceShouldBePlusOrMinusFivePercentOfTheOldPrice() {
        OldFakeStockQuote stockQuote = new OldFakeStockQuote();
        Double origPrice = new Random().nextDouble();
        Double newPrice = stockQuote.newPrice(origPrice);
        assertThat(newPrice).isGreaterThan(origPrice - (origPrice * 0.05));
        assertThat(newPrice).isLessThan(origPrice + (origPrice * 0.05));
    }

}
