package cpu.fpu;

import util.DataType;
import util.IEEE754Float;
import util.Transformer;

import javax.print.DocFlavor;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * floating point unit
 * 执行浮点运算的抽象单元
 * 浮点数精度：使用3位保护位进行计算
 */
public class FPU {

    private final String[][] addCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.P_INF, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.P_INF, IEEE754Float.NaN}
    };

    private final String[][] subCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.P_INF, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.N_INF, IEEE754Float.NaN}
    };
    private String cornerCheck(String[][] cornerMatrix, String oprA, String oprB) {
        for (String[] matrix : cornerMatrix) {
            if (oprA.equals(matrix[0]) &&
                    oprB.equals(matrix[1])) {
                return matrix[2];
            }
        }
        return null;
    }
    /**
     * compute the float add of (dest + src)
     */
    public DataType add(DataType src, DataType dest) {
        String op1 = dest.toString();
        String op2 = src.toString();
        //第一种情况就是都是Nan
        if (op1.matches(IEEE754Float.NaN_Regular) || op2.matches(IEEE754Float.NaN_Regular)) {
            return new DataType(IEEE754Float.NaN);
        }
        //然后就是使用cornerCheck进行检查
        String cornerCondition = cornerCheck(addCorner, op1, op2);
        if (null != cornerCondition) return new DataType(cornerCondition);
        String exp1 = op1.substring(1, 9);
        String exp2 = op2.substring(1, 9);
        int expVal_1 = Integer.valueOf(exp1, 2);
        int expVal_2 = Integer.valueOf(exp2, 2);
        if (expVal_1 == 255)
            return dest;
        if (expVal_2 == 255)
            return src;
        if (op1.substring(1).equals(IEEE754Float.P_ZERO.substring(1)))
            return src;
        if (op2.substring(1).equals(IEEE754Float.P_ZERO.substring(1)))
            return dest;
        String sig1 = "";
        String sig2 = "";
        if (expVal_1 == 0) {
            expVal_1++;
            sig1 += "0";
        } else sig1 += "1";
        if (expVal_2 == 0) {
            expVal_2++;
            sig2 += "0";
        } else sig2 += "1";
        sig1 = sig1 + op1.substring(9) + "000";
        sig2 = sig2 + op2.substring(9) + "000";
        int expVal = Math.max(expVal_1, expVal_2);
        if (expVal_1 > expVal_2) {
            //pay attention to don't use the c++ thought
            sig2=rightShift(sig2, expVal - expVal_2);
        } else {
            sig1=rightShift(sig1, expVal - expVal_1);
        }
        String temp=sigAdd(sig1,sig2,op1.charAt(0) , op2.charAt(0));
        String sig=temp.substring(1);
        String sign=temp.substring(0,1);
        //下面是规格化的模板,显示++去除溢出位，然后左规格化和右边规格化，左规格化--。有规格化++，最后对于expVal来进行判断
        expVal++;//相当于temp左移了一下
        //左规格化
        while (sig.charAt(0)=='0'&&expVal>0){
            expVal--;
            sig=sig.substring(1)+'0';
        }
        while (!sig.startsWith("00000000000000000000000000")&&expVal<0){
            expVal++;
            sig=rightShift(sig,1);
        }
        if(expVal>=255)
            return new DataType(sign+"1111111100000000000000000000000");
        if(expVal==0)
            sig=rightShift(sig,1);
        if(expVal<0){

        }
        String exp=Transformer.intToBinary(String.valueOf(expVal)).substring(32-8);
        String round = round(sign.charAt(0), exp, sig);
        return new DataType(round);
    }


    /**
     * compute the float add of (dest - src)
     */
    public DataType sub(DataType src, DataType dest) {
        String s = src.toString();
        char sign;
        if (s.charAt(0) == '0')
            sign = '1';
        else sign = '0';
        //改一下src的符号即可
        DataType srcpro = new DataType(sign + s.substring(1));
        return add(srcpro, dest);
    }
    //左移加0

    /**
     * right shift a num without considering its sign using its string format
     *
     * @param operand to be moved
     * @param n       moving nums of bits
     * @return after moving
     */
    private String rightShift(String operand, int n) {
        StringBuilder result = new StringBuilder(operand);  //保证位数不变
        boolean sticky = false;
        for (int i = 0; i < n; i++) {
            sticky = sticky || result.toString().endsWith("1");
            result.insert(0, "0");
            result.deleteCharAt(result.length() - 1);
        }
        if (sticky) {
            result.replace(operand.length() - 1, operand.length(), "1");
        }
        return result.substring(0, operand.length());
    }

    /**
     * 对GRS保护位进行舍入
     *
     * @param sign    符号位
     * @param exp     阶码
     * @param sig_grs 带隐藏位和保护位的尾数
     * @return 舍入后的结果
     */
    private String round(char sign, String exp, String sig_grs) {
        int grs = Integer.parseInt(sig_grs.substring(24, 27), 2);
        if ((sig_grs.substring(27).contains("1")) && (grs % 2 == 0)) {
            grs++;
        }
        String sig = sig_grs.substring(0, 24); // 隐藏位+23位
        if (grs > 4) {
            sig = oneAdder(sig);
        } else if (grs == 4 && sig.endsWith("1")) {
            sig = oneAdder(sig);
        }

        if (Integer.parseInt(sig.substring(0, sig.length() - 23), 2) > 1) {
            sig = rightShift(sig, 1);
            exp = oneAdder(exp).substring(1);
        }
        if (exp.equals("11111111")) {
            return sign == '0' ? IEEE754Float.P_INF : IEEE754Float.N_INF;
        }

        return sign + exp + sig.substring(sig.length() - 23);
    }

    /**
     * add one to the operand
     *
     * @param operand the operand
     * @return result after adding, the first position means overflow (not equal to the carray to the next) and the remains means the result
     */
    private String oneAdder(String operand) {
        int len = operand.length();
        StringBuilder temp = new StringBuilder(operand);
        temp.reverse();
        int[] num = new int[len];
        for (int i = 0; i < len; i++) num[i] = temp.charAt(i) - '0';  //先转化为反转后对应的int数组
        int bit = 0x0;
        int carry = 0x1;
        char[] res = new char[len];
        for (int i = 0; i < len; i++) {
            bit = num[i] ^ carry;
            carry = num[i] & carry;
            res[i] = (char) ('0' + bit);  //显示转化为char
        }
        String result = new StringBuffer(new String(res)).reverse().toString();
        return "" + (result.charAt(0) == operand.charAt(0) ? '0' : '1') + result;  //注意有进位不等于溢出，溢出要另外判断
    }

    /**
     * 返回，符号位+溢出位+27位
     * @param sig1
     * @param sig2
     * @param sign1
     * @param sign2
     * @return
     */
    private String sigAdd(String sig1,String sig2,char sign1,char sign2){
        boolean sameSign=sign1==sign2;
        if(sig1.equals("000000000000000000000000000"))
            return sign2+sig2;
        if(sig2.equals("000000000000000000000000000"))
            return sign1+sig1;

        if(sameSign){
            int intSig1 = Integer.valueOf(sig1, 2);
            int intSig2 = Integer.valueOf(sig2, 2);
            String intToBinary = Transformer.intToBinary(String.valueOf(intSig1 + intSig2));
            String ansSig = intToBinary.substring(32-28);
            return sign1+ansSig;
        }else {
            int intSig1 = Integer.valueOf(sig1, 2);
            int intSig2 = Integer.valueOf(sig2, 2);
            //要考虑为0的情况，没有负0的情况
            if(sig1.equals(sig2))
                return "00000000000000000000000000000";
            String intToBinary = Transformer.intToBinary(String.valueOf(Math.abs(intSig1-intSig2)));
            String ansSig = intToBinary.substring(32-28);
            return intSig1>intSig2?(sign1+""+ansSig):(sign2+""+ansSig);
        }
    }

}
