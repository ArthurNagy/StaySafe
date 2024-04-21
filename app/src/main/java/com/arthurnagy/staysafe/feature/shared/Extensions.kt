package com.arthurnagy.staysafe.feature.shared

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.arthurnagy.staysafe.R
import com.arthurnagy.staysafe.core.model.Motive
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

@ColorInt
fun Context.color(@ColorRes colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

fun Context.drawable(@DrawableRes drawableRes: Int): Drawable? = ContextCompat.getDrawable(this, drawableRes)

fun Context.dimension(@DimenRes dimenRes: Int): Float = resources.getDimension(dimenRes)

@Px
fun Context.dimensionPixel(@DimenRes dimension: Int): Int = resources.getDimensionPixelSize(dimension)

fun ConstraintLayout.updateConstraintSet(updateConstraints: ConstraintSet.() -> Unit) {
    with(ConstraintSet()) {
        clone(this@updateConstraintSet)
        updateConstraints()
        applyTo(this@updateConstraintSet)
    }
}

fun Bitmap.tint(@ColorInt tintColor: Int): Bitmap {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
    val bitmapResult = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmapResult)
    canvas.drawBitmap(this, 0F, 0F, paint)
    return bitmapResult
}

val Motive.labelRes: Int
    get() = when (this) {
        Motive.PROFESSIONAL_INTERESTS -> R.string.motive_professional_interests
        Motive.MEDICAL_ASSISTANCE -> R.string.motive_medical_assistance
        Motive.PURCHASE_OF_MEDICATION -> R.string.motive_purchase_of_medication
        Motive.MOTIVE_HELP -> R.string.motive_help
        Motive.MOTIVE_FAMILY_DECEASE -> R.string.motive_family_decease
    }

inline fun <T> LiveData<T>.doOnChanged(crossinline observer: (T) -> Unit) {
    observeForever(object : Observer<T> {
        override fun onChanged(t: T) {
            observer(t)
            this@doOnChanged.removeObserver(this)
        }
    })
}

fun <T1, T2, T3, R> combine(first: LiveData<T1>, second: LiveData<T2>, third: LiveData<T3>, func: (first: T1, second: T2, third: T3) -> R): LiveData<R> =
    MediatorLiveData<R>().apply {
        var lastValueT1: T1? = null
        var lastValueT2: T2? = null
        var lastValueT3: T3? = null
        fun combineData(first: T1?, second: T2?, third: T3?) {
            if (first != null && second != null && third != null) value = func(first, second, third)
        }
        addSource(first) {
            lastValueT1 = it
            combineData(lastValueT1, lastValueT2, lastValueT3)
        }
        addSource(second) {
            lastValueT2 = it
            combineData(lastValueT1, lastValueT2, lastValueT3)
        }
        addSource(third) {
            lastValueT3 = it
            combineData(lastValueT1, lastValueT2, lastValueT3)
        }
    }

fun Fragment.addPageChangeListenerTo(
    viewPager: ViewPager,
    onPageSelected: ((position: Int) -> Unit)? = null,
    onPageScrolled: ((position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit)? = null
) {
    val pageChangeListener = LifecyclePageChangeListener(viewPager, onPageSelected, onPageScrolled)
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            viewLifecycleOwnerLiveData.observe(this@addPageChangeListenerTo) { viewLifecycleOwner ->
                viewLifecycleOwner?.lifecycle?.addObserver(pageChangeListener)
            }
        }
    })
}

class LifecyclePageChangeListener(
    private val viewPager: ViewPager,
    private val onPageSelected: ((position: Int) -> Unit)? = null,
    private val onPageScrolled: ((position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit)? = null,
    private val onPageScrollStateChanged: ((state: Int) -> Unit)? = null
) : DefaultLifecycleObserver {

    private val pageChangeCallback: ViewPager.SimpleOnPageChangeListener

    init {
        pageChangeCallback = object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                this@LifecyclePageChangeListener.onPageSelected?.invoke(position)
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                this@LifecyclePageChangeListener.onPageScrolled?.invoke(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageScrollStateChanged(state: Int) {
                this@LifecyclePageChangeListener.onPageScrollStateChanged?.invoke(state)
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        viewPager.addOnPageChangeListener(pageChangeCallback)
    }

    override fun onPause(owner: LifecycleOwner) {
        viewPager.removeOnPageChangeListener(pageChangeCallback)
    }
}

fun Shader.setTranslation(x: Float = 0f, y: Float = 0f) {
    getLocalMatrix(matrix)
    matrix.setTranslate(x, y)
    setLocalMatrix(matrix)
}

private val matrix: Matrix by lazy(LazyThreadSafetyMode.NONE) {
    Matrix()
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun ExtendedFloatingActionButton.tintExtendedFloatingActionButton() {
    val colorOnSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)
    val colorSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface)
    val colorSecondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary)

    val backgroundColorStateList: ColorStateList by lazy {
        val backgroundColors = IntArray(buttonBackgroundStates.size)
        backgroundColors[0] = colorSecondary
        backgroundColors[1] =
            MaterialColors.layer(colorSurface, MaterialColors.compositeARGBWithAlpha(colorOnSurface, MaterialColors.ALPHA_DISABLED_LOW.toAlphaInt()))

        ColorStateList(buttonBackgroundStates, backgroundColors)
    }


    ViewCompat.setBackgroundTintList(this, backgroundColorStateList)
}

private const val MAX_ALPHA = 255

fun Float.toAlphaInt(): Int = (this * MAX_ALPHA).toInt()

val buttonBackgroundStates = arrayOf(
    intArrayOf(android.R.attr.state_enabled),
    intArrayOf()
)