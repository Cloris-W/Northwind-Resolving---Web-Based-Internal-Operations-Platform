package com.northwind.resolve.billing.api

import com.northwind.resolve.billing.application.BillingExceptionNotFoundException
import com.northwind.resolve.billing.application.BillingExceptionReviewService
import com.northwind.resolve.billing.application.toDto
import com.northwind.resolve.billing.domain.BillingExceptionStatus
import com.northwind.resolve.billing.domain.RiskLevel
import com.northwind.resolve.billing.persistence.BillingExceptionRepository
import com.northwind.resolve.common.api.PageMetadata
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController @Profile("database") @RequestMapping("/api/billing/exceptions")
class BillingExceptionController(private val exceptions: BillingExceptionRepository, private val reviews: BillingExceptionReviewService) {
    @GetMapping fun list(@RequestParam(required=false) riskLevel: RiskLevel?, @RequestParam(required=false) status: BillingExceptionStatus?, @RequestParam(required=false) region: String?, @RequestParam(defaultValue="0") page:Int, @RequestParam(defaultValue="25") size:Int): BillingExceptionListResponse {
        if(page<0||size !in 1..100) throw IllegalArgumentException("page must be at least 0 and size must be between 1 and 100")
        val result=exceptions.page(riskLevel,status,region,page,size); return BillingExceptionListResponse(result.items.map{it.toDto()},PageMetadata(page,size,result.total,((result.total+size-1)/size).toInt()))
    }
    @GetMapping("/{id}") fun detail(@PathVariable id: UUID)=exceptions.find(id)?.toDto()?:throw BillingExceptionNotFoundException(id.toString())
    @PostMapping("/{id}/review") fun review(@PathVariable id: UUID,@RequestHeader("Idempotency-Key",required=false) key:String?,@RequestBody request:BillingExceptionReviewRequest)=reviews.review(id,request,key?:throw IllegalArgumentException("Idempotency-Key is required"))
}
