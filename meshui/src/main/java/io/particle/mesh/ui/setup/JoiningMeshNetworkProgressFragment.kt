package io.particle.mesh.ui.setup


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.utils.markProgress
import kotlinx.android.synthetic.main.fragment_joining_mesh_network_progress.*
import kotlinx.coroutines.delay


class JoiningMeshNetworkProgressFragment : BaseFlowFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_joining_mesh_network_progress, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

        observeForProgress(flowUiListener.mesh.commissionerStartedLD, R.id.status_stage_1) {
            observeForProgress(flowUiListener.mesh.targetJoinedMeshNetworkLD, R.id.status_stage_2) {
                observeForProgress(
                    flowUiListener.targetDevice.isClaimedLD,
                    R.id.status_stage_3,
                    1000
                ) {
                    // FIXME: WAT.  These shouldn't observe the same LiveData!
                    observeForProgress(
                        flowUiListener.targetDevice.isClaimedLD,
                        R.id.status_stage_4,
                        2000
                    )
                }
            }
        }

        val productName = getUserFacingTypeName()

        setup_header_text.text = Phrase.from(view, R.string.p_joiningmesh_header)
            .put("product_type", productName)
            .format()

        status_stage_1.text =
            Phrase.from(view, R.string.requesting_permission_to_add_to_mesh_network)
                .put("product_type", productName)
                .format()

        status_stage_2.text = Phrase.from(view, R.string.adding_the_xenon_to_the_mesh_network)
            .put("product_type", productName)
            .format()
    }
}

internal fun BaseFlowFragment.observeForProgress(
    liveData: LiveData<Boolean?>,
    @IdRes progressStage: Int,
    delayMillis: Long = 0,
    andThen: (() -> Unit)? = null
) {
    liveData.observe(
        this,
        Observer {
            if (it != true) {
                return@Observer
            }

            flowScopes.onMain {
                if (delayMillis > 0) {
                    delay(delayMillis)
                }
                markProgress(it, progressStage)
                andThen?.let { it() }
            }
        }
    )
}
