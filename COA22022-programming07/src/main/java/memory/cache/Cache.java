package memory.cache;

import memory.Memory;
import memory.cache.cacheReplacementStrategy.FIFOReplacement;
import memory.cache.cacheReplacementStrategy.ReplacementStrategy;
import util.Transformer;

import java.util.Arrays;

/**
 * 高速缓存抽象类
 */
public class Cache {

    public static final boolean isAvailable = true; // 默认启用Cache

    public static final int CACHE_SIZE_B = 32 * 1024; // 32 KB 总大小

    public static final int LINE_SIZE_B = 64; // 64 B 行大小

    public static final int LINE_NUM = CACHE_SIZE_B / LINE_SIZE_B;

    private final CacheLine[] cache = new CacheLine[CACHE_SIZE_B / LINE_SIZE_B];

    private int SETS;   // 组数

    private int setSize;    // 每组行数

    // 单例模式
    private static final Cache cacheInstance = new Cache();

    private Cache() {
        for (int i = 0; i < cache.length; i++) {
            cache[i] = new CacheLine();
        }
    }

    public static Cache getCache() {
        return cacheInstance;
    }

    private ReplacementStrategy replacementStrategy = new FIFOReplacement();    // 替换策略

    public static boolean isWriteBack;   // 写策略


    // LFU算法增加访问次数
    public void addVisited(int rowNO) {
        cache[rowNO].visited++;
    }

    // 获取访问次数
    public int getVisited(int rowNO) {
        return cache[rowNO].visited;
    }

    // 用于LRU算法，重置时间戳
    public void setTimeStamp(int rowNO) {

        cache[rowNO].timeStamp = (long) 0;
    }

    // 获取时间戳
    public long getTimeStamp(int rowNO) {
        return cache[rowNO].timeStamp;
    }


    public void addTimeStamp() {
        for (int i = 0; i < cache.length; i++) {
            CacheLine cacheLine = cache[i];
            if (cacheLine.validBit) {
                cacheLine.timeStamp++;
            }

        }
    }

    /**
     * 读取[pAddr, pAddr + len)范围内的连续数据，可能包含多个数据块的内容
     *
     * @param pAddr 数据起始点(32位物理地址 = 26位块号 + 6位块内地址)
     * @param len   待读数据的字节数
     * @return 读取出的数据，以char数组的形式返回
     */
    public byte[] read(String pAddr, int len) {
        byte[] data = new byte[len];
        int addr = Integer.parseInt(Transformer.binaryToInt("0" + pAddr));
        int upperBound = addr + len;
        int index = 0;
        while (addr < upperBound) {
            int nextSegLen = LINE_SIZE_B - (addr % LINE_SIZE_B);
            if (addr + nextSegLen >= upperBound) {
                nextSegLen = upperBound - addr;
            }
            int rowNO = fetch(Transformer.intToBinary(String.valueOf(addr)));
            byte[] cache_data = cache[rowNO].getData();
            int i = 0;
            while (i < nextSegLen) {
                data[index] = cache_data[addr % LINE_SIZE_B + i];
                index++;
                i++;
            }
            addr += nextSegLen;
        }
        return data;
    }

    /**
     * 向cache中写入[pAddr, pAddr + len)范围内的连续数据，可能包含多个数据块的内容
     *
     * @param pAddr 数据起始点(32位物理地址 = 26位块号 + 6位块内地址)
     * @param len   待写数据的字节数
     * @param data  待写数据
     */
    public void write(String pAddr, int len, byte[] data) {
        int addr = Integer.parseInt(Transformer.binaryToInt("0" + pAddr));
        int upperBound = addr + len;
        int index = 0;
        while (addr < upperBound) {
            int nextSegLen = LINE_SIZE_B - (addr % LINE_SIZE_B);
            if (addr + nextSegLen >= upperBound) {
                nextSegLen = upperBound - addr;
            }
            int rowNO = fetch(Transformer.intToBinary(String.valueOf(addr)));
            byte[] cache_data = cache[rowNO].getData();
            int i = 0;
            while (i < nextSegLen) {
                cache_data[addr % LINE_SIZE_B + i] = data[index];
                index++;
                i++;
            }

            // TODO
            char[] tag = getTag(pAddr);
            CacheLine cacheLine = cache[rowNO];
            if (isWriteBack) {
                cacheLine.dirty = true;
//                Long timeStamp = cacheLine.timeStamp;
                replacementStrategy.init(rowNO, tag, cache_data);
//                cacheLine.pAddr = pAddr;
//                cacheLine.len = len;
//                cacheLine.timeStamp = timeStamp + 1;
//                cacheLine.pastData = data;
            } else {
                Memory.getMemory().write(pAddr, len, data);
                replacementStrategy.init(rowNO, tag, cache_data);


            }


            addr += nextSegLen;
        }
    }

