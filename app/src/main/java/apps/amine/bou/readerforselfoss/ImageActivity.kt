package apps.amine.bou.readerforselfoss

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import apps.amine.bou.readerforselfoss.fragments.ImageFragment
import kotlinx.android.synthetic.main.activity_reader.*

class ImageActivity : AppCompatActivity() {
    private lateinit var allImages : ArrayList<String>
    private var position : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image)

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        allImages = intent.getStringArrayListExtra("allImages") as ArrayList<String>
        position = intent.getIntExtra("position", 0)

        pager.adapter = ScreenSlidePagerAdapter(supportFragmentManager)
        pager.currentItem = position
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