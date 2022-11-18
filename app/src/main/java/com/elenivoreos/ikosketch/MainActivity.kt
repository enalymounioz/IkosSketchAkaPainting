package com.elenivoreos.ikosketch

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
            if(result.resultCode == RESULT_OK && result.data != null) {
                val imageBackGround : ImageView = findViewById( R.id.imageView_background)
                imageBackGround.setImageURI(result.data?.data)
            }
}

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranded = it.value

                if (isGranded) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                    ).show()

                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                } else {
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
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton


        val imageButtonBrush: ImageButton = findViewById(R.id.imageButton_brush)
        imageButtonBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val imageButtonUndo: ImageButton = findViewById(R.id.imageButton_undo)
        imageButtonUndo.setOnClickListener {
            drawingView?.onClickUndo()

        }

        val imageButtonRedo: ImageButton = findViewById(R.id.imageButton_redo)
        imageButtonRedo.setOnClickListener {
            drawingView?.onClickRedo()

        }

        val imageButtonGallery: ImageButton = findViewById(R.id.imageButton_gallery)
        imageButtonGallery.setOnClickListener {
            requestStoragePermission()
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
            drawingView!!.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        })
        val mediumButton: ImageButton = brushDialog.findViewById(R.id.imageButton_medium_brush)
        mediumButton.setOnClickListener(View.OnClickListener {
            drawingView!!.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        })

        val largeButton: ImageButton = brushDialog.findViewById(R.id.imageButton_large_brush)
        largeButton.setOnClickListener(View.OnClickListener {
            drawingView!!.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        })

        brushDialog.show()

    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

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

            mImageButtonCurrentPaint = view


        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog(
                "Ikos Drawing App", "Ikos Drawing App " +
                        "needs to Access Your External Storage")

        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                    //TODO - Add writing external storage permission
                    ))
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
}
