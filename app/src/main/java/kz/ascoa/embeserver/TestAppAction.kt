package kz.ascoa.embeserver

class TestAppAction {

    fun runTest() {
        //    private fun initWSClient(uriP: URI?) {
//
//        var uri = uriP
//
//        if(uri == null) {
//            uri = try {
//                URI("ws://127.0.0.1:38301/")
//            } catch (e: URISyntaxException) {
//                e.printStackTrace()
//                return
//            }
//        }
//
//        mWebSocketClient = object : WebSocketClient(uri) {
//            override fun onOpen(serverHandshake: ServerHandshake) {
//                runOnUiThread{
//                    Toast.makeText(activityContext, "WS Opened", Toast.LENGTH_SHORT).show()
//                }
//                mWebSocketClient!!.send("test")
//            }
//
//            override fun onMessage(s: String) {
//                runOnUiThread {
//                    Toast.makeText(activityContext, s, Toast.LENGTH_LONG).show()
//
//                }
//            }
//
//            override fun onClose(i: Int, s: String, b: Boolean) {
//                Log.i("Websocket", "Closed $s")
//            }
//
//            override fun onError(e: java.lang.Exception) {
//                Log.i("Websocket", "Error " + e.message)
//            }
//        }
//    }

//    private fun connectWebSocket() {
//
//
//        try {
//            if(testWebSocketClient.isOpen != true) {
//                testWebSocketClient.connect()
//            }
//            testWebSocketClient.send("test")
//
//            // mWebSocketClient?.connect()
//
//        } catch (e: Exception){
//            Toast.makeText(activityContext, "${e.stackTrace.toString()}", Toast.LENGTH_LONG).show()
//        }
//    }

        // Copied from example. Seems it is not used in this class but used in Activity which is used in the example. Delete?
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String?>?,
//        grantResults: IntArray?
//    ) {
//        if (requestCode == REQUEST_READ_PHONE_STATE) {
//            initView()
//        }
//        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults!!)
//    }
//    }

//class EmptyClient : WebSocketClient {
//    constructor(serverUri: URI?, draft: Draft?) : super(serverUri, draft) {}
//    constructor(serverURI: URI?) : super(serverURI) {}
//
//    override fun onOpen(handshakedata: ServerHandshake) {
//        try {
//            this.send("Hello, WS World!");
//        } catch (e: Exception) {
//            System.out.println(e.stackTrace)
//        }
//    }
//
//    override fun onClose(code: Int, reason: String, remote: Boolean) {
//        println("closed with exit code $code additional info: $reason")
//    }
//
//    override fun onMessage(message: String) {
//        println("received message: $message")
//    }
//
//    override fun onError(ex: Exception) {
//        System.err.println("an error occurred:$ex")
//    }
//}
    }

}