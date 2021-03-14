package apps.amine.bou.readerforselfoss.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import apps.amine.bou.readerforselfoss.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.fragment_image.view.*

class ImageFragment : Fragment() {

    private lateinit var imageUrl : String
    private val glideOptions = RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageUrl = requireArguments().getString("imageUrl")!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view : View = inflater.inflate(R.layout.fragment_image, container, false)

        view.photoView.visibility = View.VISIBLE
        Glide.with(activity)
                .asBitmap()
                .apply(glideOptions)
                .load(imageUrl)
                .into(view.photoView)

        return view
    }

    companion object {
        private const val ARG_IMAGE = "imageUrl"

        fun newInstance(
                imageUrl : String
        ): ImageFragment {
            val fragment = ImageFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE, imageUrl)
            fragment.arguments = args
            return fragment
        }
    }
}