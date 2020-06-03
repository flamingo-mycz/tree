package com.mycz.tree.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mycz.tree.R;

import org.angmarch.views.NiceSpinner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class LoginActivity extends AppCompatActivity{

    private EditText mEtUsername;
    private EditText mEtPassword;
    private NiceSpinner mSpRole;
    private Button mBtLogin;

    private OkHttpClient mClient;

    private String mRoleName = "管理员";

    private static final String SERVER_URL = "http://112.245.48.4:100/Taishan/LoginServlet";

    private ProgressBar mPbLogin;
    private ImageView mIvLoginBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();
        initClient();
        initEvent();
    }

    /**
     * 初始化OkHttp客户端
     */
    private void initClient() {
        mClient = new OkHttpClient();
    }


    /**
     * 初始化界面
     */
    private void initView() {
        mEtUsername = findViewById(R.id.et_username);
        mEtPassword = findViewById(R.id.et_password);
        mSpRole = findViewById(R.id.sp_role);
        mBtLogin = findViewById(R.id.bt_login);
        mPbLogin = findViewById(R.id.pb_login);

        mIvLoginBg = findViewById(R.id.iv_login_bg);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.login_background, options);
        mIvLoginBg.setImageBitmap(bitmap);
    }

    /**
     * 初始化事件
     */
    private void initEvent() {

        mBtLogin.setOnClickListener(v -> loginRemote());

        LinkedList<String> dataSource = new LinkedList<>(Arrays.asList("管理员", "普通用户", "公司用户"));
        mSpRole.attachDataSource(dataSource);
        mSpRole.addOnItemClickListener((parent, view, position, id) -> mRoleName = String.valueOf(dataSource.get(position)));
    }


    private void loginRemote() {

        //显示进度条
        mPbLogin.setVisibility(View.VISIBLE);

        String inputUsername = mEtUsername.getText().toString();
        String inputPassword = mEtPassword.getText().toString();
        String roleName = mRoleName;

        int role = 0;
        switch (roleName) {
            case "管理员": role = 0; break;
            case "普通用户": role = 1; break;
            case "公司用户": role = 2; break;
            default:break;
        }
        FormBody.Builder formBody = new FormBody.Builder();
        formBody.add("username", inputUsername);
        formBody.add("password", inputPassword);
        formBody.add("role", role + "");
        Request request = new Request.Builder().url(SERVER_URL).post(formBody.build()).build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //隐藏进度条
                        mPbLogin.setVisibility(View.INVISIBLE);
                        Toast.makeText(LoginActivity.this, "网络异常", Toast.LENGTH_SHORT).show();
                    }
                });
            };

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                System.out.println(result);
                if("success".equals(result)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //隐藏进度条
                            mPbLogin.setVisibility(View.INVISIBLE);
                            Toast.makeText(LoginActivity.this,"登录成功！", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //隐藏进度条
                            mPbLogin.setVisibility(View.INVISIBLE);
                            Toast.makeText(LoginActivity.this,"登录失败！", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

    }
}

