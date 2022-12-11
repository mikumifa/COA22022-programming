package memory;

import memory.cache.Cache;
import memory.disk.Disk;
import util.Transformer;

import java.util.Collections;

/**
 * 内存抽象类
 */
public class Memory {

    /*
     * ------------------------------------------
     *            |  Segment=true | Segment=false
     * ------------------------------------------
     * Page=true  |     段页式    |    不存在
     * ------------------------------------------
     * Page=false |    只有分段   |   实地址模式
     * ------------------------------------------
     */

    public static boolean SEGMENT = true;

    public static boolean PAGE = true;

    public static final int MEM_SIZE_B = 16 * 1024 * 1024;      // 主存大小 16 MB

    public static final int PAGE_SIZE_B = 4 * 1024;      // 页大小 4 KB

    private final byte[] memory = new byte[MEM_SIZE_B];

    private SegDescriptor[] GDT = new SegDescriptor[8];  // 全局描述符表

    private final PageItem[] pageTbl = new PageItem[Disk.DISK_SIZE_B / Memory.PAGE_SIZE_B]; // 页表

    private boolean[] pageValid = new boolean[MEM_SIZE_B / PAGE_SIZE_B];

    private static final Memory memoryInstance = new Memory();

    private Memory() {
    }

    public static Memory getMemory() {
        return memoryInstance;
    }

    private final Disk disk = Disk.getDisk();

    public static boolean timer = false;

    /**
     * 根据物理地址读取数据
     * 注意， read方法应该在load方法被调用之后调用，即read方法的目标页(如果开启分页)都是合法的
     *
     * @param pAddr 32位物理地址
     * @param len   待读取数据的长度
     * @return 读取出来的数据
     */
    public byte[] read(String pAddr, int len) {
        if (timer) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        byte[] data = new byte[len];
        int base = Integer.parseInt(Transformer.binaryToInt(pAddr));
        System.arraycopy(memory, base, data, 0, len);
        return data;
    }

    /**
     * 根据物理地址写数据
     *
     * @param pAddr 32位物理地址
     * @param len   待读取数据的长度
     */
    public void write(String pAddr, int len, byte[] data) {
        if (timer) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 通知Cache缓存失效
        if (Cache.isAvailable) {
            Cache.getCache().invalid(pAddr, len);
        }
        // 更新数据
        int start = Integer.parseInt(Transformer.binaryToInt(pAddr));
        System.arraycopy(data, 0, memory, start, len);
    }

    /**
     * 实模式下从磁盘中加载数据
     *
     * @param pAddr 实模式下，内存地址对应磁盘地址
     * @param len   数据段长度
     */
    //活用this.write,加载的东西从disk里面读取
    public void real_load(String pAddr, int len) {
        // TODO
        this.write(pAddr,len,disk.read(pAddr,len));
    }

    /**
     * 段式存储模式下，从磁盘中加载数据.段页式存储中，不用从磁盘中加载数据
     *
     * @param segIndex 段索引
     */
    public void seg_load(int segIndex) {
        // TODO
        //调用这就就是分段的，中间要讨论分页的情况
        //我们规定，除了测试需要，每个由MMU装载进入GDT的
        //GDT就是装载segDescriptor的集合
        //段，其段基址均为全0，其限长均为全1，granularity，未开启分页时粒度为false，开启分页后粒度为true。
        //的带分段，之后改一下分段的base,limit,validBit,granularity属性
        SegDescriptor segDescriptor = getSegDescriptor(segIndex);
        segDescriptor.base=String.join("", Collections.nCopies(32, "0")).toCharArray();
        segDescriptor.limit=String.join("", Collections.nCopies(20, "1")).toCharArray();
        segDescriptor.validBit=true;
        segDescriptor.granularity=PAGE;
        //段页式不用加载，段式存储要加载，实验简化，长度是全1，地址是全0
        if(!PAGE)
        {
            int len = Integer.parseInt(Transformer.binaryToInt("0" + String.valueOf(segDescriptor.limit)));
            String addr=String.valueOf(segDescriptor.base);
            this.write(addr, disk.read(addr,len).length,disk.read(addr,len));
        }



    }


