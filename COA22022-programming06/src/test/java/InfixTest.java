import org.junit.Test;
import static org.junit.Assert.*;

public class InfixTest {
    @Test
    public void test1(){
        Infix inf = new Infix();
        String[] tokens = {"2", "*", "(", "24", "/", "(", "4", "+", "2", ")", ")"};
        assertEquals( 8.0, inf.infix(tokens),0);
    }
    @Test
    public void test2(){
        Infix inf = new Infix();
        String[] tokens = {"(", "5", "+", "9", ")", "/", "2", "*", "3", "/", "7","+","8"};
        assertEquals( 11.0, inf.infix(tokens),0);
    }
    @Test
    public void test3(){
        Infix inf = new Infix();
        String[] tokens = {"1", "/", "3", "+", "(", "5", "-", "6", ")", "*", "8","-","7"};
        assertEquals( -14.666666, inf.infix(tokens),0.000001);
    }

}