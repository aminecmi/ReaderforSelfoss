package apps.amine.bou.readerforselfoss

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import apps.amine.bou.readerforselfoss.databinding.ActivityImageBinding
import apps.amine.bou.readerforselfoss.fragments.ImageFragment

class ImageActivity : AppCompatActivity() {
    private lateinit var allImages : ArrayList<String>
    private var position : Int = 0

    private lateinit var binding: ActivityImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        allImages = intent.getStringArrayListExtra("allImages") as ArrayList<String>
        position = intent.getIntExtra("position", 0)

        binding.pager.adapter = ScreenSlidePagerAdapter(supportFragmentManager)
        binding.pager.currentItem = position
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount(): Int {
            return allImages.size
        }

        override fun getItem(position: Int): ImageFragment {
            return ImageFragment.newInstance(allImages[position])
        }
    }
}