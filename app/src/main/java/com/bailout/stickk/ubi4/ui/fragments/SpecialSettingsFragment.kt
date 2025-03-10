import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSpecialSettingsBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.AUTO_LOGIN_KEY
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class SpecialSettingsFragment : BaseWidgetsFragment() {

    private lateinit var binding: Ubi4FragmentSpecialSettingsBinding
    private var main: MainActivityUBI4? = null
    private val mDataFactory: DataFactory = DataFactory()
    private val display = 2

    // Флаг выбранного режима: false – настройки протеза, true – мобильные настройки
    private var isMobileSettingsMode = false

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = Ubi4FragmentSpecialSettingsBinding.inflate(inflater, container, false)
        main = activity as? MainActivityUBI4

        // Обработчики переключения режимов
        binding.leftButton.setOnClickListener {
            if (isMobileSettingsMode) {
                isMobileSettingsMode = false
                updateUI()
            }
        }
        binding.rightButton.setOnClickListener {
            if (!isMobileSettingsMode) {
                isMobileSettingsMode = true
                updateUI()
            }
        }

        // Инициализируем состояние по умолчанию
        updateUI()

        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.settingsRecyclerView.adapter = adapterWidgets

        // Меняем текст в инклюженном виджете на "Авто логин"
        binding.mobileSettingsContainer.findViewById<TextView>(R.id.widgetDescriptionTv)?.text =
            getString(
                R.string.auto_login
            )

//        val autoLoginSwitch =
//            binding.mobileSettingsContainer.findViewById<Switch>(R.id.widgetSwitchSc)
//        autoLoginSwitch?.apply {
//            // При старте устанавливаем состояние из SharedPreferences
//            isChecked = getAutoLoginState()
//            setOnCheckedChangeListener { _, isChecked ->
//                // Сохраняем новое состояние
//                saveAutoLoginState(isChecked)
//                // Здесь можно добавить логику авто входа по MAC адресу
//                if (isChecked) {
//                    // Включаем авто вход: например, вызов функции connectAutomatically(macAddress)
//                    // Допустим, macAddress хранится в MainActivityUBI4 или в SharedPreferences
//                    main?.enableAutoLogin() // или своя реализация
//                    // Либо, например, Log.d("AutoLogin", "Auto login enabled")
//                } else {
//                    // Выключаем авто вход: main?.disableAutoLogin()
//                    main?.disableAutoLogin()
//                    // Log.d("AutoLogin", "Auto login disabled")
//                }
//            }
//        }

        widgetListUpdater()

        return binding.root
    }

//    private fun getAutoLoginState(): Boolean {
//        val prefs = context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//        return prefs?.getBoolean(AUTO_LOGIN_KEY, false) ?: false
//    }
//
//    // Сохраняем состояние авто входа
//    private fun saveAutoLoginState(state: Boolean) {
//        val prefs = context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//        prefs?.edit()?.putBoolean(AUTO_LOGIN_KEY, state)?.apply()
//    }


    // Обновление интерфейса в зависимости от выбранного режима
    private fun updateUI() {
        updateSelectorUI()
        if (isMobileSettingsMode) {
            // Режим мобильных настроек: показываем контейнер мобильных настроек и скрываем RecyclerView
            binding.mobileSettingsContainer.visibility = View.VISIBLE
            binding.settingsRecyclerView.visibility = View.GONE
        } else {
            // Режим настроек протеза: скрываем контейнер мобильных настроек и отображаем RecyclerView
            binding.mobileSettingsContainer.visibility = View.GONE
            binding.settingsRecyclerView.visibility = View.VISIBLE
            adapterWidgets.swapData(mDataFactory.prepareData(display))
        }
    }

    // Анимация индикатора и смена цвета текста кнопок, аналогично примеру в GesturesDelegateAdapter,
    // но адаптировано для двух кнопок
    private fun updateSelectorUI() {
        var duration = 200L
        val selectedColor = requireContext().getColor(R.color.white)
        val unselectedColor = requireContext().getColor(android.R.color.darker_gray)
        val displayMetrics = resources.displayMetrics

        // Получаем ширину контейнера селектора
        val containerWidth = binding.settingsSelectorContainer.width
        // Для двух кнопок предполагаем, что каждая занимает половину контейнера
        val halfWidth = containerWidth / 2f

        // Анимируем перемещение индикатора
        val targetX = if (isMobileSettingsMode) halfWidth else 0f
        ObjectAnimator.ofFloat(binding.selectorIndicator, "translationX", targetX)
            .setDuration(duration)
            .start()

        // Анимация смены цвета текста с использованием ArgbEvaluator
        val leftColorAnim = ObjectAnimator.ofInt(
            binding.leftButton,
            "textColor",
            if (!isMobileSettingsMode) unselectedColor else selectedColor,
            if (!isMobileSettingsMode) selectedColor else unselectedColor
        ).apply {
            duration = duration
            setEvaluator(ArgbEvaluator())
        }
        leftColorAnim.start()

        val rightColorAnim = ObjectAnimator.ofInt(
            binding.rightButton,
            "textColor",
            if (isMobileSettingsMode) unselectedColor else selectedColor,
            if (isMobileSettingsMode) selectedColor else unselectedColor
        ).apply {
            duration = duration
            setEvaluator(ArgbEvaluator())
        }
        rightColorAnim.start()
    }

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            updateFlow.collect {
                updateUI()
            }
        }
    }
}