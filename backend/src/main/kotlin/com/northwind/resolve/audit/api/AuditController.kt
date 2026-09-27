package com.northwind.resolve.audit.api

import com.northwind.resolve.audit.application.AuditApplicationService
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController @Profile("database") @RequestMapping("/api/audit/events")
class AuditController(private val audits:AuditApplicationService) {
 @GetMapping fun list(@RequestParam caseId:String)=audits.list(caseId)
 @PostMapping fun create(@RequestBody request:CreateAuditEventRequest,@RequestHeader("Idempotency-Key") key:String)=ResponseEntity.status(HttpStatus.ACCEPTED).body(audits.create(request,key))
 @GetMapping("/{id}/verify") fun verify(@PathVariable id:UUID)=audits.verify(id)
}
