package cpu.alu;


import util.DataType;

import java.util.Arrays;

/**
 * Arithmetic Logic Unit
 * ALU封装类
 */
public class ALU {

    /**
     * 返回两个二进制整数的乘积(结果低位截取后32位)
     *
     * dest * src
     *在ALU类中实现实现整数的二进制乘法(要求使用布斯乘法实现)。
     *
     * 输入和输出均为32位二进制补码，计算结果直接截取低32位作为最终输出
     * @param src  32-bits
     * @param dest 32-bits
     * @return 32-bits
     */
    public char[] add(char[] srcStr, char[] destStr) {
        final int bits=32;
        char[] ansCharArray=new char[bits*2];
        int cin=0;
        for(int i=bits-1;i>=0;i--){
            int t=srcStr[i]+destStr[i]+cin-'0'-'0';
            ansCharArray[i]=(char) (t%2+'0');
            cin=t/2;
        }
        for(int i=bits;i<2*bits;i++){
            ansCharArray[i]=srcStr[i];
        }
        return ansCharArray;
    }
    public DataType negetifyDataType(DataType x){
        char[] xArray=x.toString().toCharArray();
        boolean isFirst=true;
        for(int i=31;i>=0;i--){
            if(isFirst==false){
                if(xArray[i]=='1')
                    xArray[i]='0';
                else xArray[i]='1';
            }
            if(isFirst==true&&xArray[i]=='1')
            {
                isFirst=false;
            }
     }

        return new DataType(String.valueOf(xArray));
    }
    public DataType mul(DataType src, DataType dest) {
        char[] srcArray=src.toString().toCharArray();
        char[] ansArray=new char[srcArray.length*2];
        for(int i=0;i<64;i++){
            ansArray[i]='0';
        }
        for(int i= 32;i<64;i++){
            ansArray[i]=srcArray[i- 32];//寄存器的初始化
        }
        char[] destNormal=dest.toString().toCharArray();
        char[] destNegetive=negetifyDataType(dest).toString().toCharArray();
        char before='0';
        String s;
        for(int i=0;i<32;i++){
            if(ansArray[ansArray.length-1]-before==-1) {
                ansArray=add(ansArray,destNormal);
            } else if(ansArray[ansArray.length-1]-before==1){
                ansArray=add(ansArray,destNegetive);
            }
            before=ansArray[ansArray.length-1];
            for(int k=ansArray.length-1;k>=1;k--){
                ansArray[k]=ansArray[k-1];
            }
            //s = new String(ansArray);
            //System.out.println(s.substring(0,32)+" "+s.substring(32));

        }
        char[] ans=Arrays.copyOfRange(ansArray,32,ansArray.length);
        return new DataType(new String(ans));
    }

}
