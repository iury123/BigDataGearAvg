/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inatel.demos;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apaflink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer09;
import org.apache.flink.streaming.util.serialization.JSONDeserializationSchema;
import org.apache.flink.util.Collector;
import java.util.Properties;

public class GearAmount {

  public static String CarNumberFilter = "3";
  public static int count = 0;

  public static void main(String[] args) throws Exception {

    try {
        CarNumberFilter = args[0];
    } catch (ArrayIndexOutOfBoundsException e) {
        System.out.println("Please, enter a car number as argument.");
    }

    // create execution environment
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", "localhost:9092");
    properties.setProperty("group.id", "flink_consumer");

    DataStream stream = env.addSource(
            new FlinkKafkaConsumer09<>("flink-demo",
            new JSONDeserializationSchema(),
            properties)
    );

    count = 0;

    stream  .flatMap(new TelemetryJsonParser())
            .keyBy(0)
            .timeWindow(Time.seconds(3))
            .reduce(new AvgReducer())
            .map(new AvgPrinter())
            .print();

    env.execute();
    }

    // FlatMap Function - Json Parser
    // Receive JSON data from Kafka broker and parse car number, speed and counter

    // {"Car": 9, "time": "52.196000", "telemetry": {"Vaz":"1.270000", "Distance": "4.605865", "LapTime": "0.128001",
    // "RPM": "591.266113", "Ay": "24.344515", "Gear": "3.000000","Throttle": "0.000000",
    // "Steer": "0.207988", "Ax": "-17.551264", "Brake": "0.282736","Fuel": "1.898847", "Speed": "34.137680"}}

    static class TelemetryJsonParser implements FlatMapFunction<ObjectNode, Tuple2<String, String>> {
      @Override
      public void flatMap(ObjectNode jsonTelemetry, Collector<Tuple2<String, String>> out) throws Exception {
        String carNumber = jsonTelemetry.get("Car").asText();
        String gear = jsonTelemetry.get("telemetry").get("Gear").asText();
        if(carNumber.equals(CarNumberFilter))
        {
          count = 0;
          out.collect(new Tuple2<>(carNumber, gear));
        }
      }
    }

    static class AvgReducer implements ReduceFunction<Tuple2<String, String>> {
      @Override
      public Tuple2<String, String> reduce(Tuple2<String, String> value1, Tuple2<String, String> value2) {
        if(value1 != null && value.f1 != null && !value1.f1.equals(value2.f1)) {
            count++;
        }
        return value2;
      }
    }

    static class AvgPrinter implements MapFunction<Tuple2<String, String>, String> {
      @Override
      public String map(Tuple2<String, String> avgEntry) throws Exception {
        return String.format("Car %s : %d times changed the gear", avgEntry.f0 , count ) ;
      }
    }
  }