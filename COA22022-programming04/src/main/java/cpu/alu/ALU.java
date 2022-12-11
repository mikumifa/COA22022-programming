package cpu.alu;

import util.*;

import javax.xml.crypto.Data;
import java.util.Arrays;


/**
 * Arithmetic Logic Unit
 * ALU封装类
 */
public class ALU {

    DataType remainderReg;

    public char[] high32Add(char[] srcStr, char[] destStr) {
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
    public void leftMove(char[] x){
        for(int i=0;i<x.length-1;i++){
            x[i]=x[i+1];
        }
    }
    public boolean high32zero(char[] x){
        for(int i=0;i<32;i++)
        {
            if(x[i]!='0')
                return  false;
        }
        return true;
    }
    public DataType div(DataType src, DataType dest) throws ArithmeticException {
        // TODO
        DataType X=dest;
        DataType Y=src;
        char[] XZregister=new char[64];
        char[] YPostive=Y.toString().toCharArray();
        char[] XcharArray=X.toString().toCharArray();
        char[] YNegtive=negetifyDataType(Y).toString().toCharArray();
        boolean isFu=false;


        if(high32zero(YPostive)) {
            throw new ArithmeticException();
        }
        if(XcharArray[0]=='1') {
            isFu = true;
            X=negetifyDataType(X);
            XcharArray=X.toString().toCharArray();
        }
        for(int i=32;i<64;i++){
            XZregister[i]=XcharArray[i-32];
        }
        for(int i=0;i<32;i++)
            XZregister[i]=XZregister[32];
        boolean differentSign=(XZregister[0]!=YPostive[0]);

        for(int i=0;i<32;i++){
            leftMove(XZregister);
            char preSign=XZregister[0];
            boolean sameSign=(XZregister[0]==YPostive[0]);
            XZregister=high32Add(XZregister,sameSign?YNegtive:YPostive);
            if(high32zero(XZregister)||XZregister[0]==preSign){
                XZregister[63]='1';
            }
            else {
                XZregister = high32Add(XZregister, sameSign?YPostive:YNegtive);
                XZregister[63] = '0';
            }
        }
        char[] Z = new char[32];
        char[] Reg=new char[32];
        for(int i=0;i<32;i++)
        {
            Reg[i]=XZregister[i];
        }
        for(int i=0;i<32;i++){
            Z[i]=XZregister[i+32];
        }
        DataType ans = new DataType(String.valueOf(Z));
        if(differentSign){
            ans=negetifyDataType(ans);
        }
        remainderReg=new DataType(String.valueOf(Reg));
        if(isFu){
            remainderReg=negetifyDataType(remainderReg);
            ans=negetifyDataType(ans);
        }
        return ans;
    }
}
