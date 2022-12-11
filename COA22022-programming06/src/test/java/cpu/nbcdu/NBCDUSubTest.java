package cpu.nbcdu;

import org.junit.Test;
import util.DataType;
import util.Transformer;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class NBCDUSubTest {

    private final NBCDU nbcdu = new NBCDU();
    private DataType src;
    private DataType dest;
    private DataType result;

    @Test
    public void SubTest1() {
        Random random = new Random();

        while (true){
            int a=random.nextInt(10)-5;
            int b=random.nextInt(10)-5;
            a= -5;
            b=4;
            src = new DataType(Transformer.getBCDString(b));
            dest = new DataType(Transformer.getBCDString(a));
            DataType ans=new DataType(Transformer.getBCDString(a-b));
            result = nbcdu.sub(src, dest);

            assertEquals("a= "+a+";\nb="+b+";",ans.toString(), result.toString());
        }
    }

}
