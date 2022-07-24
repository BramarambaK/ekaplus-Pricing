package com.eka.ekaPricing.standalone;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.ConnectorStartFailedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.netty.handler.timeout.TimeoutException;

@Component
public class KafkaProducerhelper {
	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(KafkaProducerhelper.class);
	@Value("eka.formula.kafka.host")
	private String kafka_host;
	@Value("eka.kafka.topic")
	private String topic;
	
	@Async
	public void push(String response, String requestId) {
		Properties props = new Properties();
		props.put("bootstrap.servers", kafka_host);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		ProducerRecord producerRecord = new ProducerRecord(topic, requestId, response);

		KafkaProducer producer = new KafkaProducer(props);
		try {
			producer.send(producerRecord);
		} catch (ConnectorStartFailedException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("ConnectorStartFailedException while sending records to kafka"),e);
		} catch (IllegalStateException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("IllegalStateException while sending records to kafka"),e);
		}
		catch (TimeoutException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("TimeoutException while sending records to kafka"),e);
		} catch (KafkaException e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("KafkaException while sending records to kafka"),e);
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception while sending records to kafka"),e);
		} finally {
			producer.close();
		}

	}
}
