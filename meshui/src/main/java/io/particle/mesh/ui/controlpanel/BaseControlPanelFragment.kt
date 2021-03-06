package io.particle.mesh.ui.controlpanel

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.SerialNumber
import io.particle.mesh.setup.fetchBarcodeData
import io.particle.mesh.setup.utils.safeToast
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.TitleBarOptions
import mu.KotlinLogging


interface DeviceProvider {
    val device: ParticleDevice
}


open class BaseControlPanelFragment : BaseFlowFragment() {

    private val log = KotlinLogging.logger {}

    override val titleBarOptions = TitleBarOptions(
        showBackButton = true
    )

    val device: ParticleDevice by lazy { (activity!! as DeviceProvider).device }

    @MainThread
    suspend fun startFlowWithBarcode(
        flowStarter: (device: ParticleDevice, barcode: CompleteBarcodeData) -> Unit
    ) {
        val cloud = ParticleCloudSDK.getCloud()
        flowSystemInterface.showGlobalProgressSpinner(true)
        val barcode = try {
            flowScopes.withWorker {
                cloud.fetchBarcodeData(device.id)
            }
        } catch (ex: Exception) {
            null
        }
        flowSystemInterface.showGlobalProgressSpinner(false)

        if (barcode == null) {
            activity?.safeToast("Unable to communicate with the Particle Cloud.  Please try again.")
            return
        }

        flowStarter(device, barcode)
    }

    @WorkerThread
    private fun fetchBarcodeData(deviceId: String): CompleteBarcodeData {
        val cloud = ParticleCloudSDK.getCloud()
        val device = cloud.getDevice(deviceId)

        val barcode = CompleteBarcodeData(
            serialNumber = SerialNumber(device.serialNumber!!),
            mobileSecret = device.mobileSecret!!
        )

        log.info { "Built barcode: $barcode" }

        return barcode
    }

}
