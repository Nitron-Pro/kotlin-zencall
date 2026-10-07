package pro.nitron.zencall.call

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pro.nitron.zencall.data.prefs.FirewallPreferences

class ZenCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var engine: ScreeningEngine

    override fun onCreate() {
        super.onCreate()
        engine = ScreeningEngine(applicationContext)
        Log.i(TAG, "ZenCallScreeningService initialized")
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val startTime = System.currentTimeMillis()
        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart

        Log.d(TAG, "Incoming call received for screening: $rawNumber")

        serviceScope.launch {
            try {
                val decision = engine.evaluateFromRepository(rawNumber)

                val responseBuilder = CallResponse.Builder()

                when (decision) {
                    is ScreeningDecision.Allow -> {
                        responseBuilder.setDisallowCall(false)
                        responseBuilder.setRejectCall(false)
                        responseBuilder.setSkipCallLog(false)
                        responseBuilder.setSkipNotification(false)
                        Log.i(TAG, "Screening result: ALLOWED call from $rawNumber")
                    }
                    is ScreeningDecision.Silence -> {
                        responseBuilder.setDisallowCall(false)
                        responseBuilder.setRejectCall(false)
                        responseBuilder.setSilenceCall(true)
                        responseBuilder.setSkipNotification(true)
                        Log.i(TAG, "Screening result: SILENCED call from $rawNumber (Reason: ${decision.reason})")
                    }
                    is ScreeningDecision.Block -> {
                        responseBuilder.setDisallowCall(decision.shouldDisallowCall)
                        responseBuilder.setRejectCall(decision.shouldRejectCall)
                        responseBuilder.setSkipCallLog(decision.shouldSkipCallLog)
                        responseBuilder.setSkipNotification(decision.shouldSkipNotification)
                        Log.i(TAG, "Screening result: BLOCKED call from $rawNumber (Reason: ${decision.reason})")
                    }
                }

                val response = responseBuilder.build()
                respondToCall(callDetails, response)

                val latency = System.currentTimeMillis() - startTime
                Log.d(TAG, "Call screening completed in ${latency}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Error screening call, fail-open to allow", e)
                val fallbackResponse = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .build()
                respondToCall(callDetails, fallbackResponse)
            }
        }
    }

    companion object {
        private const val TAG = "ZenCallScreening"
    }
}