    /**
     * 段页式存储下，从磁盘中加载数据
     * 不考虑16MB内存用满的情况
     *
     * @param vPageNo 虚拟页号
     */
    //根据虚拟页号得到，
    public void page_load(int vPageNo) {
        String start = Transformer.intToBinary(String.valueOf(vPageNo * PAGE_SIZE_B));
        byte[] data = Disk.getDisk().read(start, PAGE_SIZE_B);
        //找出空闲的页框
        int pageNo = -1;
        for (int i = 0; i < pageValid.length; i++) {
            if (!pageValid[i]) {
                pageNo = i;
                break;
            }
        }
        if (pageNo == -1) {
            throw new SecurityException("Out Of Memory");
        }
        String frameAddr = String.valueOf(Transformer.intToBinary(String.valueOf(pageNo * PAGE_SIZE_B)));
        write(frameAddr, PAGE_SIZE_B, data);
        pageValid[pageNo] = true;
        PageItem pageItem = getPageItem(vPageNo);
        pageItem.isInMem = true;
        pageItem.pageFrame = frameAddr.substring(0, 20).toCharArray();

    }

    public void clear() {
        GDT = new SegDescriptor[8 * 1024];
        for (PageItem pItem : pageTbl) {
            if (pItem != null) {
                pItem.isInMem = false;
            }
        }
        pageValid = new boolean[MEM_SIZE_B / PAGE_SIZE_B];
    }


    /**
     * 理论上应该为Memory分配出一定的系统保留空间用于存放段表和页表，并计算出每个段表和页表所占的字节长度，通过地址访问
     * 不过考虑到Java特性，在此作业的实现中为了简化难度，全部的16M内存都用于存放数据，段表和页表直接用独立的数据结构表示，不占用"内存空间"
     * 除此之外，理论上每个进程都会有对应的段表和页表，作业中则假设系统只有一个进程，因此段表和页表也只有一个，不需要再根据进程号选择相应的表
     */
    private static class SegDescriptor {

        private char[] base = new char[32];  // 32位基地址

        private char[] limit = new char[20]; // 20位限长，表示段在内存中的长度

        private boolean validBit = false;    // 有效位,为true表示被占用（段已在内存中），为false表示空闲（不在内存中）

        private boolean granularity = false;    // 粒度，为true表示段以页（4KB）为基本单位，为false表示段以字节为基本单位
    }

    private SegDescriptor getSegDescriptor(int index) {
        if (GDT[index] == null) {
            GDT[index] = new SegDescriptor();
        }
        return GDT[index];
    }

    /**
     * 根据segment descriptor的索引返回该SegDescriptor的limit
     *
     * @param index 段索引
     * @return 20-bits
     */
    public char[] getLimitOfSegDes(int index) {
        return getSegDescriptor(index).limit;
    }


    /**
     * 根据segment descriptor的索引返回该SegDescriptor的base
     *
     * @param index 段索引
     * @return 32-bits
     */
    public char[] getBaseOfSegDes(int index) {
        return getSegDescriptor(index).base;
    }

    /**
     * 根据segment descriptor的索引返回该SegDescriptor是否有效
     *
     * @param index 段索引
     * @return boolean
     */
    public boolean isValidSegDes(int index) {
        return getSegDescriptor(index).validBit;
    }

    /**
     * 根据segment descriptor的索引返回该SegDescriptor的粒度
     *
     * @param index 段索引
     * @return boolean
     */
    public boolean isGranularitySegDes(int index) {
        return getSegDescriptor(index).granularity;
    }

    /**
     * 强制创建一个段描述符，指向指定的物理地址
     * 此方法仅被测试用例使用
     *
     * @param segIndex 段索引
     * @param base     段基址
     * @param len      段长
     */
    public void alloc_seg_force(int segIndex, String base, int len, boolean granularity) {
        SegDescriptor sd = getSegDescriptor(segIndex);
        sd.base = base.toCharArray();
        sd.limit = Transformer.intToBinary(String.valueOf(len)).substring(1, 32).toCharArray();
        sd.validBit = true;
        sd.granularity = granularity;
    }


    /**
     * 页表项为长度为20-bits的页框号
     */
    private static class PageItem {

        private char[] pageFrame;   // 物理页框号

        private boolean isInMem = false;    // 装入位
    }

    private PageItem getPageItem(int index) {
        if (pageTbl[index] == null) {
            pageTbl[index] = new PageItem();
        }
        return pageTbl[index];
    }

    /**
     * 根据page索引返回该page是否在内存中
     *
     * @param vPageNo 虚拟页号
     * @return boolean
     */
    public boolean isValidPage(int vPageNo) {
        if (timer) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return getPageItem(vPageNo).isInMem;
    }


    /**
     * 根据虚页页号返回该页的物理页框号
     *
     * @param vPageNo 虚拟页号
     * @return 20-bits
     */
    public char[] getFrameOfPage(int vPageNo) {
        if (timer) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return getPageItem(vPageNo).pageFrame;
    }

}