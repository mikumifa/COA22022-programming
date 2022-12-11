package cpu.fpu;

import util.DataType;
import util.IEEE754Float;
import util.Transformer;

import java.util.Arrays;
import java.util.Collections;

/**
 * floating point unit
 * 执行浮点运算的抽象单元
 * 浮点数精度：使用3位保护位进行计算
 */
public class FPU {

    private final String[][] mulCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.N_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.P_ZERO},
            {IEEE754Float.P_ZERO, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.P_ZERO, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.N_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.N_ZERO, IEEE754Float.NaN}
    };

    private final String[][] divCorner = new String[][]{
            {IEEE754Float.P_ZERO, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.N_ZERO, IEEE754Float.NaN},
            {IEEE754Float.P_ZERO, IEEE754Float.N_ZERO, IEEE754Float.NaN},
            {IEEE754Float.N_ZERO, IEEE754Float.P_ZERO, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.P_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.P_INF, IEEE754Float.N_INF, IEEE754Float.NaN},
            {IEEE754Float.N_INF, IEEE754Float.P_INF, IEEE754Float.NaN},
    };

    //返回op1.length()+1位，最前面以为是溢出位,无符号的简单的二进制的加法
    public char getCarry(char x, char y, char c) {
        return (char) (((x - '0') & (y - '0') | (x - '0') & (c - '0') | (c - '0') & (y - '0')) + '0');
    }

    public char getBit(char x, char y, char c) {
        return ((char) ((x - '0') ^ (y - '0') ^ (c - '0') + '0'));
    }

    public String carry_adder(String op1, String op2, char c) {
        char carry = c;
        char bit = '0';
        int length = op1.length();
        StringBuilder ansBuilder = new StringBuilder();
        for (int i = length - 1; i >= 0; i--) {
            bit = getBit(op1.charAt(i), op2.charAt(i), carry);
            carry = getCarry(op1.charAt(i), op2.charAt(i), carry);
            ansBuilder.append(bit);
        }
        ansBuilder.append(carry);
        return ansBuilder.reverse().toString();
    }

    public String sigMul(String sig1, String sig2) {
        int length = sig1.length();
        String product=String.join("",Collections.nCopies(length,"0"))+sig2;

        for (int i = length-1; i >=0; i--) {
            char now = product.charAt(2*length-1);
            char carry='0';
            if(now=='1'){
                String carryAdder = carry_adder(product.substring(0, length), sig1, '0');
                carry=carryAdder.charAt(0);
                product=carryAdder.substring(1)+product.substring(length);
            }
            product=carry+product.substring(0,2*length-1);
        }
        return product;
    }
    public String nagetify(String val){
        StringBuilder ansBuilder = new StringBuilder();
        for (int i = 0; i < val.length(); i++) {
            ansBuilder.append(val.charAt(i)=='1'?'0':'1');
        }
        return ansBuilder.toString();
    }
    public String sigDiv(String sig1, String sig2) {
        int length = sig1.length();
        String negSig2 = nagetify(sig2);
        String product=sig1+String.join("",Collections.nCopies(length,"0"));
        for (int i = length-1; i >=0; i--) {
            String carryAdder = carry_adder(product.substring(0, length), negSig2, '1').substring(1);
            if(carryAdder.charAt(0)=='0'){
                product=carryAdder.substring(1)+product.substring(length)+"1";
            }else{
                product=product.substring(1)+"0";
            }
        }
        return product.substring(length);
    }
    /**
     * compute the float mul of dest * src
     */
    public DataType mul(DataType src, DataType dest) {
        String op2 = src.toString();
        String op1 = dest.toString();
        String cornerCheck = cornerCheck(mulCorner, op1, op2);
        if (null != cornerCheck) return new DataType(cornerCheck);
        if(op1.matches(IEEE754Float.NaN_Regular)||op2.matches(IEEE754Float.NaN_Regular))
            return new DataType( IEEE754Float.NaN);
        int exp1 = Integer.valueOf(op1.substring(1, 9), 2);
        int exp2 = Integer.valueOf(op2.substring(1, 9), 2);

        //保护位可以最后再去加上去
        String sig1 = op1.substring(9);
        String sig2 = op2.substring(9);
        //设置这个值
        if(exp1==0) {
            sig1 = "0" + sig1;
            exp1++;
        }else sig1="1"+sig1;
        if(exp2==0){
            sig2="0"+sig2;
            exp2++;
        }else sig2="1"+sig2;
        //对于乘法来说，一开始不要取舍，对于27*2位都需要，最后才回去
        String sign = ((op1.charAt(0) - '0') ^ (op2.charAt(0) - '0')) + "";
        if(exp1==255||exp2==255) return new DataType(sign+"1111111100000000000000000000000");
        int bias=127;
        int exp=exp1+exp2-bias;
        String sigMul = sigMul(sig1, sig2);
        exp++;
        //做规格化
        while (sigMul.charAt(0)=='0'&&exp>0){
            exp--;
            sigMul= sigMul.substring(1) + "0";
        }
        while (!sigMul.startsWith("000000000000000000000000")&&exp<0){
            exp++;
            sigMul=rightShift(sigMul,1);
        }
        if(exp>=255)
            return new DataType(sign+"1111111100000000000000000000000");
        if(exp==0){
            sigMul=rightShift(sigMul,1);
        }
        if(exp<0)
            return new DataType(sign+"0000000000000000000000000000000");
        String expStr=Transformer.intToBinary(String.valueOf(exp)).substring(32-8);
        String round = round(sign.charAt(0), expStr, sigMul);
        return new DataType(round);

    }

    /**
     * compute the float mul of dest / src
     */
    public DataType div(DataType src, DataType dest) {
        String op2 = src.toString();
        String op1 = dest.toString();
        if (IEEE754Float.P_ZERO.equals(op2) || IEEE754Float.N_ZERO.equals(op2)) {
            if ((!IEEE754Float.P_ZERO.equals(op1)) && (!IEEE754Float.N_ZERO.equals(op2))) {
                throw new ArithmeticException();
            }
        }
        String cornerCheck = cornerCheck(divCorner, op1, op2);
        if (null != cornerCheck) return new DataType(cornerCheck);
        if(op1.matches(IEEE754Float.NaN_Regular)||op2.matches(IEEE754Float.NaN_Regular))
            return new DataType( IEEE754Float.NaN);
        int exp1 = Integer.valueOf(op1.substring(1, 9), 2);
        int exp2 = Integer.valueOf(op2.substring(1, 9), 2);

        //保护位可以最后再去加上去
        String sig1 = "1"+op1.substring(9)+"000";
        String sig2 = "1"+op2.substring(9)+"000";
        //对于乘法来说，一开始不要取舍，对于27*2位都需要，最后才回去
        String sign = ((op1.charAt(0) - '0') ^ (op2.charAt(0) - '0')) + "";
        int bias=127;
        int exp=exp1-exp2+bias;
        String sigMul = sigDiv(sig1, sig2);
        //做规格化
        while (sigMul.charAt(0)=='0'&&exp>0){
            exp--;
            sigMul= sigMul.substring(1) + "0";
        }
        while (!sigMul.startsWith("000000000000000000000000")&&exp<0){
            exp++;
            sigMul=rightShift(sigMul,1);
        }
        if(exp>=255)
            return new DataType(sign+"1111111100000000000000000000000");
        if(exp==0){
            sigMul=rightShift(sigMul,1);
        }
        if(exp<0)
            return new DataType(sign+"0000000000000000000000000000000");
        String expStr=Transformer.intToBinary(String.valueOf(exp)).substring(32-8);
        String round = round(sign.charAt(0), expStr, sigMul);
        return new DataType(round);
    }

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
        if (grs > 4 || (grs == 4 && sig.endsWith("1"))) {
            sig = oneAdder(sig);
            if (sig.charAt(0) == '1') {
                exp = oneAdder(exp).substring(1);
                sig = sig.substring(1);
            }
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
        StringBuffer temp = new StringBuffer(operand);
        temp = temp.reverse();
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

}
