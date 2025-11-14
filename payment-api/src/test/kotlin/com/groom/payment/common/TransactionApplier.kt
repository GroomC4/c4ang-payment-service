package com.groom.payment.common

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("test")
class TransactionApplier {
    @Transactional
    fun <T> applyPrimaryTransaction(doSomething: () -> T) = doSomething()
}
