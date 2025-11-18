package com.example.order.service

import com.example.order.model.OrderRequest
import com.example.order.model.OrderResponse
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class OrderService(
    private val kafkaTemplate: KafkaTemplate<String, order.OrderRequest>
) {
    fun createOrder(orderRequest: OrderRequest): OrderResponse {
        val orderId = 10
        sendNewOrdersEvent(orderId)
        return OrderResponse(id = orderId)
    }

    private fun sendNewOrdersEvent(orderId: Int) {
        println("[OrderService] Sending NewOrdersEvent for orderId '$orderId' on new-orders topic")
        val newOrder = order.OrderRequest.newBuilder().setId(
            orderId
        ).setOrderItems(emptyList()).build()
        val message: ProducerRecord<String, order.OrderRequest> = ProducerRecord<String, order.OrderRequest>(
            "new-orders",
            newOrder
        ).also {
            it.headers().add("orderRequestId", "1234".toByteArray())
        }

        kafkaTemplate.send(message)
        println("[OrderService] Sent message to topic 'new-order' - $newOrder")
    }
}