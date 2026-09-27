package com.northwind.resolve.billing.application

import com.northwind.resolve.billing.domain.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class BillingRiskEngineTests {
    private val engine = BillingRiskEngine()
    @Test fun `scores every deterministic rule and thresholds exactly`() {
        assertEquals(RiskLevel.LOW, engine.assess(RiskInput(false,false,false,false,false)).level)
        assertEquals(25, engine.assess(RiskInput(false,false,false,true,true)).score)
        val high=engine.assess(RiskInput(true,true,false,false,true))
        assertEquals(65,high.score); assertEquals(RiskLevel.HIGH,high.level)
        assertEquals(listOf("ESTIMATED_READ","DEVIATION_OVER_50_PERCENT","PREVIOUS_BILLING_COMPLAINT"),high.reasonCodes)
    }
}
