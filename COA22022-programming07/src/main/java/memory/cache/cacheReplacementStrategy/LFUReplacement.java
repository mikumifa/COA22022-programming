package memory.cache.cacheReplacementStrategy;

import memory.cache.Cache;

/**
 * TODO 最近不经常使用算法
 */
public class LFUReplacement implements ReplacementStrategy {

    @Override
    public void hit(int rowNO) {
        Cache.getCache().addVisited(rowNO);
    }

    @Override
    public int replace(int start, int end, char[] addrTag, byte[] input) {
        long minUse=-1;
        int ans=-1;
        for(int i=start;i<end;i++){
            long visited = Cache.getCache().getVisited(i);
            if(ans==-1|| visited <minUse){
                ans=i;
                minUse=visited;
            }
        }
        return ans;
    }

    public void init(int rowNo, char[] tagArea, byte[] data) {
        Cache.getCache().update(rowNo, tagArea, data);
        Cache.getCache().setTimeStamp(rowNo);
    }
}
