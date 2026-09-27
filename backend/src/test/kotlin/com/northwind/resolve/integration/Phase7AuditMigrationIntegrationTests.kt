package com.northwind.resolve.integration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.flywaydb.core.Flyway
import java.util.UUID

class Phase7AuditMigrationIntegrationTests {
 @Test fun `v4 migrates and audit source reference is unique`(){PostgreSQLContainer("postgres:16-alpine").withDatabaseName("phase7").withUsername("test").withPassword("test").use{db->db.start();val flyway=Flyway.configure().dataSource(db.jdbcUrl,db.username,db.password).locations("filesystem:../database/migrations").load();assertEquals(4,flyway.migrate().migrationsExecuted);val jdbc=JdbcTemplate(org.springframework.jdbc.datasource.DriverManagerDataSource(db.jdbcUrl,db.username,db.password));val caseId="case-phase7";jdbc.execute("INSERT INTO cases (case_id,account_id,category,priority,region,status,sla_days,opened_at,assigned_team) VALUES ('$caseId','account','BILLING','HIGH','region','OPEN',1,NOW(),'team')");val ref=UUID.randomUUID().toString();jdbc.update("INSERT INTO audit_events (id,case_id,event_type,payload_hash,source_event_reference,status,created_at) VALUES (?,?,?,?,?,'PENDING',NOW())",UUID.randomUUID(),caseId,"BILL_CORRECTED","a".repeat(64),ref);assertEquals(ref,jdbc.queryForObject("SELECT source_event_reference FROM audit_events WHERE case_id=?",String::class.java,caseId));assertThrows(Exception::class.java){jdbc.update("INSERT INTO audit_events (id,case_id,event_type,payload_hash,source_event_reference,status,created_at) VALUES (?,?,?,?,?,'PENDING',NOW())",UUID.randomUUID(),caseId,"BILL_CORRECTED","b".repeat(64),ref)}}}
}
