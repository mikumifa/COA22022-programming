package cpu.nbcdu;

import util.DataType;
import util.Transformer;

import java.util.Collections;
import java.util.HashMap;

public class NBCDU {
    public String postiveNBCDToString(String operand) {
        StringBuilder ans = new StringBuilder();
        operand = operand.substring(4);
        for (int i = 0; i < operand.length() && i < 28; i += 4) {
            ans.append(Integer.valueOf(operand.substring(i, i + 4), 2));
        }
        return ans.toString();
    }

    public String NBCD_adder(String op1, String op2, String carry) {
        StringBuilder builder = new StringBuilder();
        int length = op1.length();
        int bitNum = length / 4;
        int c = Integer.valueOf(carry, 2);
        for (int num = bitNum - 1; num >= 0; num--) {
            int beginPos = num * 4;
            int nowAns = Integer.valueOf(op1.substring(beginPos, beginPos + 4), 2) + Integer.valueOf(op2.substring(beginPos, beginPos + 4), 2) + c;
            builder.insert(0, Transformer.intToBinary(String.valueOf(nowAns % 10)).substring(32 - 4));
            c = nowAns / 10;
        }
        return c + builder.toString();
    }

    /**
     * @param src  A 32-bits NBCD String
     * @param dest A 32-bits NBCD String
     * @return dest + src
     */
    DataType add(DataType src, DataType dest) {
        // TODO
        String op2 = src.toString();
        String op1 = dest.toString();
        if (op1.startsWith("1100") && op2.startsWith("1101")) {
            return sub(new DataType("1100" + op2.substring(4)), dest);
        } else if (op1.startsWith("1101") && op2.startsWith("1100")) {
            return sub(new DataType("1100" + op1.substring(4)), src);
        } else if (op1.startsWith("1101") && op2.startsWith("1101"))
            return new DataType("1101" + add(new DataType("1100" + op1.substring(4)), new DataType("1100" + op2.substring(4))).toString().substring(4));
        else {
            return new DataType("1100" + NBCD_adder(op1.substring(4), op2.substring(4), "0000").substring(1));
        }
    }

    public String getNBCDNeg(String op) {
        StringBuilder builder = new StringBuilder();
        int length = op.length();
        int bitNum = length / 4;
        for (int num = bitNum - 1; num >= 0; num--) {
            int beginPos = num * 4;
            int nowAns = 9 - Integer.valueOf(op.substring(beginPos, beginPos + 4), 2);
            builder.insert(0, Transformer.intToBinary(String.valueOf(nowAns)).substring(32 - 4));
        }
        return builder.toString();
    }

    /***
     *
     * @param src A 32-bits NBCD String
     * @param dest A 32-bits NBCD String
     * @return dest - src
     */

    DataType sub(DataType src, DataType dest) {
        String op2 = src.toString();
        String op1 = dest.toString();
        //转换可能会出错，要多实验几次
        if (op1.startsWith("1100") && op2.startsWith("1101")) {
            return add(new DataType("1100" + op2.substring(4)), dest);
        } else if (op1.startsWith("1101") && op2.startsWith("1100")) {
            return add(new DataType("1101" + op2.substring(4)), dest);
        } else if (op1.startsWith("1101") && op2.startsWith("1101")) {
            return sub(new DataType("1100" + op1.substring(4)), new DataType("1100" + op2.substring(4)));
        } else {

            String ans;
            String nbcdAdder = NBCD_adder(op1.substring(4), getNBCDNeg(op2.substring(4)), "0001");
            if (nbcdAdder.charAt(0) == '0')
                //首先要注意取反负数，写一个带carry的加法，和一个取反的函数，最后，先取反后加1来实现操作。
                ans = "1101" + NBCD_adder(String.join("", Collections.nCopies(28,"0")), getNBCDNeg(nbcdAdder.substring(1)), "0001").substring(1);
            else ans = "1100" + nbcdAdder.substring(1);
            return new DataType(ans);

        }
    }

}
