package com.mycompany.news;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

public class LoginActivity extends AppCompatActivity {

    private EditText accountEdit;
    private EditText passportEdit;
    private CheckBox rememberPass;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        accountEdit = findViewById(R.id.account);
        passportEdit = findViewById(R.id.password);
        rememberPass = findViewById(R.id.remember_pass);
        Button login = findViewById(R.id.login);

        boolean isRemember = pref.getBoolean("remember_password",false);
        if (isRemember){
            //将账号和密码都设置到文本框中
            String account = pref.getString("account","");
            String password = pref.getString("password","");
            accountEdit.setText(account);
            passportEdit.setText(password);
            rememberPass.setChecked(true);
        }

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String account = accountEdit.getText().toString();
                String password = passportEdit.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //发送事件
                        EventBus.getDefault().post(new LoginEvent(account));
                    }
                }).start();
                if (account.equals("admin") && password.equals("123456")){
                    editor = pref.edit();
                    if (rememberPass.isChecked()){
                        editor.putBoolean("remember_password",true);
                        editor.putString("account",account);
                        editor.putString("password",password);
                    }else{
                        editor.clear();
                    }
                    editor.apply();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("username",account);
                    startActivity(intent);
                    finish();
                }else {
                    Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_SHORT).show();
                }
            }
        });


    }
}
