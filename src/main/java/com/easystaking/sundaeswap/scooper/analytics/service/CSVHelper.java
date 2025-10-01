package com.easystaking.sundaeswap.scooper.analytics.service;

import com.easystaking.sundaeswap.scooper.analytics.model.ExtendedScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.model.Scoop;
import lombok.extern.slf4j.Slf4j;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

@Slf4j
public class CSVHelper {

    public static ByteArrayOutputStream scoops(List<Scoop> scoops) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//             Long timestamp, String txHash, Long numOrders, String scooperHash, Boolean isMempool
             CsvListWriter csvWriter = new CsvListWriter(new OutputStreamWriter(outputStream), CsvPreference.STANDARD_PREFERENCE)) {
            csvWriter.write("timestamp", "tx_hash", "num_orders", "scooper_hash", "is_mempool");
            scoops.forEach(scoop -> {
                try {
                    csvWriter.write(scoop.timestamp(),
                            scoop.txHash(),
                            scoop.numOrders(),
                            scoop.scooperHash(),
                            scoop.isMempool());
                } catch (Exception e) {
                    log.warn("error:", e);
                }

            });
            return outputStream;
        } catch (IOException e) {
            throw new RuntimeException("fail to import data to CSV file: " + e.getMessage());
        }
    }

    public static ByteArrayOutputStream scoopersStats(List<ExtendedScooperStats> stats) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             CsvListWriter csvWriter = new CsvListWriter(new OutputStreamWriter(outputStream), CsvPreference.STANDARD_PREFERENCE)) {
            csvWriter.write("pub_key_hash", "address", "total_scoops", "total_orders", "total_protocol_fee", "total_transaction_fee", "total_num_mempool_orders");
            stats.forEach(scooperStats -> {
                try {
                    csvWriter.write(scooperStats.pubKeyHash(),
                            scooperStats.address(),
                            scooperStats.totalScoops(),
                            scooperStats.totalOrders(),
                            scooperStats.totalProtocolFee(),
                            scooperStats.totalTransactionFee(),
                            scooperStats.totalNumMempoolOrders());
                } catch (Exception e) {
                    log.warn("error:", e);
                }

            });
            return outputStream;
        } catch (IOException e) {
            throw new RuntimeException("fail to import data to CSV file: " + e.getMessage());
        }
    }
}
