package apps.amine.bou.readerforselfoss.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.databinding.FragmentImageBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class ImageFragment : Fragment() {

    private lateinit var imageUrl : String
    private val glideOptions = RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageUrl = requireArguments().getString("imageUrl")!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        val view = binding?.root

        binding!!.photoView.visibility = View.VISIBLE
        Glide.with(activity)
                .asBitmap()
                .apply(glideOptions)
                .load(imageUrl)
                .into(binding!!.photoView)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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