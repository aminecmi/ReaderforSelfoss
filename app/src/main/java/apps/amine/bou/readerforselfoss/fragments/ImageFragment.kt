package apps.amine.bou.readerforselfoss.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import apps.amine.bou.readerforselfoss.R
import kotlinx.android.synthetic.main.fragment_article.view.webcontent
import kotlin.math.abs

class ImageFragment : Fragment() {

    private var position: Int = 0
    private lateinit var allImages: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        position = arguments!!.getInt("position")
        allImages = arguments!!.getStringArrayList("allImages")

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        val view : View = inflater.inflate(R.layout.fragment_image, container, false)

        view.webcontent.visibility = View.VISIBLE
        view.webcontent.settings.setLoadWithOverviewMode(true)
        view.webcontent.settings.setUseWideViewPort(true)
        view.webcontent.settings.setSupportZoom(true)
        view.webcontent.settings.setBuiltInZoomControls(true)
        view.webcontent.settings.setDisplayZoomControls(false)

        val gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                val SWIPE_MIN_DISTANCE = 120
                val SWIPE_MAX_OFF_PATH = 250
                val SWIPE_THRESHOLD_VELOCITY = 200
                if (abs(e1!!.y - e2!!.y) > SWIPE_MAX_OFF_PATH)
                    return false
                if (e1.x - e2.x > SWIPE_MIN_DISTANCE
                        && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    changeImage(1)
                }
                else if (e2.x - e1.x > SWIPE_MIN_DISTANCE
                        && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    changeImage(-1)
                }

                return super.onFling(e1, e2, velocityX, velocityY);
            }
        })
        view.webcontent.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event)}

        view.webcontent.loadUrl(allImages[position])

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
                menu.clear()
    }

    override fun onDestroy() {
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(true)
        super.onDestroy()
    }

    fun changeImage(change : Int) {
        position += change
        if (position < 0 || position >= allImages.size) {
            position -= change
        }
        else {
            view!!.webcontent.loadUrl(allImages[position])
        }
    }

    companion object {
        private const val ARG_POSITION = "position"
        private const val ARG_IMAGES = "allImages"

        fun newInstance(
                position: Int,
                allImages: ArrayList<String>
        ): ImageFragment {
            val fragment = ImageFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            args.putStringArrayList(ARG_IMAGES, allImages)
            fragment.arguments = args
            return fragment
        }
    }
}