    public char[] getTag(String pAddr) {
        int tagSize = 26 - (int) (Math.log(SETS) / Math.log(2));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 26 - tagSize; i++) {
            builder.append("0");
        }
        builder.append(pAddr.substring(0, tagSize));
        return builder.toString().toCharArray();
    }


    /**
     * 查询{@link Cache#cache}表以确认包含pAddr的数据块是否在cache内
     * 如果目标数据块不在Cache内，则将其从内存加载到Cache
     *
     * @param pAddr 数据起始点(32位物理地址 = 26位块号 + 6位块内地址)
     * @return 数据块在Cache中的对应行号
     */
    private int fetch(String pAddr) {

        int blockNO = getBlockNO(pAddr);
        int lineId = map(blockNO);
        int lineSetId = blockNO % SETS;
        int lineFrist = lineSetId * setSize;
        int lineLast = lineFrist + setSize;
        char[] tagArea = getTag(pAddr);
        if (lineId == -1) {
            //没hit
            byte[] readBytes = Memory.getMemory().read(pAddr.substring(0, 26) + "000000", LINE_SIZE_B);
            int vaildId = vaild(blockNO);
            if (vaildId == -1) {
                //满了
                int replaceId = replacementStrategy.replace(lineFrist, lineLast, tagArea, readBytes);
                CacheLine cacheLine = cache[replaceId];
                if (cacheLine.dirty) {
                    Memory.getMemory().write(pAddr.substring(0, 26) + "000000",LINE_SIZE_B,readBytes);
                    cacheLine.dirty = false;
                }
                replacementStrategy.init(replaceId, tagArea, readBytes);
                return replaceId;
            } else {
                replacementStrategy.init(vaildId, tagArea, readBytes);
                return vaildId;
            }

        } else {
            //hit了
            replacementStrategy.hit(lineId);
            return lineId;
        }
    }


    /**
     * 根据目标数据内存地址前26位的int表示，进行映射
     *
     * @param blockNO 数据在内存中的块号
     * @return 返回cache中所对应的行，-1表示未命中
     */

    private int map(int blockNO) {

        int lineSetId = blockNO % SETS;
        int lineFrist = lineSetId * setSize;
        int lineLast = lineFrist + setSize;
        for (int i = lineFrist; i < lineLast; i++) {
            CacheLine cacheLine = cache[i];
            if (cacheLine.validBit) {
                String tagStr = String.valueOf(cacheLine.getTag());
                if (Integer.parseInt(Transformer.binaryToInt(tagStr)) == blockNO/SETS) {
                    return i;
                }

            }
        }
        return -1;
    }

    //
    private int vaild(int blockNO) {
        //看有没有空位,有救返回空位
        int lineSetId = blockNO % SETS;
        int lineFrist = lineSetId * setSize;
        int lineLast = lineFrist + setSize;
        for (int i = lineFrist; i < lineLast; i++) {
            CacheLine cacheLine = cache[i];
            if (!cacheLine.validBit) {
                return i;
            }
        }
        return -1;
    }


    /**
     * 更新cache
     *
     * @param rowNO 需要更新的cache行号
     * @param tag   待更新数据的Tag
     * @param input 待更新的数据
     */
    public void update(int rowNO, char[] tag, byte[] input) {
        CacheLine cacheLine = cache[rowNO];
        cacheLine.validBit = true;
        cacheLine.data = input;
        cacheLine.tag = tag;
    }


    /**
     * 从32位物理地址(26位块号 + 6位块内地址)获取目标数据在内存中对应的块号
     *
     * @param pAddr 32位物理地址
     * @return 数据在内存中的块号
     */
    private int getBlockNO(String pAddr) {
        return Integer.parseInt(Transformer.binaryToInt("0" + pAddr.substring(0, 26)));
    }


    /**
     * 该方法会被用于测试，请勿修改
     * 使用策略模式，设置cache的替换策略
     *
     * @param replacementStrategy 替换策略
     */
    public void setReplacementStrategy(ReplacementStrategy replacementStrategy) {
        this.replacementStrategy = replacementStrategy;
    }

    /**
     * 该方法会被用于测试，请勿修改
     *
     * @param SETS 组数
     */
    public void setSETS(int SETS) {
        this.SETS = SETS;
    }

    /**
     * 该方法会被用于测试，请勿修改
     *
     * @param setSize 每组行数
     */
    public void setSetSize(int setSize) {
        this.setSize = setSize;
    }

    /**
     * 告知Cache某个连续地址范围内的数据发生了修改，缓存失效
     * 该方法仅在memory类中使用，请勿修改
     *
     * @param pAddr 发生变化的数据段的起始地址
     * @param len   数据段长度
     */
    public void invalid(String pAddr, int len) {
        int from = getBlockNO(pAddr);
        int to = getBlockNO(Transformer.intToBinary(String.valueOf(Integer.parseInt(Transformer.binaryToInt("0" + pAddr)) + len - 1)));

        for (int blockNO = from; blockNO <= to; blockNO++) {
            int rowNO = map(blockNO);
            if (rowNO != -1) {
                cache[rowNO].validBit = false;
            }
        }
    }

    /**
     * 清除Cache全部缓存
     * 该方法会被用于测试，请勿修改
     */
    public void clear() {
        for (CacheLine line : cache) {
            if (line != null) {
                line.validBit = false;
            }
        }
    }

    /**
     * 输入行号和对应的预期值，判断Cache当前状态是否符合预期
     * 这个方法仅用于测试，请勿修改
     *
     * @param lineNOs     行号
     * @param validations 有效值
     * @param tags        tag
     * @return 判断结果
     */
    public boolean checkStatus(int[] lineNOs, boolean[] validations, char[][] tags) {
        if (lineNOs.length != validations.length || validations.length != tags.length) {
            return false;
        }
        for (int i = 0; i < lineNOs.length; i++) {
            CacheLine line = cache[lineNOs[i]];
            if (line.validBit != validations[i]) {
                return false;
            }
            if (!Arrays.equals(line.getTag(), tags[i])) {
                return false;
            }
        }
        return true;
    }


    /**
     * Cache行，每行长度为(1+22+{@link Cache#LINE_SIZE_B})
     */
    private static class CacheLine {

        // 有效位，标记该条数据是否有效
        boolean validBit = false;

        // 脏位，标记该条数据是否被修改
        boolean dirty = false;

        // 用于LFU算法，记录该条cache使用次数
        int visited = 0;

        // 用于LRU和FIFO算法，记录该条数据时间戳
        Long timeStamp = 0L;

        // 标记，占位长度为26位，有效长度取决于映射策略：
        // 直接映射: 17 位
        // 全关联映射: 26 位
        // (2^n)-路组关联映射: 26-(9-n) 位
        // 注意，tag在物理地址中用高位表示，如：直接映射(32位)=tag(17位)+行号(9位)+块内地址(6位)，
        // 那么对于值为0b1111的tag应该表示为00000000000000000000001111，其中低12位为有效长度
        char[] tag = new char[26];
        // 数据
        byte[] data = new byte[LINE_SIZE_B];

        byte[] getData() {
            return this.data;
        }

        char[] getTag() {
            return this.tag;
        }


    }
}
