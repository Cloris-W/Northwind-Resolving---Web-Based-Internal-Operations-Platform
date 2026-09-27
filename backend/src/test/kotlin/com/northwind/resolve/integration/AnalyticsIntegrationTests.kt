package com.northwind.resolve.integration

import com.northwind.resolve.analytics.application.AnalyticsService
import com.northwind.resolve.analytics.persistence.AnalyticsReadRepository
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal

class AnalyticsIntegrationTests {
 @Test fun `analytics reads imported-source tables from PostgreSQL`() { PostgreSQLContainer("postgres:16-alpine").withDatabaseName("analytics").withUsername("test").withPassword("test").use { db ->
  db.start(); Flyway.configure().dataSource(db.jdbcUrl,db.username,db.password).locations("filesystem:../database/migrations").load().migrate()
  val jdbc=NamedParameterJdbcTemplate(DriverManagerDataSource(db.jdbcUrl,db.username,db.password)); val raw=jdbc.jdbcTemplate
  raw.execute("INSERT INTO monthly_kpis VALUES ('2024-01-01',100,90,1,0.80,1,1,1),('2024-02-01',200,190,1,0.90,1,1,1)")
  raw.execute("INSERT INTO legacy_complaints (complaint_id,date_opened,raw_status,channel,raw_category,raw_priority,region,source_system_id,transferred_between_systems,sla_days,sla_breach,reopened,account_id) VALUES ('a','2024-01-01','Closed','web','Billing - disputed amount','P2','North','CaseTrack',true,1,false,false,'x'),('b','2024-02-01','Open','web','Billing - disputed amount','P2','North','CaseTrack',false,1,true,true,'y')")
  raw.execute("INSERT INTO cases (case_id,account_id,category,priority,region,status,sla_days,opened_at,assigned_team) VALUES ('a','x','BILLING','HIGH','North','OPEN',1,NOW(),'team')")
  raw.execute("INSERT INTO meter_region_monthly_metrics VALUES ('2024-01-01','North',100,0.20,0,0,'meter'),('2024-02-01','North',300,0.30,0,0,'meter')")
  raw.execute("INSERT INTO unit_costs VALUES ('Complaint handled end to end (average)',68,'USD','test'),('Complaint handled end to end (transferred between systems)',121,'USD','test')")
  val service=AnalyticsService(AnalyticsReadRepository(jdbc)); val dashboard=service.dashboard("North",null,null); val value=service.valueCase(BigDecimal("10"),BigDecimal("20"),BigDecimal.ZERO,BigDecimal("100"))
  assertEquals(BigDecimal("27.50"),dashboard.metrics.estimatedReadRatePercent);assertEquals(BigDecimal("86.67"),dashboard.metrics.firstContactResolutionPercent);assertEquals(BigDecimal("68.00"),value.observedBaseline.averageComplaintCost);assertEquals(BigDecimal("121.00"),value.observedBaseline.transferredComplaintCost)
 }}
}
