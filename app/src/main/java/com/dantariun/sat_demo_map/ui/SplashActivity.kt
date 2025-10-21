package com.dantariun.sat_demo_map.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.databinding.ActivitySplashBinding
import com.dantariun.sat_demo_map.ui.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    // 각 비동기 작업을 정의하는 Enum (동영상 재생 작업 추가)
    private enum class SplashTask {
        VIDEO_PLAYBACK,
        LOCATION_PERMISSION,
        MINIMUM_DELAY,
        GRAPH_HOPPER_INIT
    }

    private lateinit var binding: ActivitySplashBinding
    private val splashViewModel: SplashViewModel by viewModels()

    // 완료된 작업을 추적하는 Set
    private val completedTasks = mutableSetOf<SplashTask>()
    private var hasNavigated = false // 중복 실행 방지 플래그

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
            onPermissionGranted()
        } else {
            Toast.makeText(this, "앱을 사용하려면 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            lifecycleScope.launch {
                delay(2000L)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindowInsets()

        binding.progressBar.progress = 0

        // 1. 우선적으로 인트로 동영상 재생
//        playIntroVideo()
        binding.progressBar.visibility = View.VISIBLE
        updateProgressAndNavigate(SplashTask.VIDEO_PLAYBACK)
        startRemainingTasks()
    }

    private fun playIntroVideo() {
        // 중요: intro_movie.mp4 파일을 'assets' 폴더에서 'res/raw' 폴더로 이동시켜야 합니다.
        // VideoView는 res/raw 리소스를 재생하는 것이 훨씬 간단하고 효율적입니다.
        try {
            val videoView = binding.splashVideoView
            val uri = Uri.parse("android.resource://$packageName/${R.raw.intro_movie}")
            videoView.setVideoURI(uri)

            videoView.setOnPreparedListener { mp ->
                val videoRatio = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
                val screenRatio = videoView.width.toFloat() / videoView.height.toFloat()

                // centerCrop
//                val scaleX = videoRatio / screenRatio
//                if (scaleX >= 1f) {
//                    videoView.scaleX = scaleX
//                } else {
//                    videoView.scaleY = 1f / scaleX
//                }

                //fitCenter
                if (videoRatio > screenRatio) {
                    val scale = screenRatio / videoRatio
                    videoView.scaleY = scale
                } else {
                    val scale = videoRatio / screenRatio
                    videoView.scaleX = scale
                }

                mp.start()
            }

            videoView.setOnCompletionListener {
                // 영상 재생이 완료되면, 첫 번째 작업을 완료 처리하고 나머지 작업들을 시작합니다.
                binding.progressBar.visibility = View.VISIBLE
                updateProgressAndNavigate(SplashTask.VIDEO_PLAYBACK)
                startRemainingTasks()
            }

            videoView.setOnErrorListener { _, _, _ ->
                // 영상 재생에 실패할 경우, 바로 다음 작업으로 넘어갑니다.
                binding.progressBar.visibility = View.VISIBLE
//                Toast.makeText(this, "영상 재생에 실패했습니다.", Toast.LENGTH_SHORT).show()
                updateProgressAndNavigate(SplashTask.VIDEO_PLAYBACK) // 작업을 완료된 것으로 간주하여 진행
                startRemainingTasks()
                true // 에러를 처리했음을 알림
            }

//            videoView.start()
        } catch (e: Exception) {
            // 리소스를 찾지 못하는 등 예외 발생 시 처리 (예: R.raw.intro_movie 없음)
            Toast.makeText(this, "인트로 영상을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            updateProgressAndNavigate(SplashTask.VIDEO_PLAYBACK)
            startRemainingTasks()
        }
    }

    /**
     * 영상 재생 후 실행되어야 할 나머지 작업들을 시작합니다.
     */
    private fun startRemainingTasks() {
        // ViewModel의 LiveData 관찰 설정 (GraphHopper 초기화)
        observeViewModel()

        // 위치 권한 확인 시작
        checkLocationPermission()
    }


    private fun observeViewModel() {
        splashViewModel.graphHopperState.observe(this) { state ->
            when (state) {
                is SplashViewModel.GraphHopperState.Success -> {
                    updateProgressAndNavigate(SplashTask.GRAPH_HOPPER_INIT)
                }
                is SplashViewModel.GraphHopperState.Error -> {
                    Toast.makeText(this, "맵 초기화 실패: ${state.message}", Toast.LENGTH_LONG).show()
                    lifecycleScope.launch {
                        delay(2000L)
                        finish()
                    }
                }
                is SplashViewModel.GraphHopperState.Loading -> {
                    // 로딩 중
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * 권한이 승인되면 관련된 작업을 처리합니다.
     */
    private fun onPermissionGranted() {
        // 1. 위치 권한 작업 완료 처리
        updateProgressAndNavigate(SplashTask.LOCATION_PERMISSION)

        // 2. 최소 스플래시 시간 대기 시작
        lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            // 대기 시간 종료 후, 최소 시간 작업 완료 처리
            updateProgressAndNavigate(SplashTask.MINIMUM_DELAY)
        }
    }

    /**
     * 작업 완료를 알리고, ProgressBar를 업데이트하며, 모든 조건 충족 시 다음 화면으로 이동합니다.
     * 여러 스레드에서 동시에 접근할 수 있으므로 synchronized로 보호합니다.
     */
    @Synchronized
    private fun updateProgressAndNavigate(task: SplashTask) {
        // 이미 화면 전환이 시작되었다면 더 이상 진행하지 않음
        if (hasNavigated) return

        completedTasks.add(task)

        // 새로운 progress 값 계산 (완료된 작업 수 / 전체 작업 수)
        val totalTasks = SplashTask.values().size
        val newProgress = (completedTasks.size * 100) / totalTasks

        // 현재 progress에서 새로운 progress까지 부드럽게 애니메이션
        val progressBar = binding.progressBar
        val currentProgress = progressBar.progress
        ObjectAnimator.ofInt(progressBar, "progress", currentProgress, newProgress).apply {
            duration = 500L // 0.5초 동안 애니메이션
            interpolator = DecelerateInterpolator()
            start()
        }

        // 모든 작업이 완료되었는지 확인
        if (completedTasks.size == totalTasks) {
            hasNavigated = true // 중복 실행 방지
            // ProgressBar가 100%까지 채워지는 애니메이션을 보여주기 위해 짧은 딜레이 후 이동
            lifecycleScope.launch {
                delay(500L) // 위 애니메이션 시간과 맞춤
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupWindowInsets() {
        // 기존 코드와 동일
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 2000L
    }
}