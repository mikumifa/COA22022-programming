package cpu.alu;

import util.DataType;

/**
 * Arithmetic Logic Unit
 * ALU封装类
 */
public class ALU {

    /**
     * 返回两个二进制整数的和
     * dest + src
     *
     * @param src  32-bits
     * @param dest 32-bits
     * @return 32-bits
     */
    public DataType add(DataType src, DataType dest) {
        final int bits=32;
        char[] srcStr=src.toString().toCharArray();
        char[] destStr=dest.toString().toCharArray();
        char[] ansCharArray=new char[bits];
        int cin=0;
        for(int i=bits-1;i>=0;i--){
            int t=srcStr[i]+destStr[i]+cin-'0'-'0';
            ansCharArray[i]=(char) (t%2+'0');
            cin=t/2;
        }
        return new DataType(String.valueOf(ansCharArray));
    }

    /**
     * 返回两个二进制整数的差
     * dest - src
     *
     * @param src  32-bits
     * @param dest 32-bits
     * @return 32-bits
     */
    public DataType sub(DataType src, DataType dest) {
        // TODO
        final int bits=32;
        char[] srcStr=src.toString().toCharArray();
        for(int i=bits-1;i>=0;i--){
            if(srcStr[i]=='0')
                srcStr[i]='1';
            else srcStr[i]='0';
        }
        return add(add(dest,new DataType(String.valueOf(srcStr))),new DataType("00000000000000000000000000000001"));
    }

}
