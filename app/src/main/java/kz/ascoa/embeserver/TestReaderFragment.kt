package kz.ascoa.embeserver

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import kz.ascoa.embeserver.databinding.FragmentTestReaderBinding
import org.java_websocket.exceptions.WebsocketNotConnectedException
import java.net.URI
import java.net.URISyntaxException

/**
 * A simple [Fragment] subclass.
 * Use the [TestReaderFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TestReaderFragment : Fragment() {

    private var _uiBinding: FragmentTestReaderBinding? = null

    var tagEpc: String = ""
        get() {
            return field
        }
        set(value){
            field = value
            if(value.length > 0) {
                activity?.runOnUiThread {
                    uiBinding.tagLocationButton.isEnabled = true
                }
            }
        }

    // This property is only valid between onCreateView and
    // onDestroyView.
    val uiBinding get() = _uiBinding!!

    private var wsClient: WSClient? = null;
    private var uri: URI = URI("ws://127.0.0.1:38301/")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(wsClient == null) {
            val prefManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val wsPort = prefManager.getString("web_socket_port", "38301")
            uri = try {
                URI("ws://127.0.0.1:${wsPort}/")
            } catch (e: URISyntaxException) {
                e.printStackTrace()
                throw e
                return
            }
            wsClient = activity?.let { it1 -> WSClient(uri, it1, this) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _uiBinding = FragmentTestReaderBinding.inflate(inflater, container, false)
        if( wsClient == null ) {
            wsClient = activity?.let { it1 -> WSClient(uri, it1, this) }
        }
        if(!wsClient?.isOpen!!) {
            wsClient = activity?.let { it1 -> WSClient(uri, it1, this) }
            wsClient!!.connect()
        }
        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiBinding.readTag.setOnClickListener{
            val message = "${WSCommands.READ_TAG}${Dividers.FIELD_DIVIDER}"
            sendMessage(message)
        }

        uiBinding.continuousReadButton.setOnClickListener{
            val message = "${WSCommands.READ_TAG_CONTINUOUS}${Dividers.FIELD_DIVIDER}"
            sendMessage(message)
        }

        uiBinding.stopReadingButton.setOnClickListener {
            val message = "${WSCommands.STOP_READING}${Dividers.FIELD_DIVIDER}"
            sendMessage(message)
        }

        uiBinding.tagLocationButton.setOnClickListener {
            val message = "${WSCommands.LOCATE_TAG}${Dividers.FIELD_DIVIDER}${tagEpc}"
            sendMessage(message)
        }
        uiBinding.tagLocationButton.isEnabled = false    // Enable on get EPC
    }

    private fun sendMessage(message: String) {
        try {
            wsClient?.send(message)
        } catch (e: WebsocketNotConnectedException) {
            wsClient = activity?.let { it1 -> WSClient(uri, it1, this) }
            wsClient?.connect()
            wsClient?.send(message)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TestReaderFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TestReaderFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}