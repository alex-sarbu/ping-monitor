package com.alex.pingmonitor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@SpringBootApplication
public class PingMonitorApplication implements ApplicationRunner {

	private static DataSource ds;

	public static void main(String[] args) {
		SpringApplication.run(PingMonitorApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments arg0) throws Exception {
		ds = (new DataSourceConfig()).getDataSource();
		while (true) {
			BigDecimal responseTimeDecimal = new BigDecimal(runPing());
			insertPingHoury(responseTimeDecimal.movePointLeft(6));
			Thread.sleep(5000);
		}

	}

	public void insertPingHoury(BigDecimal responseTime) {
		PingHourlyRun lastRun = getLastRun();

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new java.util.Date());
		calendar.add(Calendar.HOUR_OF_DAY, -1);

		String sql;

		//logError("NOW ---> " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(calendar.getTime()));
		//logError("THEN --> " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(lastRun.ts.getTime()));

		if (lastRun == null || lastRun.ts.before(calendar.getTime())) {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date());
			if (responseTime.signum() > 0) {
				sql = "insert into ping_hourly (ts, avg_response) values ('" + timeStamp + "', '" + responseTime + "')";
			} else {
				sql = "insert into ping_hourly (ts, avg_response, cnt_unreachable) values ('" + timeStamp + "', '" + responseTime + "', 1 )";
			}
		} else {
			int cnt = lastRun.cnt + 1;
			if (responseTime.signum() > 0) {
				BigDecimal avgResponse = lastRun.avgResponse.multiply(new BigDecimal(lastRun.cnt)).add(responseTime).divide(new BigDecimal(cnt), RoundingMode.HALF_UP);
				sql = "update ping_hourly set cnt = " + cnt + ", avg_response = '" + avgResponse + "' where id = " + lastRun.id;
			} else {
				int cntUnreachable = lastRun.cntUnreachable + 1;
				sql = "update ping_hourly set cnt = " + cnt + ", cnt_unreachable = " + cntUnreachable + " where id = " + lastRun.id;
				insertUnreachable();
			}
		}

		logInfo(sql);

		try (var connection = ds.getConnection();
			 var stmt = connection.createStatement();
			 ) {
			int rs = stmt.executeUpdate(sql);
			logInfo("insert successful, returned " + rs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Boolean insertUnreachable() {
		String sql = "insert into log_unreachable (text) values ('unreachable')";

		try (var connection = ds.getConnection();
			 var stmt = connection.createStatement();
		) {
			int rs = stmt.executeUpdate(sql);
			return rs == 1;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static PingHourlyRun getLastRun() {
		try (var connection = ds.getConnection();
			 var stmt = connection.createStatement();
			 var rs = stmt.executeQuery("select id, ts, avg_response, cnt, cnt_unreachable from ping_hourly where ts = (select max(ts) from ping_hourly)")) {
			while (rs.next()) {
				int id = rs.getInt("id");
				Timestamp ts = rs.getTimestamp("ts");
				BigDecimal avgResponse = rs.getBigDecimal("avg_response");
				int cnt = rs.getInt("cnt");
				int cntUnreachable = rs.getInt("cnt_unreachable");
				PingHourlyRun lastRun = new PingHourlyRun(id, ts, avgResponse, cnt, cntUnreachable);
				logInfo("retrieved last run: " + lastRun);
				return lastRun;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static long runPing() {
		Socket connection = new Socket();
		boolean reachable;
		long start = System.nanoTime();
		try {
			try {
				SocketAddress sa = new InetSocketAddress("8.8.8.8", 53);
				connection.connect(sa, 5000);
			} finally {
				connection.close();
			}
			long dur = (System.nanoTime() - start);
			logInfo(  "response " + dur + " us 8.8.8.8" );
			return dur;
		} catch (Exception e) {
			logError("8.8.8.8 UNREACHABLE");
			return -1;
		}
	}

	public static String getTimestampString() {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
		return "[" + timeStamp + "]";
	}

	public static void logInfo(String s) {
		System.out.println(getTimestampString() + " " + s);
	}


	public static void logError(String s) {
		final String ANSI_RED = "\u001B[31m";
		final String ANSI_RESET = "\u001B[0m";
		System.out.println(ANSI_RED + getTimestampString() + " " + s + ANSI_RESET);
	}

}
