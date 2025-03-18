package com.bailout.stickk.ubi4.adapters.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.local.CollectionGesturesProvider
import com.bailout.stickk.ubi4.data.local.SprGestureItemsProvider
import com.bailout.stickk.ubi4.resources.AndroidResourceProvider

class SelectedGesturesAdapter(
    private var selectedGesturesList: MutableList<kotlin.Pair<Int, Int>>,
    private val onCheckGestureSprListener: OnCheckSprGestureListener,
    private val onDotsClickListener: (Int) -> Unit
) : RecyclerView.Adapter<SelectedGesturesAdapter.SprGesturesViewHolder>() {
    private lateinit var myContext: Context
    inner class SprGesturesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gestureName: TextView = view.findViewById(R.id.gestureNumber)
        var gestureAnimation: LottieAnimationView = view.findViewById(R.id.lottieAnimationGesture)
        val dotsThreeBtnSpr: ImageView = itemView.findViewById(R.id.dotsThreeBtnSpr)
    }

    private val collectionGesturesProvider: CollectionGesturesProvider by lazy {
        CollectionGesturesProvider(AndroidResourceProvider(myContext))
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SprGesturesViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_gesture, parent, false)
        myContext = parent.context
        return SprGesturesViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return selectedGesturesList.size
    }

    override fun onBindViewHolder(holder: SprGesturesViewHolder, position: Int) {
        val bindingGesture = selectedGesturesList[position]
        //CollectionGesturesProvider.getGesture(listBindingGesture[selectedPosition].second).gestureName)
        holder.gestureName.text = collectionGesturesProvider.getGesture(bindingGesture.second).gestureName
        holder.gestureAnimation.setAnimation(SprGestureItemsProvider(AndroidResourceProvider(myContext)).getSprGesture(bindingGesture.first).animationId)
        holder.gestureAnimation.playAnimation()
        holder.dotsThreeBtnSpr.setOnClickListener {
            onDotsClickListener(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateGestures(newGestures: List<kotlin.Pair<Int, Int>>) {
        selectedGesturesList.clear()
        selectedGesturesList.addAll(newGestures)
        notifyDataSetChanged()
    }

    interface OnCheckSprGestureListener {
        fun onGestureSprClicked(position: Int, title: String)
    }

}
