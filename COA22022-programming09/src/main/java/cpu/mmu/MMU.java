package cpu.mmu;

import memory.Memory;
import memory.cache.Cache;
import memory.tlb.TLB;
import util.Transformer;

/**
 * MMU内存管理单元抽象类
 * 接收一个48-bits的逻辑地址，并最终将其转换成32-bits的物理地址
 * Memory.SEGMENT和Memory.PAGE标志用于表示是否开启分段和分页。
 * 实际上在机器里从实模式进入保护模式后分段一定会保持开启(即使实模式也会使用段寄存器)，因此一共只要实现三种模式的内存管理即可：实模式、只有分段、段页式
 * 所有模式的物理地址都采用32-bits，实模式的物理地址高位补0
 */

public class MMU {

    private static final MMU mmuInstance = new MMU();

    private MMU() {
    }

    public static MMU getMMU() {
        return mmuInstance;
    }

    private final Memory memory = Memory.getMemory();

    private final Cache cache = Cache.getCache();

    private final TLB tlb = TLB.getTLB();

    /**
     * 读取数据
     *
     * @param logicAddr 48-bits逻辑地址
     * @param length    读取数据的长度
     * @return 内存中的数据
     */
    public byte[] read(String logicAddr, int length) {
        String physicalAddr = addressTranslation(logicAddr, length);
        return Cache.isAvailable ? cache.read(physicalAddr, length) : memory.read(physicalAddr, length);
    }

    /**
     * 写数据
     *
     * @param logicAddr 48-bits逻辑地址
     * @param length    读取数据的长度
     */
    public void write(String logicAddr, int length, byte[] data) {
        String physicalAddr = addressTranslation(logicAddr, length);

        if (Cache.isAvailable) {
            cache.write(physicalAddr, length, data);
        } else {
            memory.write(physicalAddr, length, data);
        }
    }

    /**
     * 地址转换
     *
     * @param logicAddr 48-bits逻辑地址
     * @param length    读取数据的长度
     * @return 32-bits物理地址
     */
    private String addressTranslation(String logicAddr, int length) {
        String linearAddr;      // 32位线性地址
        String physicalAddr;    // 32位物理地址
        if (!Memory.SEGMENT) {
            // 实模式：线性地址等于物理地址
            linearAddr = toRealLinearAddr(logicAddr);
            memory.real_load(linearAddr, length);  // 从磁盘中加载到内存
            physicalAddr = linearAddr;
        } else {
            // 分段模式
            int segIndex = getSegIndex(logicAddr);
            if (!memory.isValidSegDes(segIndex)) {
                // 缺段中断，该段不在内存中，内存从磁盘加载该段索引的数据
                memory.seg_load(segIndex);
            }
            linearAddr = toSegLinearAddr(logicAddr);
            // 权限检查
            int start = Integer.parseInt(Transformer.binaryToInt(linearAddr));
            int base = chars2int(memory.getBaseOfSegDes(segIndex));
            long limit = chars2int(memory.getLimitOfSegDes(segIndex));
            if (memory.isGranularitySegDes(segIndex)) {
                limit = (limit + 1) * Memory.PAGE_SIZE_B - 1;
            }
            if ((start < base) || (start + length > base + limit)) {
                throw new SecurityException("Segmentation Fault");
            }
            if (!Memory.PAGE) {
                // 分段模式：线性地址等于物理地址
                physicalAddr = linearAddr;
            } else {
                // 段页式
                int startvPageNo = Integer.parseInt(Transformer.binaryToInt(linearAddr.substring(0, 20)));   // 高20位表示虚拟页号
                int offset = Integer.parseInt(Transformer.binaryToInt(linearAddr.substring(20, 32)));         // 低12位的页内偏移
                int pages = (length - offset + Memory.PAGE_SIZE_B - 1) / Memory.PAGE_SIZE_B;
                if (offset > 0) pages++;
                int endvPageNo = startvPageNo + pages - 1;
                for (int i = startvPageNo; i <= endvPageNo; i++) {
                    //TODO
                    if (TLB.isAvailable) {
                        if (!tlb.isValidPage(i)) {
                            // 缺页中断，该页不在内存中，内存从磁盘加载该页的数据
                            memory.page_load(i);
                            tlb.write(i);
                        }
                    } else {
                        if (!memory.isValidPage(i)) {
                            // 缺页中断，该页不在内存中，内存从磁盘加载该页的数据
                            memory.page_load(i);
                        }
                    }

                }
                physicalAddr = toPagePhysicalAddr(linearAddr);
            }
        }
        return physicalAddr;
    }

    /**
     * 实模式下的逻辑地址转线性地址
     *
     * @param logicAddr 48位 = 16位段寄存器 + 32位offset，计算公式为：①(16-bits段寄存器左移4位 + offset的低16-bits) = 20-bits物理地址 ②高位补0到32-bits
     * @return 32-bits实模式线性地址
     */
    private String toRealLinearAddr(String logicAddr) {

        //加法之后计算
        return Transformer.intToBinary("" + (Integer.valueOf(logicAddr.substring(0, 16) + "0000", 2) + Integer.valueOf("0000" + logicAddr.substring(48 - 16), 2)));

    }

    /**
     * 分段模式下的逻辑地址转线性地址
     *
     * @param logicAddr 48位 = 16位段选择符(高13位index选择段表项) + 32位段内偏移
     * @return 32-bits 线性地址
     */
    //若果不分页就是物理地址
    private String toSegLinearAddr(String logicAddr) {
        int segIndex = Integer.valueOf(logicAddr.substring(0,16),2);
        //使用memory.getBaseOfSegDes(segIndex)段，段里面存储这基地址，基地址加上便宜量就是物理地址
        return Transformer.intToBinary(String.valueOf(Integer.parseInt(Transformer.binaryToInt(String.valueOf(memory.getBaseOfSegDes(segIndex)))) + Integer.parseInt(Transformer.binaryToInt("0" + logicAddr.substring(16, 48)))));
    }

    /**
     * 段页式下的线性地址转物理地址
     *
     * @param linearAddr 32位
     * @return 32-bits 物理地址
     */
    //前20位是页号，页的大小4*1024kb
    //在tlb里面可以通过直接通过线性地址的页号来得到物理地址的页
    // 框号
    //都是根据index去查内存，分段模式得到base，之后加上偏移量，分页模式，使用tlb或者memory直接转换
    private String toPagePhysicalAddr(String linearAddr) {
        String offset = linearAddr.substring(20);
        int vPageNo = Integer.parseInt(Transformer.binaryToInt("000000000000"+linearAddr.substring(0,20)));
        return TLB.isAvailable ? (new String(tlb.getFrameOfPage(vPageNo)) + offset) : (String.valueOf(memory.getFrameOfPage(vPageNo)) + offset);
    }

    /**
     * 根据逻辑地址找到对应的段索引
     *
     * @param logicAddr 逻辑地址
     * @return 整数表示的段索引
     */
    private int getSegIndex(String logicAddr) {
        String indexStr = logicAddr.substring(0, 13);
        return Integer.parseInt(Transformer.binaryToInt(indexStr));   // 段描述符索引
    }

    private int chars2int(char[] chars) {
        return Integer.parseInt(Transformer.binaryToInt(String.valueOf(chars)));
    }

    public void clear() {
        memory.clear();
        if (Cache.isAvailable) {
            cache.clear();
        }
        if (TLB.isAvailable) {
            tlb.clear();
        }
    }

}
