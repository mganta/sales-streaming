package com.foo.sales;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer09;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer09;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.flink.streaming.util.serialization.TypeInformationSerializationSchema;
import org.apache.flink.util.Collector;
import org.apache.sling.commons.json.JSONObject;

import java.nio.charset.Charset;
import java.util.*;

public class SalesStreaming {

	
	public static void main(String[] args) throws Exception {
       // ParameterTool parameterTool = ParameterTool.fromArgs(args);

        Configuration conf = new Configuration();

        // conf.setBoolean(ConfigConstants.LOCAL_START_WEBSERVER, true);
        //conf.setString(ConfigConstants.JOB_MANAGER_WEB_LOG_PATH_KEY, "/tmp/foo.txt");

        final StreamExecutionEnvironment env = new LocalStreamEnvironment(conf);

		final Properties kafkaConsumerProps = new Properties();
	    kafkaConsumerProps.setProperty("zookeeper.connect", "localhost:2181");
	    kafkaConsumerProps.setProperty("group.id", "pinot-ingest");
	    kafkaConsumerProps.setProperty("auto.commit.enable", "false");
	    kafkaConsumerProps.setProperty("bootstrap.servers", "localhost:9092");
	    
	    FlinkKafkaConsumer09 kafkaSrc = new FlinkKafkaConsumer09(
	         "raw-input", new SimpleStringSchema(),
	         kafkaConsumerProps);

		DataStream kafkaInput = env.addSource(kafkaSrc);

		DataStream jsonStream = kafkaInput.rebalance().map(new MapFunction<String, JSONObject>() {
	            public JSONObject map(String value) throws Exception {
                    JSONObject jObj = new JSONObject(value);
                    return jObj;
	            }});

	    DataStream strippedDownStream = jsonStream.flatMap(
	       new FlatMapFunction<JSONObject, JSONObject>() {
	            public void flatMap(JSONObject value, Collector<JSONObject> out) throws Exception {
                    List<JSONObject> flattenedJsons = InvoiceFlattener.parseJSON(value);
                   for(JSONObject json : flattenedJsons)
                       out.collect(json);
	            }});

	  /**  DataStream byteArrayStream = strippedDownStream.map(
	        new MapFunction<JSONObject, byte[]>() {
	            public byte[] map(JSONObject value) throws Exception {
					System.out.println(value.toString());
					//System.out.println(value.toString().getBytes("UTF-8"));
	                return value.toString().getBytes(
	                   Charset.forName("UTF-8"));
	            }}); **/

        //   TypeInformationSerializationSchema<byte[]> typeInfo = new TypeInformationSerializationSchema<byte[]>(PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO, env.getConfig());
        //  byteArrayStream.addSink(new FlinkKafkaProducer09<byte[]>("localhost:9092", "pinot-input", typeInfo));

		DataStream jsonArrayStream = strippedDownStream.map(
				new MapFunction<JSONObject, String>() {
					public String map(JSONObject value) throws Exception {
						System.out.println(value.toString());
						return value.toString();
					}});



		jsonArrayStream.addSink(new FlinkKafkaProducer09<String>("localhost:9092", "pinot-input", new SimpleStringSchema()));
        env.execute("pinot-ingestion");

	}

}
