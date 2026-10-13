package com.kb.system.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：ES 索引全量重建 专用队列/交换机/绑定
 */
@Configuration
public class RabbitMQConfig {

    /** 索引重建队列 */
    public static final String QUEUE_ES_REBUILD = "kb.es.rebuild.queue";
    /** 索引重建交换机（直连） */
    public static final String EXCHANGE_ES_REBUILD = "kb.es.rebuild.exchange";
    /** 路由键 */
    public static final String ROUTING_ES_REBUILD = "es.rebuild";

    @Bean
    public Queue esRebuildQueue() {
        return QueueBuilder.durable(QUEUE_ES_REBUILD).build();
    }

    @Bean
    public DirectExchange esRebuildExchange() {
        return new DirectExchange(EXCHANGE_ES_REBUILD, true, false);
    }

    @Bean
    public Binding esRebuildBinding(Queue esRebuildQueue, DirectExchange esRebuildExchange) {
        return BindingBuilder.bind(esRebuildQueue).to(esRebuildExchange).with(ROUTING_ES_REBUILD);
    }

    /** 消息体使用 JSON 序列化 */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
