package memory.disk;

import java.util.Arrays;

public class Scheduler {

    /**
     * 先来先服务算法
     *
     * @param start   磁头初始位置
     * @param request 请求访问的磁道号
     * @return 平均寻道长度
     */
    public double FCFS(int start, int[] request) {
        // TODO
        double sum=0;
        for(int i=0;i<request.length;i++){
            sum+=Math.abs(request[i]-start);
            start=request[i];
        }
        return sum/request.length;
    }

    /**
     * 最短寻道时间优先算法
     *
     * @param start   磁头初始位置
     * @param request 请求访问的磁道号
     * @return 平均寻道长度
     */
    public double SSTF(int start, int[] request) {
        // TODO
        double sum=0;
        boolean[] visited = new boolean[request.length];
        for(int i=0;i<request.length;i++){
            int min= Integer.MAX_VALUE,minIndex=-1;
            for(int k=0;k<request.length;k++)
                if(!visited[k]&&Math.abs(start-request[k])<min) {
                    min = Math.abs(start - request[k]);
                    minIndex=k;
                }
            visited[minIndex]=true;
            sum+=min;
            start=request[minIndex];
        }
        return sum/ request.length;
    }

    /**
     * 扫描算法
     *
     * @param start     磁头初始位置
     * @param request   请求访问的磁道号
     * @param direction 磁头初始移动方向，true表示磁道号增大的方向，false表示磁道号减小的方向
     * @return 平均寻道长度
     */
    public double SCAN(int start, int[] request, boolean direction) {
        double sum = 0;
        Arrays.sort(request);
        int min = request[0];
        int max = request[request.length - 1];
        //得到最大值和最小值，画图
        if (direction) {
            if (start <= min) {
                sum = max - start;
            } else {
                sum = Disk.TRACK_NUM - 1 - start + Disk.TRACK_NUM - 1 - min;
            }
        } else {
            if (start >= max) {
                sum = start - min;
            } else {
                sum = start + max;
            }
        }
        return sum / request.length;
    }

}
