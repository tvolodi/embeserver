package kz.ascoa.embeserver

import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog


    fun showAlert(context: Context, message: String?) {
        try {
            // Create the object of AlertDialog Builder class
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)

            // Set the message show for the Alert time
            builder.setMessage(message)

            // Set Alert Title
            builder.setTitle("Alert!")

            // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
            builder.setCancelable(false)

            // Set the positive button with yes name Lambda OnClickListener method is use of DialogInterface interface.
            builder.setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    // When the user click yes button then app will close
                    dialog?.cancel();
                } as DialogInterface.OnClickListener)


            // Create the Alert dialog
            val alertDialog: AlertDialog = builder.create()
            // Show the Alert Dialog box
            alertDialog.getWindow()?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL);
            alertDialog.show()
        } catch (e: Exception) {
            Log.i("Alert", "${e.message}")
        }

    }
