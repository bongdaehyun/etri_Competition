package com.google.sample.cloudvision;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class SplashActivity extends AppCompatActivity {

    Animation flowAnim;
    TextView textView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        flowAnim = AnimationUtils.loadAnimation(this, R.anim.flow);


        //UI
        textView1 = (TextView)findViewById(R.id.textView1);


        flowAnim.setAnimationListener(new FlowAnimationListener());


        //뷰에 애니메이션 설정정
        textView1.startAnimation(flowAnim);

        startLoading();
    }
    private void startLoading() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               finish();
            }
        }, 4000);
    }
    private final class FlowAnimationListener implements Animation.AnimationListener {
        //애니메이션 종료되었을때
        @Override
        public void onAnimationEnd(Animation animation) {


            //Toast.makeText(getApplicationContext(), "애니메이션 종료됨", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            Log.d("ddd", "Animation Repeat");
        }
        //애니메이션 시작했을때
        @Override
        public void onAnimationStart(Animation animation) {
            Log.d("ddd", "Animation Start");
        }
    }


}
