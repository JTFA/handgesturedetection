package dd.sample.opencv

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager.LayoutParams.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dd.sample.opencv.colorblobdetection.ColorBlobDetector
import kotlinx.android.synthetic.main.main_surface_view.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*


class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2,
    OnTouchListener {

    private val REQUEST_CODE_PERMISSIONS = 127
    private val REQUIRED_PERMISSIONS = arrayOf(
        CAMERA,
        WRITE_EXTERNAL_STORAGE
    )
    // openCV callback
    lateinit var cvBaseLoaderCallback: BaseLoaderCallback
    // image storage
    lateinit var imageMat: Mat

    companion object {

        val TAG = "MYLOG " + MainActivity::class.java.simpleName
        fun lgd(s: String) = Log.d(TAG, s)
        fun lge(s: String) = Log.e(TAG, s)
        fun lgi(s: String) = Log.i(TAG, s)

        fun shortMsg(context: Context, s: String) =
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()

        // messages:
        private const val OPENCV_SUCCESSFUL = "OpenCV Loaded Successfully!"
        private const val OPENCV_FAIL = "Could not load OpenCV!!!"
        private const val OPENCV_PROBLEM = "There's a problem in OpenCV."
        private const val PERMISSION_NOT_GRANTED = "Permissions not granted by the user."

    }

    private var mRgba: Mat? = null
//    private var mGray: Mat? = null


//    private val main_surface_view: CustomSufaceView? = null
//    private val numberOfFingersText: TextView? = null
    var iThreshold = 0.0

    private var mBlobColorHsv: Scalar? = null
    private var mBlobColorRgba: Scalar? = null
    private var mDetector: ColorBlobDetector? = null
    private var mSpectrum: Mat? = null
    private var mIsColorSelected = false

    private var SPECTRUM_SIZE: Size? = null
    private var CONTOUR_COLOR: Scalar? = null
    private var CONTOUR_COLOR_WHITE: Scalar? = null

    val mHandler = Handler()
    var noOfFingers = 0
    val mUpdateFingerCountResults = Runnable { updateNumberOfFingers() }


    fun updateNumberOfFingers() {
        numberOfFingers.text = noOfFingers.toString()
    }

    private fun converScalarHsv2Rgba(hsvColor: Scalar): Scalar? {
        val pointMatRgba = Mat()
        val pointMatHsv = Mat(1, 1, CvType.CV_8UC3, hsvColor)
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4)
        return Scalar(pointMatRgba[0, 0])
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(FLAG_FORCE_NOT_FULLSCREEN)
        window.setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN)
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.main_surface_view)

        // Request camera permissions
        if (allPermissionsGranted()) {
            checkOpenCV(this)
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS )
        }

        main_surface_view.visibility = SurfaceView.VISIBLE
        main_surface_view.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT)
        main_surface_view.setCvCameraViewListener(this)

        cvBaseLoaderCallback = object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {

                when (status) {
                    SUCCESS -> {
//                        lgi(OPENCV_SUCCESSFUL)
//                        shortMsg(this@MainActivity, OPENCV_SUCCESSFUL)
                        main_surface_view.enableView()
                        main_surface_view.setOnTouchListener(this@MainActivity)
                    }

                    else -> super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            cvBaseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            shortMsg(this,"There is a problem with openCV")
        }
    }

    override fun onPause() {
        super.onPause()
        main_surface_view?.let { main_surface_view.disableView() }
    }

    override fun onDestroy() {
        super.onDestroy()
        main_surface_view?.let { main_surface_view.disableView() }
    }

    private fun checkOpenCV(context: Context) {

        if (OpenCVLoader.initDebug()) {
            shortMsg(context, OPENCV_SUCCESSFUL)
            lgi("OpenCV started...")
        } else {
            shortMsg(context, OPENCV_FAIL)
            lge(OPENCV_FAIL)
        }
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                checkOpenCV(this)
            } else {
                shortMsg(this, PERMISSION_NOT_GRANTED)
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
//        imageMat = Mat(width, height, CvType.CV_8UC4)

//        mGray = Mat()
        mRgba = Mat(width, height, CvType.CV_8UC4)

        mDetector = ColorBlobDetector()
        mSpectrum = Mat()
        mBlobColorRgba = Scalar(255.toDouble())
        mBlobColorHsv = Scalar(255.toDouble())
        SPECTRUM_SIZE = Size(200.toDouble(), 64.toDouble())
        CONTOUR_COLOR = Scalar(255.toDouble(), 0.toDouble(), 0.toDouble(), 255.toDouble())
        CONTOUR_COLOR_WHITE = Scalar(255.toDouble(), 255.toDouble(), 255.toDouble(), 255.toDouble())
    }

    override fun onCameraViewStopped() {
//        imageMat.release()

//        mGray!!.release()
        mRgba!!.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        /*imageMat = inputFrame!!.rgba()
        return imageMat*/

        try {
            mRgba = inputFrame!!.rgba()
//        mGray = inputFrame.gray()

            Imgproc.medianBlur(mRgba, mRgba, 3)
            // Imgproc.GaussianBlur(inputFrame.gray(), mIntermediateMat, new org.opencv.core.Size(3, 3), 1 ,  1);
            //Imgproc.threshold(mIntermediateMat, mRgba, 181, 255, Imgproc.THRESH_BINARY);
            if (!mIsColorSelected) return mRgba!!

            val contours = mDetector!!.contours
            mDetector!!.process(mRgba)

            Log.d(TAG, "Contours count: " + contours.size)

            if (contours.size <= 0) {
                return mRgba!!
            }

            var rect = Imgproc.minAreaRect(MatOfPoint2f(*contours[0].toArray()))

            var boundWidth = rect.size.width
            var boundHeight = rect.size.height
            var boundPos = 0

            for (i in 1 until contours.size) {
                rect = Imgproc.minAreaRect(MatOfPoint2f(*contours[i].toArray()))
                if (rect.size.width * rect.size.height > boundWidth * boundHeight) {
                    boundWidth = rect.size.width
                    boundHeight = rect.size.height
                    boundPos = i
                }
            }

            val boundRect =
                Imgproc.boundingRect(MatOfPoint(*contours[boundPos].toArray()))

            Imgproc.rectangle(mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR_WHITE, 2, 8, 0)

            Log.d(TAG,
                " Row start [" +
                        boundRect.tl().y.toInt() + "] row end [" +
                        boundRect.br().y.toInt() + "] Col start [" +
                        boundRect.tl().x.toInt() + "] Col end [" +
                        boundRect.br().x.toInt() + "]"
            )

            var a = boundRect.br().y - boundRect.tl().y
            a = a * 0.7
            a = boundRect.tl().y + a

            Log.d(TAG,
                " A [" + a + "] br y - tl y = [" + (boundRect.br().y - boundRect.tl().y) + "]"
            )

            //Core.rectangle( mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR, 2, 8, 0 );
            Imgproc.rectangle(
                mRgba,
                boundRect.tl(),
                Point(boundRect.br().x, a),
                CONTOUR_COLOR,
                2,
                8,
                0
            )

            val pointMat = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contours[boundPos].toArray()), pointMat, 3.0, true)
            contours[boundPos] = MatOfPoint(*pointMat.toArray())

            val hull = MatOfInt()
            val convexDefect = MatOfInt4()
            Imgproc.convexHull(MatOfPoint(*contours[boundPos].toArray()), hull)

            if (hull.toArray().size < 3) return mRgba!!

            Imgproc.convexityDefects(MatOfPoint(*contours[boundPos].toArray()), hull, convexDefect)

            val hullPoints: MutableList<MatOfPoint> = LinkedList()
            val listPo: MutableList<Point> = LinkedList()

            for (j in hull.toList().indices) {
                listPo.add(contours[boundPos].toList()[hull.toList()[j]])
            }

            val e = MatOfPoint()
            e.fromList(listPo)
            hullPoints.add(e)

            val defectPoints: MutableList<MatOfPoint> = LinkedList()
            val listPoDefect: MutableList<Point> =LinkedList()

            for(j in 0..convexDefect.toList().lastIndex step 4){
                val farPoint =
                    contours[boundPos].toList()[convexDefect.toList()[j + 2]]
                val depth = convexDefect.toList()[j + 3]
                if (depth > iThreshold && farPoint.y < a) {
                    listPoDefect.add(
                        contours[boundPos].toList()[convexDefect.toList()[j + 2]]
                    )
                }
                Log.d(TAG,
                    "defects [" + j + "] " + convexDefect.toList()[j + 3]
                )
            }

            val e2 = MatOfPoint()
            e2.fromList(listPo)
            defectPoints.add(e2)

            Log.d(TAG, "hull: " + hull.toList())
            Log.d(TAG, "defects: " + convexDefect.toList())

            Imgproc.drawContours(mRgba, hullPoints, -1, CONTOUR_COLOR, 3)

            val defectsTotal = convexDefect.total().toInt()
            Log.d(TAG, "Defect total $defectsTotal")

            this.noOfFingers = listPoDefect.size
            if (this.noOfFingers > 5) this.noOfFingers = 5

            mHandler.post(mUpdateFingerCountResults)

            for (p in listPoDefect) {
                Imgproc.circle(mRgba, p, 6, Scalar(255.toDouble(), 0.toDouble(), 255.toDouble()))
            }
        }
        catch (e:Exception)
        {
            Log.d(TAG, e.localizedMessage)
        }

        return mRgba!!
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        try {
            val cols = mRgba!!.cols()
            val rows = mRgba!!.rows()

            val xOffset = (main_surface_view!!.width - cols) / 2
            val yOffset = (main_surface_view!!.height - rows) / 2

            val x = event!!.x.toInt() - xOffset
            val y = event.y.toInt() - yOffset

            Log.i(TAG, "Touch image coordinates: ($x, $y)")

            if (x < 0 || y < 0 || x > cols || y > rows) return false

            val touchedRect = Rect()

            touchedRect.x = if (x > 5) x - 5 else 0
            touchedRect.y = if (y > 5) y - 5 else 0

            touchedRect.width = if (x + 5 < cols) x + 5 - touchedRect.x else cols - touchedRect.x
            touchedRect.height = if (y + 5 < rows) y + 5 - touchedRect.y else rows - touchedRect.y

            val touchedRegionRgba = mRgba!!.submat(touchedRect)

            val touchedRegionHsv = Mat()
            Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL)

            // Calculate average color of touched region

            mBlobColorHsv = Core.sumElems(touchedRegionHsv)
            val pointCount = touchedRect.width * touchedRect.height

            for (i in 0..mBlobColorHsv?.`val`?.lastIndex!!){
                mBlobColorHsv!!.`val`[i] /= pointCount.toDouble()
            }
            mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv!!)

            Log.i(TAG,
                "Touched rgba color: (" + mBlobColorRgba!!.`val`[0] + ", " + mBlobColorRgba!!.`val`[1] +
                        ", " + mBlobColorRgba!!.`val`[2] + ", " + mBlobColorRgba!!.`val`[3] + ")"
            )

            mDetector?.setHsvColor(mBlobColorHsv)

            Imgproc.resize(mDetector!!.spectrum, mSpectrum, SPECTRUM_SIZE)

            mIsColorSelected = true

            touchedRegionRgba.release()
            touchedRegionHsv.release()
        }
        catch (e:Exception){
            Log.d(TAG, e.localizedMessage)
        }

        return false // don't need subsequent touch events

    }

}