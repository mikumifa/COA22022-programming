package memory.cache.cacheReplacementStrategy;

import memory.Memory;
import memory.cache.Cache;

/**
 * TODO 先进先出算法
 */
public class FIFOReplacement implements ReplacementStrategy {

    @Override
    public void hit(int rowNO) {
       Cache.getCache().addTimeStamp();
    }

    @Override
    public int replace(int start, int end, char[] addrTag, byte[] input) {

        long maxTime=-1;
        int ans=-1;
        for(int i=start;i<end;i++){
            long timeStamp = Cache.getCache().getTimeStamp(i);
            if(ans==-1|| timeStamp >maxTime){
                ans=i;
                maxTime=timeStamp;
            }
        }
        return ans;
    }
    public void init(int rowNo, char[] tagArea, byte[] data) {
        Cache.getCache().addTimeStamp();
        Cache.getCache().update(rowNo, tagArea, data);
        Cache.getCache().setTimeStamp(rowNo);
    }

}
