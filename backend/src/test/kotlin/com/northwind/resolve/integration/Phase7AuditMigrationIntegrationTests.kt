package com.northwind.resolve.integration
import org.junit.jupiter.api.Assertions.*
import com.northwind.resolve.audit.application.*
import com.northwind.resolve.audit.persistence.AuditEventRepository
import com.northwind.resolve.billing.domain.BillCorrectionEntity
import com.northwind.resolve.billing.persistence.BillCorrectionRepository
import com.northwind.resolve.cases.application.CaseWorkspaceService
import com.northwind.resolve.common.persistence.MutationIdempotencyRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.flywaydb.core.Flyway
import java.util.UUID

class Phase7AuditMigrationIntegrationTests {
 @Test fun `v4 migrates and audit source reference is unique`(){PostgreSQLContainer("postgres:16-alpine").withDatabaseName("phase7").withUsername("test").withPassword("test").use{db->db.start();val flyway=Flyway.configure().dataSource(db.jdbcUrl,db.username,db.password).locations("filesystem:../database/migrations").load();assertEquals(4,flyway.migrate().migrationsExecuted);val jdbc=JdbcTemplate(org.springframework.jdbc.datasource.DriverManagerDataSource(db.jdbcUrl,db.username,db.password));val caseId="case-phase7";jdbc.execute("INSERT INTO cases (case_id,account_id,category,priority,region,status,sla_days,opened_at,assigned_team) VALUES ('$caseId','account','BILLING','HIGH','region','OPEN',1,NOW(),'team')");val ref=UUID.randomUUID().toString();jdbc.update("INSERT INTO audit_events (id,case_id,event_type,payload_hash,source_event_reference,status,created_at) VALUES (?,?,?,?,?,'PENDING',NOW())",UUID.randomUUID(),caseId,"BILL_CORRECTED","a".repeat(64),ref);assertEquals(ref,jdbc.queryForObject("SELECT source_event_reference FROM audit_events WHERE case_id=?",String::class.java,caseId));assertThrows(Exception::class.java){jdbc.update("INSERT INTO audit_events (id,case_id,event_type,payload_hash,source_event_reference,status,created_at) VALUES (?,?,?,?,?,'PENDING',NOW())",UUID.randomUUID(),caseId,"BILL_CORRECTED","b".repeat(64),ref)}}}
 @Test fun `bill corrected creates one pending audit`(){PostgreSQLContainer("postgres:16-alpine").withDatabaseName("phase7flow").withUsername("test").withPassword("test").use{db->db.start();Flyway.configure().dataSource(db.jdbcUrl,db.username,db.password).locations("filesystem:../database/migrations").load().migrate();val jdbc=org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(org.springframework.jdbc.datasource.DriverManagerDataSource(db.jdbcUrl,db.username,db.password));jdbc.jdbcTemplate.execute("INSERT INTO cases (case_id,account_id,category,priority,region,status,sla_days,opened_at,assigned_team) VALUES ('case','account','BILLING','HIGH','region','OPEN',1,NOW(),'team')");val c=BillCorrectionEntity(UUID.randomUUID(),"account",BigDecimal.ONE,BigDecimal.TEN,"reason","region",Instant.EPOCH);BillCorrectionRepository(jdbc).create(c);val audits=AuditEventRepository(jdbc);val service=AuditApplicationService(audits,BillCorrectionRepository(jdbc),mock(CaseWorkspaceService::class.java),mock(MutationIdempotencyRepository::class.java),jacksonObjectMapper(),object:SolanaProvider{override fun submit(m:String)=throw IllegalStateException();override fun verify(s:String,m:String)=SolanaAnchorVerification.PENDING});service.createPendingForBillCorrection("case",c);service.createPendingForBillCorrection("case",c);assertEquals(1,jdbc.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_type='BILL_CORRECTED' AND status='PENDING'",Int::class.java));val audit=jdbc.jdbcTemplate.queryForMap("SELECT case_id,source_event_reference,status FROM audit_events WHERE event_type='BILL_CORRECTED'");assertEquals("case",audit["case_id"]);assertEquals(c.id.toString(),audit["source_event_reference"]);assertEquals("PENDING",audit["status"])}}
}
