package util;

import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.assertEquals;

public class TransformerTest {

    @Test
    public void intToBinaryTest1() {
        System.out.println(Transformer.intToBinary("2029"));
        System.out.println(Transformer.floatToBinary("2029"));
    }

    @Test
    public void binaryToIntTest1() {
        assertEquals("-1", Transformer.binaryToInt("11111111111111111111111111111111"));
    }

    @Test
    public void decimalToNBCDTest1() {
        assertEquals("11000000000000000000000000010000", Transformer.decimalToNBCD("10"));
    }

    @Test
    public void NBCDToDecimalTest1() {
        assertEquals("10", Transformer.NBCDToDecimal("11000000000000000000000000010000"));
    }

    @Test
    public void floatToBinaryTest1() {
        while (true) {
            Random random = new Random();
            float y = random.nextFloat();
            String x = TrueTransformer.floatToBinary(String.valueOf(y));
            String he = TrueTransformer.binaryToFloat(String.valueOf(x));
            String my = Transformer.binaryToFloat(String.valueOf(x));
            assertEquals(String.format("x:%s,he:%s,my:%s", x, he, my), he, my);
        }
    }

    @Test
    public void floatToBinaryTest2() {
        assertEquals("+Inf", Transformer.floatToBinary(String.valueOf(Math.pow(2,128)))); // 对于float来说溢出
    }

    @Test
    public void binaryToFloatTest1() {
        assertEquals(String.valueOf(85f/256f), Transformer.binaryToFloat("000010101010100000000000000000"));
    }

}
