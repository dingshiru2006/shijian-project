package com.kb.system.module.system.dashboard.vo;

import com.kb.system.module.system.log.vo.OperationLogVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 仪表盘统计数据
 */
@Data
@AllArgsConstructor
public class DashboardVO {

    /** 知识库总数 */
    private Long kbCount;
    /** 文档总数 */
    private Long documentCount;
    /** 已发布文档数 */
    private Long publishedCount;
    /** 草稿数 */
    private Long draftCount;
    /** 用户总数 */
    private Long userCount;
    /** 分类总数 */
    private Long categoryCount;
    /** 标签总数 */
    private Long tagCount;
    /** ES 索引中的文档数 */
    private Long esDocCount;
    /** 今日操作日志数 */
    private Long todayLogCount;
    /** 各分类文档分布 */
    private List<ChartItem> categoryChart;
    /** 各知识库文档分布 */
    private List<ChartItem> kbChart;
    /** 最近操作日志 */
    private List<OperationLogVO> recentLogs;

    /**
     * 图表数据项
     */
    @Data
    @AllArgsConstructor
    public static class ChartItem {
        private String name;
        private Long value;
    }
}
