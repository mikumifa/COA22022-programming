package cpu.alu;

import org.junit.Test;
import util.DataType;

import java.util.Random;
import util.Transformer;

import static org.junit.Assert.assertEquals;

public class ALUMulTest {

	private final ALU alu = new ALU();
	private DataType src;
	private DataType dest;
	private DataType result;

	@Test
	public void MulTest1() {
		Random random = new Random();
		float a=random.nextFloat()*100000;
		float b=random.nextFloat()*100000;
		src = new DataType(Transformer.floatToBinary(String.valueOf(a)));
		dest = new DataType(Transformer.floatToBinary(String.valueOf(b)));
		result = alu.mul(src, dest);
		assertEquals("a:"+a+"  b:"+b,Transformer.floatToBinary(String.valueOf(a*b)), Transformer.floatToBinary(result.toString()));
	}

}
