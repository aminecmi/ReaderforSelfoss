package apps.amine.bou.readerforselfoss.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import apps.amine.bou.readerforselfoss.R
import kotlinx.android.synthetic.main.fragment_article.view.webcontent

class ImageFragment : Fragment() {

    private lateinit var position: Number
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
        view.webcontent.loadUrl(allImages[0])

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
                menu.clear()
    }

    override fun onDestroy() {
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(true)
        super.onDestroy()
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