package com.elenivoreos.ikosketch

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.elenivoreos.ikosketch.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private var mImageButtonCurrentPaint: ImageButton? =
        null // A variable for current color is picked from color pallet

    //Todo 1: create a variable for the dialog
    var customProgressDialog: Dialog? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackGround: ImageView = findViewById(R.id.imageView_background)

                imageBackGround.setImageURI(result.data?.data)
            }
        }

    /** create an ActivityResultLauncher with MultiplePermissions since we are requesting
     * both read and write
     */
    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranded = it.value
                //if permission is granted show a toast and perform operation
                if (isGranded) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                    ).show()

                    val pickIntent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    openGalleryLauncher.launch(pickIntent)
                } else {
                    //Displaying another toast if permission is not granted and this time focus on
                    //    Read external storage
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this@MainActivity,
                            "Oops you just denied the permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.drawingView.setSizeForBrush(20.toFloat())

        binding.imageButtonBrush.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_selected
            )
        )

        binding.imageButtonBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }
        val imageButtonGallery: ImageButton = findViewById(R.id.imageButton_gallery)
        imageButtonGallery.setOnClickListener {
            requestStoragePermission()
        }

        val imageButtonUndo: ImageButton = findViewById(R.id.imageButton_undo)
        imageButtonUndo.setOnClickListener {
            // This is for undo recent stroke.
            binding.drawingView.onClickUndo()

        }

        val imageButtonRedo: ImageButton = findViewById(R.id.imageButton_redo)
        imageButtonRedo.setOnClickListener {
            binding.drawingView.onClickRedo()

        }



        val imageButtonSave: ImageButton = findViewById(R.id.imageButton_save)
        //set onclick listener
        imageButtonSave.setOnClickListener {
            //check if permission is allowed
            if (isReadStorageAllowed()) {
                //Todo 4: show dialog before launching coroutine
                showProgressDialog()
                //launch a coroutine block
                lifecycleScope.launch {
                    //reference the frame layout
                    val frameLayoutDrawingView: FrameLayout =
                        findViewById(R.id.frameLayout_drawing_view_container)
                    //Save the image to the device
                    saveBitmapFile(getBitmapFromView(frameLayoutDrawingView))
                }
            }

        }

    }

    /**
     * Method is used to launch the dialog to select different brush sizes.
     */
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")

        val smallButton: ImageButton = brushDialog.findViewById(R.id.imageButton_small_brush)
        smallButton.setOnClickListener(View.OnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        })
        val mediumButton: ImageButton = brushDialog.findViewById(R.id.imageButton_medium_brush)
        mediumButton.setOnClickListener(View.OnClickListener {
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        })

        val largeButton: ImageButton = brushDialog.findViewById(R.id.imageButton_large_brush)
        largeButton.setOnClickListener(View.OnClickListener {
            binding.drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        })

        brushDialog.show()
    }
    /**
     * Method is called when color is clicked from pallet_normal.
     *
     * @param view ImageButton on which click took place.
     */
    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            // Update the color
            val imageButton = view as ImageButton
            // Here the tag is used for swaping the current color with previous color.
            // The tag stores the selected view
            val colorTag = imageButton.tag.toString()
            // The color is set as per the selected tag here.
            binding.drawingView.setColor(colorTag)
        // Swap the backgrounds for last active and currently active image button.
            imageButton!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this, R.drawable.pallet_selected
                )
            )
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(
                    this, R.drawable.pallet_normal
                )
            )
            //Current view is updated with selected view in the form of ImageButton.
            mImageButtonCurrentPaint = view
        }
    }
    /**
     * We are calling this method to check the permission status
     */
    private fun isReadStorageAllowed(): Boolean {
        //Getting the permission status
        // Here the checkSelfPermission is
        /**
         * Determine whether <em>you</em> have been granted a particular permission.
         *
         * @param permission The name of the permission being checked.
         *
         */
        val result = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        /**
         *
         * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
         * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
         *
         */
        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }
    //create a method to requestStorage permission
    private fun requestStoragePermission() {
        // Check if the permission was denied and show rationale
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            //call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog(
                "Ikos Drawing App", "Ikos Drawing App " +
                        "needs to Access Your External Storage"
            )

        } else {
            // You can directly ask for the permission.
            //if it has not been denied then request for permission
            //  The registered ActivityResultCallback gets the result of this request.
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    /**
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
    /**
     * Create bitmap from view and returns it
     */
    private fun getBitmapFromView(view: View): Bitmap {

        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(
            view.width,
            view.height, Bitmap.Config.ARGB_8888
        )
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val backgroundDrawable = view.background
        if (backgroundDrawable != null) {
            //has background drawable, then draw it on the canvas
            backgroundDrawable.draw(canvas)

        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()// Creates a new byte array output stream.
                    // The buffer capacity is initially 32 bytes, though its size increases if necessary.
                    mBitmap!!.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    /**
                     * Write a compressed version of the bitmap to the specified outputstream.
                     * If this returns true, the bitmap can be reconstructed by passing a
                     * corresponding inputstream to BitmapFactory.decodeStream(). Note: not
                     * all Formats support all bitmap configs directly, so it is possible that
                     * the returned bitmap from BitmapFactory could be in a different bitdepth,
                     * and/or may have lost per-pixel alpha (e.g. JPEG only supports opaque
                     * pixels).
                     *
                     * @param format   The format of the compressed image
                     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
                     *                 small size, 100 meaning compress for max quality. Some
                     *                 formats, like PNG which is lossless, will ignore the
                     *                 quality setting
                     * @param stream   The outputstream to write the compressed data.
                     * @return true if successfully compressed to the specified stream.
                     */


                    val file = File(
                        externalCacheDir?.absoluteFile.toString() +
                                File.separator + "IkosSketch_" +
                                System.currentTimeMillis() / 1000 + ".png"
                    )
                    // Here the Environment : Provides access to environment variables.
                    // getExternalStorageDirectory : returns the primary shared/external storage directory.
                    // absoluteFile : Returns the absolute form of this abstract pathname.
                    // File.separator : The system-dependent default name-separator character. This string contains a single character.

                    val fileOut = FileOutputStream(file) // Creates a file output stream to write to the file represented by the specified object.
                    fileOut.write(bytes.toByteArray())  // Writes bytes from the specified byte array to this file output stream.
                    fileOut.close() // Closes this file output stream and releases any system resources associated with this stream. This file output stream may no longer be used for writing bytes.

                    result = file.absolutePath
                    runOnUiThread {
                        ///Todo 5: cancel dialog after the file is saved
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully: $result",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }

            }
        }
        return result

    }

    /** Todo 2:create function to show the dialog
     * Method is used to show the Custom Progress Dialog.
     */
    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}
