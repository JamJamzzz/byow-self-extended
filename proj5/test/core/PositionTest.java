package core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PositionTest {

    @Test
    public void equalCoordinatesAreEqual() {
        assertEquals(new Position(3, 5), new Position(3, 5));
    }

    @Test
    public void differentCoordinatesAreNotEqual() {
        assertNotEquals(new Position(3, 5), new Position(5, 3));
    }

    @Test
    public void equalPositionsHaveEqualHashCodes() {
        assertEquals(new Position(7, 11).hashCode(), new Position(7, 11).hashCode());
    }

    @Test
    public void notEqualToOtherTypes() {
        assertNotEquals(new Position(1, 1), "not a position");
    }
}
