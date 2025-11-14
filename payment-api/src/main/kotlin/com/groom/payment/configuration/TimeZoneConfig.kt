package com.groom.payment.configuration

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import java.time.ZoneId
import java.util.TimeZone

@Configuration
class TimeZoneConfig {
    @PostConstruct
    fun configureDefaultTimeZone() {
        val zoneId = ZoneId.of("Asia/Seoul")
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
    }
}
