package com.ssafy.obosa.config;

import com.ssafy.obosa.util.DatabaseProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:application.yml")
@EnableJpaRepositories(basePackages = "com.ssafy.obosa.repository", entityManagerFactoryRef = "entityManager", transactionManagerRef = "platformTransactionManager")
@EnableJpaAuditing
public class DatabaseConfig
{
    private final DatabaseProperty databaseProperty;
    private final String ddl;
    private final String dialect;
    private final String namingStrategy;

    public DatabaseConfig(DatabaseProperty databaseProperty,
                          @Value("${spring.jpa.hibernate.ddl-auto}") String ddl,
                          @Value("${spring.jpa.hibernate.dialect}") String dialect,
                          @Value("${spring.jpa.hibernate.naming.physical-strategy}") String namingStrategy)
    {
        this.databaseProperty = databaseProperty;
        this.ddl=ddl;
        this.dialect = dialect;
        this.namingStrategy = namingStrategy;
    }

    public DataSource makeDataSource(String url)
    {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClass(com.mysql.cj.jdbc.Driver.class);
        dataSource.setUsername(databaseProperty.getUsername());
        dataSource.setPassword(databaseProperty.getPassword());

        return dataSource;
    }

    @Bean
    public DataSource routingDataSource()
    {
        ReplicationRoutingDataSource replicationRoutingDataSource = new ReplicationRoutingDataSource();

        DataSource master = makeDataSource(databaseProperty.getUrl());

        Map<Object, Object> dataSourceMap = new LinkedHashMap<>();
        dataSourceMap.put("master", master);

        databaseProperty.getSlaveList().forEach(slave ->
        {
          dataSourceMap.put(slave.getName(), makeDataSource(slave.getUrl()));
        });

        replicationRoutingDataSource.setTargetDataSources(dataSourceMap);
        replicationRoutingDataSource.setDefaultTargetDataSource(master);
        return replicationRoutingDataSource;
    }

    @Bean
    public DataSource dataSource()
    {
        return new LazyConnectionDataSourceProxy(routingDataSource());
    }

    @Bean (name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean()
    {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource());
        entityManagerFactoryBean.setPackagesToScan(new String[]{"com.ssafy.obosa.model.domain"});
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);

        HashMap<String, Object> properties = new HashMap<>();

        properties.put("hibernate.hbm2ddl.auto", this.ddl);
        properties.put("hibernate.dialect", this.dialect);
        properties.put("hibernate.physical_naming_strategy", namingStrategy);//스네이크형식을 자동으로 카멜케이스로 바꿔준다.

        entityManagerFactoryBean.setJpaPropertyMap(properties);

        return entityManagerFactoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory)
    {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(entityManagerFactory);
        return tm;
    }
}
