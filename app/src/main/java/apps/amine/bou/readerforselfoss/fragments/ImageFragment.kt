package apps.amine.bou.readerforselfoss.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import kotlinx.android.synthetic.main.fragment_article.*
import kotlinx.android.synthetic.main.fragment_article.view.*
import retrofit2.http.Url

class ImageFragment : Fragment() {

    private lateinit var position: Number
    private lateinit var allImages: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        position = arguments!!.getInt("position")
        allImages = arguments!!.getStringArrayList("allImages")

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view : View = inflater.inflate(R.layout.fragment_image, container, false)

        view.webcontent.visibility = View.VISIBLE
        view.webcontent.loadUrl(allImages[0])

        return view
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