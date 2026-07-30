package com.example.overlay

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.example.automation.ScriptRunner
import com.example.databinding.OverlayTargetEditDialogBinding
import com.example.model.ClickTarget
import com.example.model.ExecutionState
import com.example.model.GlobalSettings
import com.example.model.ScriptModel
import com.example.model.TargetType
import com.example.utils.DisplayUtils
import com.example.utils.FeedbackUtils
import com.example.permission.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayManager(
    private val context: Context,
    private val feedbackUtils: FeedbackUtils
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scriptRunner = ScriptRunner(feedbackUtils)
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    private val targetViews = mutableListOf<TargetOverlayView>()
    private val _currentScript = MutableStateFlow(
        ScriptModel(
            name = "Quick Clicker Session",
            targets = mutableListOf()
        )
    )
    val currentScript: StateFlow<ScriptModel> = _currentScript.asStateFlow()

    private var globalSettings = GlobalSettings()

    val executionState: StateFlow<ExecutionState> = scriptRunner.executionState

    private var floatingBar: OverlayFloatingBar? = null
    private var isOverlayVisible = false
    private var isOverlayLocked = false
    private var currentEditDialog: Dialog? = null

    private fun dismissCurrentDialog() {
        try {
            currentEditDialog?.dismiss()
        } catch (e: Exception) {
            // Ignore
        }
        currentEditDialog = null
    }

    init {
        scope.launch {
            scriptRunner.executionState.collect { state ->
                when (state) {
                    is ExecutionState.Running -> {
                        targetViews.forEach { view ->
                            view.setTouchThrough(true)
                            view.setActiveStep(state.currentStep)
                        }
                        floatingBar?.setPlayState(isPlaying = true, isPaused = false)
                    }
                    is ExecutionState.Paused -> {
                        targetViews.forEach { view ->
                            view.setTouchThrough(false)
                            view.setActiveStep(-1)
                        }
                        floatingBar?.setPlayState(isPlaying = false, isPaused = true)
                    }
                    else -> {
                        targetViews.forEach { view ->
                            view.setTouchThrough(false)
                            view.setActiveStep(-1)
                        }
                        floatingBar?.setPlayState(isPlaying = false, isPaused = false)
                    }
                }
            }
        }
    }

    fun initOverlay(settings: GlobalSettings) {
        this.globalSettings = settings
        if (floatingBar == null) {
            floatingBar = OverlayFloatingBar(context, windowManager, object : OverlayFloatingBar.Listener {
                override fun onPlayPauseClicked() {
                    toggleExecution()
                }

                override fun onStopClicked() {
                    stopExecution()
                }

                override fun onAddTargetClicked() {
                    addTarget(TargetType.SINGLE_TAP)
                }

                override fun onAddSwipeClicked() {
                    addTarget(TargetType.SWIPE)
                }

                override fun onLockToggleClicked() {
                    toggleLockState()
                }

                override fun onRemoveTargetClicked() {
                    removeLastTarget()
                }

                override fun onCloseClicked() {
                    hideOverlay()
                }
            })
        }
    }

    fun showOverlay() {
        if (!PermissionUtils.hasOverlayPermission(context)) return
        if (!isOverlayVisible) {
            floatingBar?.show()
            targetViews.forEach {
                try { windowManager.addView(it, it.windowParams) } catch (e: Exception) {}
            }
            isOverlayVisible = true
        }
    }

    fun hideOverlay() {
        if (isOverlayVisible) {
            scriptRunner.stopScript()
            floatingBar?.hide()
            targetViews.forEach {
                try { windowManager.removeView(it) } catch (e: Exception) {}
            }
            isOverlayVisible = false
        }
    }

    fun loadScript(script: ScriptModel) {
        clearTargets()
        _currentScript.value = script
        script.targets.forEach { target ->
            createAndAddTargetOverlay(target)
        }
    }

    private fun addTarget(type: TargetType) {
        val currentTargets = _currentScript.value.targets.toMutableList()
        val (screenW, screenH) = DisplayUtils.getScreenSize(context)
        val nextOrder = currentTargets.size + 1

        val newTarget = ClickTarget(
            order = nextOrder,
            type = type,
            xPx = (screenW / 2f) + (nextOrder * 20),
            yPx = (screenH / 2f) + (nextOrder * 20),
            swipeEndXPx = (screenW / 2f) + 100f,
            swipeEndYPx = (screenH / 2f) - 200f,
            delayMs = globalSettings.defaultDelayMs,
            durationMs = if (type == TargetType.SWIPE) 400L else globalSettings.defaultDurationMs,
            label = if (type == TargetType.SWIPE) "Swipe #$nextOrder" else "Target #$nextOrder"
        )

        currentTargets.add(newTarget)
        _currentScript.value = _currentScript.value.copy(targets = currentTargets)
        createAndAddTargetOverlay(newTarget)
    }

    private fun createAndAddTargetOverlay(target: ClickTarget) {
        val targetView = TargetOverlayView(
            context = context,
            windowManager = windowManager,
            clickTarget = target,
            isOverlayLocked = isOverlayLocked,
            onPositionChanged = { updatedTarget ->
                updateTargetInScript(updatedTarget)
            },
            onClickTarget = { clickedTarget ->
                showEditTargetDialog(clickedTarget)
            }
        )

        targetViews.add(targetView)
        if (isOverlayVisible) {
            try {
                windowManager.addView(targetView, targetView.windowParams)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun toggleLockState() {
        isOverlayLocked = !isOverlayLocked
        floatingBar?.setLockState(isOverlayLocked)
        targetViews.forEach { view ->
            view.isOverlayLocked = isOverlayLocked
            view.invalidate()
        }
    }

    fun duplicateTarget(target: ClickTarget) {
        val currentTargets = _currentScript.value.targets.toMutableList()
        val nextOrder = currentTargets.size + 1
        val duplicated = target.copy(
            id = 0,
            order = nextOrder,
            xPx = target.xPx + 40f,
            yPx = target.yPx + 40f,
            label = "Target #$nextOrder"
        )
        currentTargets.add(duplicated)
        _currentScript.value = _currentScript.value.copy(targets = currentTargets)
        createAndAddTargetOverlay(duplicated)
    }

    fun deleteTargetByOrder(targetOrder: Int) {
        val viewIndex = targetViews.indexOfFirst { it.clickTarget.order == targetOrder }
        if (viewIndex != -1) {
            val viewToRemove = targetViews.removeAt(viewIndex)
            try {
                windowManager.removeView(viewToRemove)
            } catch (e: Exception) {
                // Ignore
            }
        }

        val updatedTargets = _currentScript.value.targets
            .filter { it.order != targetOrder }
            .mapIndexed { index, item -> item.copy(order = index + 1) }

        _currentScript.value = _currentScript.value.copy(targets = updatedTargets)

        // Refresh existing target views with reordered target indices
        targetViews.forEachIndexed { index, targetView ->
            val newTarget = updatedTargets.getOrNull(index)
            if (newTarget != null) {
                targetView.updateTargetData(newTarget)
            }
        }
    }

    private fun updateTargetInScript(updatedTarget: ClickTarget) {
        val updatedList = _currentScript.value.targets.map {
            if (it.order == updatedTarget.order) updatedTarget else it
        }
        _currentScript.value = _currentScript.value.copy(targets = updatedList)
    }

    private fun removeLastTarget() {
        if (targetViews.isNotEmpty()) {
            val lastView = targetViews.removeAt(targetViews.size - 1)
            try {
                windowManager.removeView(lastView)
            } catch (e: Exception) {
                // Ignore
            }
            val currentTargets = _currentScript.value.targets.toMutableList()
            if (currentTargets.isNotEmpty()) {
                currentTargets.removeAt(currentTargets.size - 1)
                _currentScript.value = _currentScript.value.copy(targets = currentTargets)
            }
        }
    }

    private fun clearTargets() {
        targetViews.forEach {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        targetViews.clear()
        _currentScript.value = _currentScript.value.copy(targets = emptyList())
    }

    private fun toggleExecution() {
        dismissCurrentDialog()
        val currentState = scriptRunner.executionState.value
        if (currentState is ExecutionState.Running) {
            scriptRunner.pauseScript()
            targetViews.forEach { it.setTouchThrough(false) }
            floatingBar?.setPlayState(isPlaying = false, isPaused = true)
        } else if (currentState is ExecutionState.Paused) {
            targetViews.forEach { it.setTouchThrough(true) }
            scriptRunner.resumeScript()
            floatingBar?.setPlayState(isPlaying = true, isPaused = false)
        } else {
            if (_currentScript.value.targets.isNotEmpty()) {
                targetViews.forEach { it.setTouchThrough(true) }
                scriptRunner.startScript(_currentScript.value, globalSettings)
                floatingBar?.setPlayState(isPlaying = true, isPaused = false)
            }
        }
    }

    private fun stopExecution() {
        dismissCurrentDialog()
        scriptRunner.stopScript()
        targetViews.forEach { it.setTouchThrough(false) }
        floatingBar?.setPlayState(isPlaying = false, isPaused = false)
    }

    private fun showEditTargetDialog(target: ClickTarget) {
        if (scriptRunner.executionState.value !is ExecutionState.Idle) {
            return
        }
        dismissCurrentDialog()

        val dialogBinding = OverlayTargetEditDialogBinding.inflate(LayoutInflater.from(context))
        dialogBinding.tvTitle.text = "Action Button #${target.order}"
        dialogBinding.etDelayMs.setText(target.delayMs.toString())
        dialogBinding.etDurationMs.setText(target.durationMs.toString())
        dialogBinding.etSizePx.setText(target.sizePx.toInt().toString())
        dialogBinding.etTextInput.setText(target.textContent)

        var currentDialogSize = target.sizePx

        // Populate Action Type Spinner
        val actionTypes = TargetType.values()
        val adapter = android.widget.ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            actionTypes.map { it.displayName }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        dialogBinding.spinnerActionType.adapter = adapter
        dialogBinding.spinnerActionType.setSelection(target.type.ordinal)

        fun updateTextInputVisibility(type: TargetType) {
            dialogBinding.layoutTextInput.visibility = if (type == TargetType.TEXT_INPUT) android.view.View.VISIBLE else android.view.View.GONE
        }

        updateTextInputVisibility(target.type)

        dialogBinding.spinnerActionType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedType = actionTypes[position]
                updateTextInputVisibility(selectedType)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Auto-save text content as typed
        dialogBinding.etTextInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentText = s?.toString() ?: ""
                val selType = actionTypes.getOrNull(dialogBinding.spinnerActionType.selectedItemPosition) ?: target.type
                val quickUpdated = target.copy(type = selType, textContent = currentText)
                updateTargetInScript(quickUpdated)
                targetViews.find { it.clickTarget.order == target.order }?.updateTargetData(quickUpdated)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        dialogBinding.btnDecreaseSize.setOnClickListener {
            currentDialogSize = (currentDialogSize - 10f).coerceAtLeast(48f)
            dialogBinding.etSizePx.setText(currentDialogSize.toInt().toString())
        }

        dialogBinding.btnIncreaseSize.setOnClickListener {
            currentDialogSize = (currentDialogSize + 10f).coerceAtMost(240f)
            dialogBinding.etSizePx.setText(currentDialogSize.toInt().toString())
        }

        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                @Suppress("DEPRECATION")
                window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
            }
        }

        currentEditDialog = dialog
        dialog.setOnDismissListener {
            if (currentEditDialog == dialog) {
                currentEditDialog = null
            }
        }

        dialogBinding.btnDuplicate.setOnClickListener {
            duplicateTarget(target)
            dialog.dismiss()
        }

        dialogBinding.btnDelete.setOnClickListener {
            deleteTargetByOrder(target.order)
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnSave.setOnClickListener {
            val selectedType = actionTypes.getOrNull(dialogBinding.spinnerActionType.selectedItemPosition) ?: target.type
            val delayMs = dialogBinding.etDelayMs.text.toString().toLongOrNull() ?: target.delayMs
            val durationMs = dialogBinding.etDurationMs.text.toString().toLongOrNull() ?: target.durationMs
            val sizePx = dialogBinding.etSizePx.text.toString().toFloatOrNull() ?: currentDialogSize
            val textContent = dialogBinding.etTextInput.text.toString()

            val updatedTarget = target.copy(
                type = selectedType,
                delayMs = delayMs,
                durationMs = durationMs,
                sizePx = sizePx.coerceIn(48f, 240f),
                textContent = textContent,
                label = "${selectedType.displayName} #${target.order}"
            )

            updateTargetInScript(updatedTarget)

            // Update matching view in overlay
            targetViews.find { it.clickTarget.order == target.order }?.updateTargetData(updatedTarget)

            dialog.dismiss()
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            // Ignore if overlay window token invalid
        }
    }
}
