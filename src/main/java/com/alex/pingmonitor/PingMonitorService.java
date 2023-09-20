package com.alex.pingmonitor;

import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

}
