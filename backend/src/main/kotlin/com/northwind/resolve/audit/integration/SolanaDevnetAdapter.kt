package com.northwind.resolve.audit.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.northwind.resolve.audit.application.SolanaProvider
import com.northwind.resolve.audit.application.SolanaSubmission
import com.northwind.resolve.audit.application.SolanaAnchorVerification
import com.northwind.resolve.audit.config.SolanaProperties
import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.Transaction
import org.p2p.solanaj.programs.MemoProgram
import org.p2p.solanaj.utils.Base58
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Base64

@Component class SolanaDevnetAdapter(private val properties:SolanaProperties,private val mapper:ObjectMapper):SolanaProvider {
 private val client by lazy {RestClient.builder().requestFactory(SimpleClientHttpRequestFactory().apply{setConnectTimeout(properties.timeout);setReadTimeout(properties.timeout)}).build()}
 override fun submit(memo:String):SolanaSubmission { require(properties.enabled&&properties.cluster=="devnet"){"Solana Devnet is not configured"};require(memo.length==98&&memo.matches(Regex("northwind-resolve:audit:v1:sha256:[a-f0-9]{64}"))){"Invalid audit memo"};val account=Account.fromJson(Files.readString(java.nio.file.Path.of(properties.keypairPath)));val blockhash=rpc("getLatestBlockhash",listOf(mapOf("commitment" to "confirmed"))).path("value").path("blockhash").asText().ifBlank{throw IllegalStateException("No blockhash")};val tx=Transaction().addInstruction(MemoProgram.writeUtf8(account.publicKey,memo));tx.setRecentBlockHash(blockhash);tx.sign(account);val signature=rpc("sendTransaction",listOf(Base64.getEncoder().encodeToString(tx.serialize()),mapOf("encoding" to "base64","preflightCommitment" to "confirmed","skipPreflight" to false))).asText().takeIf{it.matches(Regex("[1-9A-HJ-NP-Za-km-z]{32,128}"))}?:throw IllegalStateException("Invalid Solana signature");repeat(3){val status=rpc("getSignatureStatuses",listOf(listOf(signature),mapOf("searchTransactionHistory" to true))).path("value").firstOrNull();if(status!=null&&status.path("err").isNull&&(status.path("confirmationStatus").asText()=="confirmed"||status.path("confirmationStatus").asText()=="finalized"))return SolanaSubmission(signature);Thread.sleep(150)};throw IllegalStateException("Solana confirmation timed out") }
 override fun verify(signature:String,expectedMemo:String):SolanaAnchorVerification = runCatching { val tx=rpc("getTransaction",listOf(signature,mapOf("commitment" to "confirmed","encoding" to "json","maxSupportedTransactionVersion" to 0)));if(tx.isNull)return SolanaAnchorVerification.PENDING;val message=tx.path("transaction").path("message");val keys=message.path("accountKeys");val memos=message.path("instructions").filter{instruction->val index=instruction.path("programIdIndex").asInt(-1);index>=0&&keys.path(index).asText()==MEMO_PROGRAM_ID}.map{instruction->String(Base58.decode(instruction.path("data").asText()),StandardCharsets.UTF_8)};if(memos.size==1&&memos.single()==expectedMemo)SolanaAnchorVerification.MATCH else SolanaAnchorVerification.MISMATCH }.getOrElse{SolanaAnchorVerification.PENDING}
 private fun rpc(method:String,params:Any):JsonNode {val response=client.post().uri(properties.rpcUrl).contentType(MediaType.APPLICATION_JSON).body(mapOf("jsonrpc" to "2.0","id" to 1,"method" to method,"params" to params)).retrieve().body(JsonNode::class.java)?:throw IllegalStateException("Empty Solana response");if(response.has("error"))throw IllegalStateException("Solana RPC error");return response.path("result")}
 companion object{const val MEMO_PROGRAM_ID="MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"}
}
