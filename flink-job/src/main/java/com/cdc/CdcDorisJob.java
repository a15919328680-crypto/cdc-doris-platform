package com.cdc;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.doris.sink.DorisSink;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;

public class CdcDorisJob {
    public static void main(String[] args) throws Exception {
        if (args.length < 11) {
            System.err.println("Usage: CdcDorisJob <mysqlHost> <mysqlPort> <mysqlUser> <mysqlPwd> <mysqlDb> <mysqlTable> " +
                    "<dorisFe> <dorisDb> <dorisTable> <dorisUser> <dorisPwd>");
            System.exit(1);
        }

        String mysqlHost = args[0];
        String mysqlPort = args[1];
        String mysqlUser = args[2];
        String mysqlPwd = args[3];
        String mysqlDb = args[4];
        String mysqlTable = args[5];

        String dorisFe = args[6];
        String dorisDb = args[7];
        String dorisTable = args[8];
        String dorisUser = args[9];
        String dorisPwd = args[10];

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        env.enableCheckpointing(5000);
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setCheckpointingInterval(5000);
        env.getCheckpointConfig().setLocalRecoveryEnabled(true);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(3000);

        MySqlSource<String> mysqlSource = MySqlSource.<String>builder()
                .hostname(mysqlHost)
                .port(Integer.parseInt(mysqlPort))
                .username(mysqlUser)
                .password(mysqlPwd)
                .databaseList(mysqlDb)
                .tableList(mysqlDb + "." + mysqlTable)
                .deserializer(new JsonDebeziumDeserializationSchema())
                .build();

        DataStream<String> stream = env.fromSource(mysqlSource, WatermarkStrategy.noWatermarks(), "MySQL-CDC");

        DorisSink<String> dorisSink = DorisSink.<String>builder()
                .setFenodes(dorisFe)
                .setTableIdentifier(dorisDb + "." + dorisTable)
                .setUsername(dorisUser)
                .setPassword(dorisPwd)
                .setSinkLabelPrefix("cdc-" + mysqlDb + "-" + mysqlTable + "-" + System.currentTimeMillis())
                .build();

        stream.sinkTo(dorisSink);

        env.execute("CDC-To-Doris-" + mysqlDb + "-" + mysqlTable);
    }
}
