package org.kpu.tearch_cam


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*

import android.media.Image
import android.media.ImageReader
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size

import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.kpu.tearch_cam.utils.Constants.TAG
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Thread.sleep
import kotlin.collections.forEach as forEach1

class CameraActivity : AppCompatActivity() {
    private val  REQUEST_CAMERA_PERMISSION = 1001
    private var texture : SurfaceTexture? = null
    private var surface : Surface? = null
    private var cameraId : String? = null
    private var mTextureView : TextureView? = null
    private var mCameraSession : CameraCaptureSession? = null
    private var cameraDevice : CameraDevice? = null
    private var captureRequestBuilder : CaptureRequest.Builder? = null
    private var imageDimension : Size? = null
    private var imageReader : ImageReader? = null
    private var storage : FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef : StorageReference = storage.getReference()
    private var cafeNum : Int = 1
    var btn : Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)


        Log.d(TAG, "CameraActivity - onCreate() called")

        mTextureView = findViewById<TextureView>(R.id.mTextureView)

        val intent = intent
        cafeNum = intent.getIntExtra("cafeNum", 1)

        val folder = storageRef.child(cafeNum.toString())

        folder.listAll().addOnSuccessListener{
            val items = it.items
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                items.forEach(){
                    it.delete()     //폴더 안 파일 하나하나 지우는 과정
                }
            }
            Log.d(TAG, "Firebase Storage ${cafeNum.toString()} folder delete()")

        }

        mTextureView?.surfaceTextureListener = textureListener

        btn = findViewById<Button>(R.id.btn)
        btn?.setOnClickListener {
            takePicture()
        }

        val btn2 = findViewById<Button>(R.id.btn2)
        btn2.setOnClickListener {
            finish()
        }


    }




    private var textureListener : TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "State callback : onSurfaceTextureAvailable()")
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

        }

    }

    private fun openCamera(){
        Log.e(TAG, "openCamera() 메서드가 호출되었음")

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try{
            //CameraManager에서 cameraIdList의 값을 가져온다
            cameraId = manager.cameraIdList[0]

            val charateristics = manager.getCameraCharacteristics(cameraId!!)
            val map = charateristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            //SurfaceTexture에 사용할 Size 값을 map에서 가져와 imageDimension에 할당해준다
            imageDimension = map!!.getOutputSizes<SurfaceTexture>(SurfaceTexture::class.java)[0]

            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED // 카메라 권한없음
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) { // 쓰기권한 없음
                // 카메라 권한이 없는 경우 권한을 요청한다
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CAMERA_PERMISSION)
                return
            }

            //CameraManager.openCamera() 메서드를 이용해 인자로 넘겨준 cameraId의 카메라를 실행한다
            //이때, stateCallback은 카메라를 실행할 때 호출되는 콜백메서드이며, cameraDevice에 값을 할당해주고, 카메라 미리보기를 생성
            manager.openCamera(cameraId!!, stateCallback,null)      //permission check를 해야하는데 tedpermission으론 안됨

        }catch(e: CameraAccessException){
            e.printStackTrace()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "stateCallback : onOpened()")

            //cameraDevice 에 값을 할당해주고 카메라 비기를 시작
            //나중에 cameraDevice 리소스를 해지할 때 해당 cameraDevice 객체의 참조가 필요하므로
            //인자로 들어온 camera 값을 전역변수 cameraDevice에 넣어준다
            cameraDevice = camera

            //createCameraPreview() 메서드로 카메라 미리보기를 생성해준다
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "stateCallback : Disconnected()")

            //연결이 해제되면 cameraDevice를 닫아준다
            cameraDevice!!.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, "stateCallback: onError() : ${error.toString()}")

            //에러가 뜨면, cameraDevice를 닫고, 전역변수 cameraDevice에 null값을 할당해 준다
            cameraDevice!!.close()
            cameraDevice = null
        }

    }

    private fun createCameraPreviewSession(){
        Log.d(TAG, "createCameraPreviewSession() called")
        try{
            //캡쳐세션을 만들기 전에 프리뷰를 위한 Surface를 준비
            texture = mTextureView?.surfaceTexture

            //미리보기를 위한 surface 기본 버퍼의 크기는 카메라 미리보기 크기로 구성
            texture?.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)

            //미리보기를 시작하기 위해 필요한 출력표면인 surface
            surface = Surface(texture)

            //미리보기 화면을 요청하는 RequestBuilder를 만들어줌
            //이 요청은 위에서 만든 surfcae를 타겟으로 함
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface!!)

            //위에서 만든 surfcae에 미리보기를 보여주기 위해 createCaptureSession() 메서드를 시작한다
            //createCaptureSession 의 콜백메서드를 통해 onConfigured 상태가 확인되면
            //CameraCaptureSession을 통해 미리보기를 보여주기 시작한다
            cameraDevice!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback(){
                override fun onConfigured(session: CameraCaptureSession) {
                    if(cameraDevice == null){
                        return
                    }

                    mCameraSession = session

                    captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

                    try{
                        mCameraSession?.setRepeatingRequest(captureRequestBuilder!!.build(), null, null)
                    }catch(e: CameraAccessException){
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.d(TAG, "Configuration change")
                }

            }, null)

        }catch (e: CameraAccessException){
            e.printStackTrace()
        }
    }

    private fun takePicture(){
        Log.d(TAG, "takePicture() called")

        try{
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
            var jpegSizes : Array<Size>? = null
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG)

            var width = jpegSizes[0].width
            var height = jpegSizes[0].height

            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)

            val outputSurface = ArrayList<Surface>(2)
            outputSurface.add(imageReader!!.surface)
            outputSurface.add(Surface(mTextureView!!.surfaceTexture))

            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)

            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            /*
            //사진의 rotation을 설정
            val displayManager = getSystemService<DisplayManager>()!!
            val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val rotation = defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))
*/
            val readerListener = object : ImageReader.OnImageAvailableListener{
                override fun onImageAvailable(reader: ImageReader?) {
                    Log.d(TAG, "State callback : ImageAvailable()")

                    var image : Image? = null
                    var pathRef : StorageReference? = null
                    try{
                        image = imageReader!!.acquireLatestImage()

                        val buffer = image!!.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)

                        val rotateBytes = rotateImg(bytes)

                        val imageName = "Taver_" + System.currentTimeMillis() + ".jpg"
                        pathRef  = storageRef.child(cafeNum.toString()).child(imageName)

                        pathRef.putBytes(rotateBytes).addOnSuccessListener {
                            Log.d(TAG, "CameraActivity - firebase upload success")
                            Toast.makeText(this@CameraActivity, "파이어베이스 업로드 완료", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener{
                            Log.d(TAG, "CameraActivity - firebase upload fail")
                            Toast.makeText(this@CameraActivity, "파이어베이스 업로드 실패", Toast.LENGTH_SHORT).show()
                        }

                    }catch(e:Exception){
                        e.printStackTrace()
                    }finally {
                        sleep(30000L)

                        pathRef?.delete()
                        Log.d(TAG, "Firebase image delete()")

                        btn?.performClick() //takepicture() 강제 실행
                    }

                }

            }
            imageReader!!.setOnImageAvailableListener(readerListener, null)

            val captureListener = object: CameraCaptureSession.CaptureCallback(){
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                   // createCameraPreviewSession()      //강제 자동 촬영 시 오류로 인해 textureview repeat은 건너뜀
                }
            }

            //outputSurface위에서 만든 captureListener를 달아, 캡쳐(사진 찍기) 해주고 나서 카메라 미리보기 세션을 재시작한다
            cameraDevice!!.createCaptureSession(outputSurface, object : CameraCaptureSession.StateCallback(){
                override fun onConfigureFailed(session: CameraCaptureSession) {

                }

                override fun onConfigured(session: CameraCaptureSession) {
                    try{
                        Log.d(TAG, "State Callback : onConfigured()")
                        session.capture(captureBuilder.build(), captureListener, null)
                    }catch(e: CameraAccessException){
                        e.printStackTrace()
                    }
                }
            }, null)

        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    private fun rotateImg(bytes: ByteArray) : ByteArray{
        Log.d(TAG, "rotateImg() called")
        val bitmapImg : Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        val matrix : Matrix = Matrix()
        matrix.postRotate(90F)
        val rotatedBitmap = Bitmap.createBitmap(bitmapImg, 0, 0, bitmapImg.width, bitmapImg.height, matrix, true)

        val stream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)

        return stream.toByteArray()
    }

}






