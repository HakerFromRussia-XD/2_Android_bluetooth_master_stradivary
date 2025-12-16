package com.bailout.stickk.ubi4.adapters.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider

class SelectedGesturesAdapter(
    private var selectedGesturesList: MutableList<Pair<Int, Int>>,
    private val onCheckGestureSprListener: OnCheckSprGestureListener,
    private val onDotsClickListener: (Int) -> Unit
) : RecyclerView.Adapter<SelectedGesturesAdapter.SprGesturesViewHolder>() {

    private lateinit var myContext: Context
    private var activeGestureId: Int? = null

    inner class SprGesturesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ВАЖНО: именно этот квадрат с рамкой будем перекрашивать
        val gestureContainer: View = view.findViewById(R.id.gestureSprCollection1Btn)

        val gestureName: TextView = view.findViewById(R.id.gestureNumber)
        val gestureAnimation: LottieAnimationView = view.findViewById(R.id.lottieAnimationGesture)
        val dotsThreeBtnSpr: ImageView = view.findViewById(R.id.dotsThreeBtnSpr)
        val gestureSprTitleTop: TextView = view.findViewById(R.id.gestureSprTitleTop)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SprGesturesViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_gesture, parent, false)
        myContext = parent.context
        return SprGesturesViewHolder(itemView)
    }

    override fun getItemCount(): Int = selectedGesturesList.size

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: SprGesturesViewHolder, position: Int) {
        val (sprId, gestureId) = selectedGesturesList[position]

        Log.d(
            "SelectedGesturesAdapter",
            "onBind pos=$position sprId=$sprId gestureId=$gestureId activeGestureId=$activeGestureId"
        )

        // название жеста протеза (если 0 — слот пустой)
        if (gestureId != 0) {
            holder.gestureName.text =
                CollectionGesturesProvider.getGesture(gestureId).gestureName
        } else {
            holder.gestureName.text = ""
        }

        // анимация SPR-жеста
        val sprGesture = SprGestureItemsProvider(myContext).getSprGesture(sprId)
        holder.gestureAnimation.setAnimation(sprGesture.animationId)
        holder.gestureSprTitleTop.text = sprGesture.title
        holder.gestureAnimation.playAnimation()

        val isActive = (activeGestureId == gestureId && gestureId != 0)

        // РАМКА АКТИВНОГО ЖЕСТА: красим ИМЕННО gestureSprCollection1Btn
        holder.gestureContainer.setBackgroundResource(
            if (isActive) R.drawable.ubi4_view_with_corners_gray_active
            else R.drawable.ubi4_view_with_corners_gray_outside
        )

        holder.dotsThreeBtnSpr.setOnClickListener {
            onDotsClickListener(position)
        }

        holder.itemView.setOnClickListener {
            // если на ячейке нет назначенного жеста — игнор
            if (gestureId == 0) {
                Log.d("SelectedGesturesAdapter", "click on empty binding slot, skip")
                return@setOnClickListener
            }

            val title = holder.gestureName.text.toString()
            activeGestureId = gestureId
            Log.d(
                "SelectedGesturesAdapter",
                "click pos=$position sprId=$sprId gestureId=$gestureId → set activeGestureId=$activeGestureId"
            )
            notifyDataSetChanged() // перерисовать рамки внутри адаптера

            onCheckGestureSprListener.onGestureSprClicked(position, title, gestureId)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateGestures(newGestures: List<Pair<Int, Int>>) {
        selectedGesturesList.clear()
        selectedGesturesList.addAll(newGestures)
        Log.d("SelectedGesturesAdapter", "updateGestures = $selectedGesturesList")
        notifyDataSetChanged()
    }

    // вызывать из делегата, когда активный жест прилетел по BLE
    @SuppressLint("NotifyDataSetChanged")
    fun setActiveGesture(gestureId: Int?) {
        Log.d("SelectedGesturesAdapter", "setActiveGesture from delegate: $gestureId")
        activeGestureId = gestureId
        notifyDataSetChanged()
    }

    interface OnCheckSprGestureListener {
        fun onGestureSprClicked(position: Int, title: String, gestureId: Int)
    }
}