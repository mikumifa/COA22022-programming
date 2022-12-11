package memory.cache;

import memory.Memory;
import memory.cache.cacheReplacementStrategy.ReplacementStrategy;
import util.Transformer;

import java.util.Arrays;

/**
 * 高速缓存抽象类
 */
public class Cache {

    public static boolean isAvailable = true; // 默认启用Cache

    public static final int CACHE_SIZE_B = 32 * 1024; // 32 KB 总大小

    public static final int LINE_SIZE_B = 64; // 64 B 行大小

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

    private ReplacementStrategy replacementStrategy;    // 替换策略

    public static boolean isWriteBack;   // 写策略

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
            //写回，把cache的rowNo，行号设为脏号（标记是否被修改）
            //写直达策略就是在write函数完成写cache后直接修改主存
            //写回策略就是在write函数完成写cache后设置好脏位，并在replace函数将要替换掉该行的时候将该行写回内存。
            if (isWriteBack) {
                cache[rowNO].dirty = true;
            } else {
                Memory.getMemory().write(calculatePAddr(rowNO), Cache.LINE_SIZE_B, cache_data);
            }

            addr += nextSegLen;
        }
    }

    /**
     * 查询{@link Cache#cache}表以确认包含pAddr的数据块是否在cache内
     * 如果目标数据块不在Cache内，则将其从内存加载到Cache
     *
     * @param pAddr 数据起始点(32位物理地址 = 26位块号 + 6位块内地址)
     * @return 数据块在Cache中的对应行号
     */
    private int fetch(String pAddr) {
        //物理地址得到blockNo，然后blockNo得到行号，没有命中就把块加载进去，然后返回rowNO
        int blockNO = getBlockNO(pAddr);    // 地址前26位int形式
        int rowNO = map(blockNO);  // 返回内存地址blockNO所对应的cache中的行，返回-1则表示没有命中

        if (rowNO == -1) {    // 未命中
            rowNO = loadBlock(blockNO);
        }
        return rowNO;
    }

    /**
     * 根据目标数据内存地址前26位的int表示，进行映射
     *
     * @param blockNO 数据在内存中的块号
     * @return 返回cache中所对应的行，-1表示未命中
     */
    private int map(int blockNO) {
        int setNO = blockNO % SETS;           // 获得内存地址blockNO所对应的组号setNO
        char[] addrTag = calculateTag(blockNO);   // 获得内存地址blockNO所对应的tag
        int start = setNO * setSize;
        int end = (setNO + 1) * setSize - 1;
        for (int i = start; i <= end; i++) {
            if (cacheInstance.isMatch(i, addrTag)) {   // 命中该行(前提是有数据且数据是vaild的)
                replacementStrategy.hit(i);//这个hit其实没有什么用，主要是为了更新cacheline里面的一些数据
                return i; // 返回该行
            }
        }
        return -1;
    }

    /**
     * 根据块号，结合具体的映射策略，计算数据块在Cache行中的Tag
     *
     * @param blockNO 数据在内存中的块号
     * @return 长度为26，结合具体的映射策略判断前多少位有效
     */
    private char[] calculateTag(int blockNO) {
        int tag = blockNO / SETS;
        return Transformer.intToBinary("" + tag).substring(6, 32).toCharArray();
    }

    /**
     * 未命中的情况下，将内存读取出该内存块的数据写入cache
     *
     * @param blockNO 数据在内存中的块号
     * @return 返回cache中所对应的行
     */
    //输入块号，把数据写进去
    private int loadBlock(int blockNO) {
        //TODO
        //根据块号来就散应该插在cache的哪一行。
        //blockNO可以计算出tag（blockNo除以组数可以得到组的二进制形式）
        //setNo是blockNo除以组数的余数
        //想想地址的组成形式

        int setNO = blockNO % SETS;
        char[] addrTag = calculateTag(blockNO);
        //如果是直接映射，就没有其他的形式。
        if (SETS == CACHE_SIZE_B / LINE_SIZE_B) {
            cacheInstance.update(setNO, addrTag, Memory.getMemory().read(Transformer.intToBinary(String.valueOf(Cache.LINE_SIZE_B * blockNO)), Cache.LINE_SIZE_B));
            return setNO;
        }
        //框一下组的范围，tag值主要是为了，cacheLine的tag属性
        return this.replacementStrategy.replace(setNO * setSize, (setNO + 1) * setSize - 1, addrTag, Memory.getMemory().read(Transformer.intToBinary(String.valueOf(Cache.LINE_SIZE_B * blockNO)), Cache.LINE_SIZE_B));
    }

    /**
     * 更新cache
     *
     * @param rowNO 需要更新的cache行号
     * @param tag   待更新数据的Tag
     * @param input 待更新的数据
     */
    public void update(int rowNO, char[] tag, byte[] input) {
        //TODO
        //更新cache，可以理解为把数据写入cache的制定位置，注意要把所有都更新一下
        CacheLine cacheLine = cache[rowNO];
        cacheLine.update(tag, input);
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
     * 判断该行是否命中
     *
     * @param rowNO 内存地址对应的cache行
     * @param tag   内存地址对应的tag
     * @return 判断结果
     */
    public boolean isMatch(int rowNO, char[] tag) {
        //TODO
        //判断行时候命中，只有在cache不为空，然后有有效位的时候，去比较tag是否一样(使用了，Arrays.equals方法)
        if (!this.cache[rowNO].validBit) {
            return false;
        }
        return Arrays.equals(this.cache[rowNO].tag, tag);
    }

    /**
     * 根据行号计算该行首地址对应的物理地址
     *
     * @param rowNO 行号
     * @return 对应的物理地址
     */
    //给cache行号可以知道cache的地址。
    //cache的每一行有一个标记位tag,他是货真价实的tag
    //虚拟地址长度=tag长度+offset+行地址的长度
    /*
    首先理清楚虚拟地址的结构，主要是计算出中间点的长度，根据组数来计算
    rowNo/setSize是组号，长度是offset位
     */
    public String calculatePAddr(int rowNO) {
        //TODO
        int offset = 0;
        for (int i = 1; i < SETS; i *= 2) {
            offset++;
        }
        String setNo = Transformer.intToBinary("" + rowNO / setSize).substring(32 - offset, 32);
        char[] tag = cache[rowNO].tag;
        return new String(tag).substring(offset, tag.length) + setNo + "000000";
    }
    //cache函数用来从外部获取cache的某一行的信息
    //其中，LFU算法（least frequently used),要增加访问次数，和获取访问次数
    //LRU(Least Recently used),最近最少使用算法，需要获取时间戳，FIFO也要获取时间戳
    // 获取有效位
    //TODO
    public boolean isValid(int rowNO) {
        return cache[rowNO].validBit;
    }

    // 获取脏位
    public boolean isDirty(int rowNO) {
        return cache[rowNO].dirty;
    }

    // LFU算法增加访问次数
    public void addVisited(int rowNO) {
        CacheLine cacheLine = cache[rowNO];
        cacheLine.visited = cacheLine.visited + 1;
    }

    // 获取访问次数
    public int getVisited(int rowNO) {
        //TODO
        //只要是validBit返回值是-1，表示必选他
        CacheLine cacheLine = cache[rowNO];
        if (cacheLine.validBit) {
            return cacheLine.visited;
        }
        return -1;
    }

    // 用于LRU算法，重置时间戳
    public void setTimeStamp(int rowNO) {
        CacheLine cacheLine = cache[rowNO];
        cacheLine.timeStamp = System.currentTimeMillis();
    }

    // 获取时间戳
    public long getTimeStamp(int rowNO) {
        CacheLine cacheLine = cache[rowNO];
        if (cacheLine.validBit) {
            return cacheLine.timeStamp;
        }
        return -1;
    }

    // 获取该行数据
    public byte[] getData(int rowNO) {
        return cache[rowNO].getData();
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
     * Cache行，每行长度为(1+26+{@link Cache#LINE_SIZE_B})
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
        // 那么对于值为0b1111的tag应该表示为00000000000000000000001111，其中低17位为有效长度
        char[] tag = new char[26];

        // 数据
        byte[] data = new byte[LINE_SIZE_B];

        byte[] getData() {
            return this.data;
        }

        char[] getTag() {
            return this.tag;
        }

        // 更新该CacheLine
        void update(char[] tag, byte[] input) {
            //TODO
            //使用了3种
            validBit = true;
            visited = 1;
            timeStamp = System.currentTimeMillis();
            System.arraycopy(tag, 0, this.tag, 0, tag.length);
            // input.length <= this.data.length
            System.arraycopy(input, 0, this.data, 0, input.length);
        }

    }
}
