package cpu.alu;

import org.junit.Test;
import util.DataType;
import util.Transformer;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class ALUDivTest {

    private final ALU alu = new ALU();
    private DataType src;
    private DataType dest;
    private DataType result;

    /**
     * 10 / 10 = 1 (0)
     */
    @Test
    public void DivTest1() {
        int x=5;
        int y=1;
        String xs=String.valueOf(x);
        String ys=String.valueOf(y);
        String reg=String.valueOf(x%y);
        String zs=String.valueOf(x/y);
        src = new DataType(Transformer.intToBinary(ys));
        dest = new DataType(Transformer.intToBinary(xs));
        result = alu.div(src, dest);
        String quotient = Transformer.intToBinary(zs);
        String remainder = Transformer.intToBinary(reg);
        assertEquals("X:"+xs+" y:"+ys, quotient, result.toString());
        assertEquals("X:"+xs+" y:"+ys,remainder, alu.remainderReg.toString());
    }

    /**
     * -8 / 2 = -4 (0)
     * 除法算法固有的bug
     */
    @Test
    public void DivSpecialTest() {

        Random random = new Random();
        while (true) {
            int x=random.nextInt();
            int y=random.nextInt();
            if(y==0)
                continue;
            String xs=String.valueOf(x);
            String ys=String.valueOf(y);
            String reg=String.valueOf(x%y);
            String zs=String.valueOf(x/y);
            src = new DataType(Transformer.intToBinary(ys));
            dest = new DataType(Transformer.intToBinary(xs));
            result = alu.div(src, dest);
            String quotient = Transformer.intToBinary(zs);
            String remainder = Transformer.intToBinary(reg);
            assertEquals("X:"+xs+" y:"+ys, quotient, result.toString());
            assertEquals("X:"+xs+" y:"+ys,remainder, alu.remainderReg.toString());
        }

    }

    /**
     * 0 / 0  除0异常
     */
    @Test
    public void DivExceptionTest1() {
        src = new DataType("00000100000000000000000000000000");
        dest = new DataType("00000111000000000000000000000000");
        System.out.println(result = alu.div(src, dest));
    }

}
