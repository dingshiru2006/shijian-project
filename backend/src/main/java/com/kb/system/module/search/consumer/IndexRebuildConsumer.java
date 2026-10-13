package com.kb.system.module.search.consumer;

import com.kb.system.config.RabbitMQConfig;
import com.kb.system.module.search.service.IndexRebuildService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 索引重建消息消费者：监听 RabbitMQ 队列，后台执行 ES 全量索引重建
 * <p>
 * 手动 ACK：任务结果已写入任务表（成功/失败均有记录），失败也 ACK 避免毒消息无限循环。
 */
@Component
public class IndexRebuildConsumer {

    private static final Logger log = LoggerFactory.getLogger(IndexRebuildConsumer.class);

    private final IndexRebuildService indexRebuildService;

    public IndexRebuildConsumer(IndexRebuildService indexRebuildService) {
        this.indexRebuildService = indexRebuildService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ES_REBUILD)
    public void handleRebuildTask(Long taskId, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到索引重建消息: taskId={}", taskId);
        try {
            indexRebuildService.executeRebuild(taskId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("索引重建消息消费失败: taskId={}", taskId, e);
            channel.basicAck(deliveryTag, false);
        }
    }
}
