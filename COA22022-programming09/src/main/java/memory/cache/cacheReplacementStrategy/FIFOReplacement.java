package memory.cache.cacheReplacementStrategy;

import memory.Memory;
import memory.cache.Cache;

/**
 * 先进先出算法
 */
public class FIFOReplacement implements ReplacementStrategy {
//FIFO，就是根据时间戳，替换最小的
    //hit不需要任何操作
    @Override
    public void hit(int rowNO) {
        // do nothing
    }

    @Override
    public int replace(int start, int end, char[] addrTag, byte[] input) {
        //得到最小时间戳的行号（没有的会在get时候是-1）
        long minTime = Long.MAX_VALUE;
        int minIndex = -1;
        for (int i = start; i <= end; i++) {
            long curTime = Cache.getCache().getTimeStamp(i);
            if (curTime < minTime) {
                minTime = curTime;
                minIndex = i;
            }
        }
        //Writeback只在，replace的时候写到内存里面，写直达，当时就概率，writeback只去更改脏位
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
