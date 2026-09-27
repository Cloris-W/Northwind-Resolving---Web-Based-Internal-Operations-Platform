package com.northwind.resolve.audit.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("northwind.solana")
data class SolanaProperties(var enabled:Boolean=false, var cluster:String="devnet", var rpcUrl:String="https://api.devnet.solana.com", var keypairPath:String="", var commitment:String="confirmed", var timeout:Duration=Duration.ofSeconds(10), var retryDelay:Duration=Duration.ofMinutes(5))
