package com.northwind.resolve.billing.application
class BillingExceptionNotFoundException(id: String) : RuntimeException("Billing exception $id was not found")
