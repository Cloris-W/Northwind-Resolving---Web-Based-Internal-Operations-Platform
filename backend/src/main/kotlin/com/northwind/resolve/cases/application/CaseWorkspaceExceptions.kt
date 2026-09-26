package com.northwind.resolve.cases.application

class CaseNotFoundException(caseId: String) : RuntimeException("Case $caseId was not found")
class AccountNotFoundException(accountId: String) : RuntimeException("Account $accountId was not found")
class InvalidRequestException(message: String) : RuntimeException(message)
