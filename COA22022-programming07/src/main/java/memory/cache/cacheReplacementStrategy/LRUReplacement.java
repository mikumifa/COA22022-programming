package memory.cache.cacheReplacementStrategy;

import memory.cache.Cache;

/**
 * TODO 最近最少用算法
 */
public class LRUReplacement implements ReplacementStrategy {

    @Override
    public void hit(int rowNO) {
        Cache.getCache().addTimeStamp();
        Cache.getCache().setTimeStamp(rowNO);
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





























