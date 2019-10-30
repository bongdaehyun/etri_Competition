package com.google.sample.cloudvision;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

public class CustomDialog extends Dialog {

    private Button mPositiveButton;

    private TextView name;
    private TextView info;
    private View.OnClickListener mPositiveListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String nameTempDi;

        //다이얼로그 밖의 화면은 흐리게 만들어줌
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);

        setContentView(R.layout.custom_dialog);
        name = (TextView)findViewById(R.id.nameText);
        info = (TextView)findViewById(R.id.infoText);

        nameTempDi = MainActivity.item.get(MainActivity.pos);
        nameTempDi = nameTempDi.replaceAll("\"","");
        name.setText(nameTempDi);
        info.setText(MainActivity.result.get(MainActivity.pos));

        //셋팅
        mPositiveButton=(Button)findViewById(R.id.pbutton);

        //클릭 리스너 셋팅 (클릭버튼이 동작하도록 만들어줌.)
        mPositiveButton.setOnClickListener(mPositiveListener);
    }

    //생성자 생성
    public CustomDialog(@NonNull Context context, View.OnClickListener positiveListener) {
        super(context);
        this.mPositiveListener = positiveListener;
    }
}

