package com.alex.pingmonitor;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.firebirdsql.jdbc.FBDriver");
        dataSourceBuilder.url("jdbc:firebirdsql://xxx//srv/db/ping.db");
        dataSourceBuilder.username("sysdba");
        dataSourceBuilder.password("xxx");
        return dataSourceBuilder.build();
    }
}
