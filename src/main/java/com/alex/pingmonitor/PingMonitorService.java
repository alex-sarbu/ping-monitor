package com.alex.pingmonitor;

import com.alex.pingmonitor.util.HTMLTableBuilder;


import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import de.vandermeer.asciitable.AsciiTable;

@Path("/ping-monitor")
public class PingMonitorService {

    private final DataSourceConfig dataSourceConfig;
    private final DataSource dataSource;

    public PingMonitorService() {
        this.dataSourceConfig = new DataSourceConfig();
        this.dataSource = this.dataSourceConfig.getDataSource();
    }
    
    public PingMonitorService(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
        this.dataSource = dataSourceConfig.getDataSource();
    }
    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    public String getStatus() {
        String lastTS = "";
        try (var connection = dataSource.getConnection();
             var stmt = connection.createStatement();
             var rs = stmt.executeQuery("select id, ts, avg_response, cnt from ping_hourly where ts = (select max(ts) from ping_hourly)")) {
            while (rs.next()) {
                System.out.printf("%s %s%n",
                        rs.getString("ts"), rs.getString("avg_response"));
                lastTS = rs.getString("ts");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "error checking DB\n" +
                    "error message: " + e.getMessage();
        }
        return "ping monitor is up \n" +
                "most recent timestamp: " + lastTS;
    }

    @GET
    @Path("/runs")
    @Produces(MediaType.TEXT_HTML)
    public String getLastRuns() {
        List<PingHourlyRun> runs = new ArrayList<PingHourlyRun>();
        try (var connection = dataSource.getConnection();
             var stmt = connection.createStatement();
             var rs = stmt.executeQuery("select first 48 id, ts, avg_response, cnt, cnt_unreachable from ping_hourly order by ts desc")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                Timestamp ts = rs.getTimestamp("ts");
                BigDecimal avgResponse = rs.getBigDecimal("avg_response");
                int cnt = rs.getInt("cnt");
                int cntUnreachable = rs.getInt("cnt_unreachable");
                PingHourlyRun run = new PingHourlyRun(id, ts, avgResponse, cnt, cntUnreachable);
                runs.add(run);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "error retrieving last runs from DB\n" +
                    "error message: " + e.getMessage();
        }
        return getRunTable(runs);
    }

    @GET
    @Path("/runs-unreachable")
    @Produces(MediaType.TEXT_HTML)
    public String getLastUnreachableRuns() {
        List<PingHourlyRun> runs = new ArrayList<PingHourlyRun>();
        try (var connection = dataSource.getConnection();
             var stmt = connection.createStatement();
             var rs = stmt.executeQuery("select first 12 id, ts, avg_response, cnt, cnt_unreachable from ping_hourly where cnt_unreachable > 0 order by ts desc")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                Timestamp ts = rs.getTimestamp("ts");
                BigDecimal avgResponse = rs.getBigDecimal("avg_response");
                int cnt = rs.getInt("cnt");
                int cntUnreachable = rs.getInt("cnt_unreachable");
                PingHourlyRun run = new PingHourlyRun(id, ts, avgResponse, cnt, cntUnreachable);
                runs.add(run);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "error retrieving last runs from DB\n" +
                    "error message: " + e.getMessage();
        }
        return getRunTable(runs);
    }

    private String getRunTable(List<PingHourlyRun> runs) {

        HTMLTableBuilder htmlBuilder = new HTMLTableBuilder(null, true, runs.size(), 3);
        htmlBuilder.addTableHeader("Timestamp", "Avg Response", "Unreachable");
        for (PingHourlyRun run : runs) {
            htmlBuilder.addRowValues(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(run.ts.getTime()), new DecimalFormat("#0.######").format(run.avgResponse), Integer.toString(run.cntUnreachable));
        }
        return htmlBuilder.build();

        //AsciiTable at = new AsciiTable();
        //at.addRule();
        //at.addRow("Timestamp", "Avg Response", "Unreachable");
        //for (PingHourlyRun run : runs) {
        //    at.addRow(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(run.ts.getTime()), new DecimalFormat("#0.######").format(run.avgResponse), run.cntUnreachable);
        //}
        //at.addRule();
        //return at.render();

    }
}
