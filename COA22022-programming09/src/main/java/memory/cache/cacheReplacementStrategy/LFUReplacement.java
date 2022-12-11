package memory.cache.cacheReplacementStrategy;

import memory.Memory;
import memory.cache.Cache;

/**
 * 最近不经常使用算法
 */
public class LFUReplacement implements ReplacementStrategy {
//最少使用的算法
    @Override
    public void hit(int rowNO) {
        // 增加该行的访问次数 visited
        Cache.getCache().addVisited(rowNO);
    }

    @Override
    public int replace(int start, int end, char[] addrTag, byte[] input) {
        int minVisited = Integer.MAX_VALUE;     // visited最小值
        int minIndex = -1;       // visited最小值对应的下标

        for (int i = start; i <= end; i++) {
            int curVisited = Cache.getCache().getVisited(i);
            if (curVisited < minVisited) {
                minVisited = curVisited;
                minIndex = i;
            }
        }
        if (Cache.isWriteBack) {
            //脏位vaild必须同时满足
            //TODO
            if (Cache.getCache().isDirty(minIndex) && Cache.getCache().isValid(minIndex)) {
                String addr = Cache.getCache().calculatePAddr(minIndex);
                Memory.getMemory().write(addr, Cache.LINE_SIZE_B, Cache.getCache().getData(minIndex));
            }
        }
        Cache.getCache().update(minIndex, addrTag, input);
        return minIndex;
    }
}
