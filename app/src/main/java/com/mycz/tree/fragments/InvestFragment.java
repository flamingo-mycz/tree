package com.mycz.tree.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.mycz.tree.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 调查数据的Fragment
 *
 * @author 木已成舟
 * @date 2019/10/12
 */
public class InvestFragment extends Fragment {

    //地径
    private EditText mEtGroundDiameter;
    //照片(全貌)
    private ImageView mIvPhotoFullView;
    //照片(迹地清理)
    private ImageView mIvPhotoCleanliness;
    //照片(封根)
    private ImageView mIvPhotoCoverRoot;
    //备选照片
    private ImageView mIvPhotoAlternative;
    //本次工作内容
    private Spinner mSpWorkContent;
    //现场操作人员
    private EditText mEtSpotOperator;
    //是否取样
    private CheckBox mCbWhetherToSample;
    //枯死树木处理方式
    private Spinner mSpProcessingMethod;
    //备注
    private EditText mEtRemarks;
    //林场
    private Spinner mSpForestFarm;
    //林班
    private EditText mEtForestClass;
    //小班
    private EditText mEtSmallClass;
    //保存
    private Button mBtSave;
    //上传
    private Button mBtUpload;

    //ImageView的数组
    private ImageView[] mImageViews;

    //当前被点击的ImageView
    private ImageView mClickedImageView;
    //当前被点击的ImageView所对应的的图像文件的路径
    private String mSavedImagePath;

    //打开相册的请求码
    public static final  int GALLERY_REQUEST_CODE = 0x01;
    //打开摄像头的请求码
    private static final int CAMERA_REQUEST_CODE = 0x02;

    //装载了图片文件的Map集合
    private Map<String, File> mPhotoMap;

    //服务器的URL
    private static final String SERVER_URL = "http://112.245.48.4:100/Taishan/FileUpLoadServlet";

    //OkHttp的客户端对象
    private OkHttpClient mClient;

    //保存拍摄的照片的目录文件夹
    private File imgDir;

    //自定义上传的进度条
    private LinearLayout mLLUploading;
    private EditText mEtTreeId;

    /**
     * 当Fragment被创建时回调此方法，
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invest, container, false);
    }

    /**
     * 当Fragment创建完成时回调此方法
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
        initData();
        initCollections();
        initClient();
        initEvent();
    }



    /**
     * 初始化界面
     */
    private void initView(View root) {
        mEtTreeId = root.findViewById(R.id.et_tree_id);
        mEtGroundDiameter = root.findViewById(R.id.et_ground_diameter);
        // 设置失去焦点
        mEtGroundDiameter.clearFocus();
        mIvPhotoFullView = root.findViewById(R.id.iv_photo_full_view);
        mIvPhotoCleanliness = root.findViewById(R.id.iv_photo_cleanliness);
        mIvPhotoCoverRoot = root.findViewById(R.id.iv_photo_cover_root);
        mIvPhotoAlternative = root.findViewById(R.id.iv_photo_alternative);
        mSpWorkContent = root.findViewById(R.id.sp_work_content);
        mEtSpotOperator = root.findViewById(R.id.et_spot_operator);
        mCbWhetherToSample = root.findViewById(R.id.cb_whether_to_sample);
        mSpProcessingMethod = root.findViewById(R.id.sp_processing_method);
        mEtRemarks = root.findViewById(R.id.et_remarks);
        mSpForestFarm = root.findViewById(R.id.sp_forest_farm);
        mEtForestClass = root.findViewById(R.id.et_forest_class);
        mEtSmallClass = root.findViewById(R.id.et_small_class);
        mBtSave = root.findViewById(R.id.bt_save);
        mBtUpload = root.findViewById(R.id.bt_upload);
        mLLUploading = root.findViewById(R.id.ll_uploading);
    }

    /**
     * 初始化数据，从SharedPreferences中获取数据
     */
    private void initData() {
        SharedPreferences defaultData = getActivity().getSharedPreferences("default_data", Context.MODE_PRIVATE);
        //加载 地径 信息
        String groundDiameter = defaultData.getString("ground_diameter", "");
        mEtGroundDiameter.setText(groundDiameter);
        //加载 工作内容 信息
        String workContent = defaultData.getString("work_content", "default");
        if(!"default".equals(workContent)) {
            SpinnerAdapter adapter = mSpWorkContent.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if(workContent.equals(adapter.getItem(i).toString())) {
                    mSpWorkContent.setSelection(i, true);
                }
            }
        }
        //加载 操作人员 信息
        String spotOperator = defaultData.getString("spot_operator", "");
        mEtSpotOperator.setText(spotOperator);
        //加载 是否取样 信息
        boolean whetherToSample = defaultData.getBoolean("whether_to_sample", false);
        mCbWhetherToSample.setChecked(whetherToSample);
        //加载 枯死树木处理方式 信息
        String processingMethod = defaultData.getString("processing_method", "default");
        if(!"default".equals(processingMethod)) {
            SpinnerAdapter adapter = mSpProcessingMethod.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if(processingMethod.equals(adapter.getItem(i).toString())) {
                    mSpProcessingMethod.setSelection(i, true);
                }
            }
        }
        //加载 林场 信息
        String forestFarm = defaultData.getString("forest_farm", "default");
        if(!"default".equals(forestFarm)) {
            SpinnerAdapter adapter = mSpForestFarm.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if(forestFarm.equals(adapter.getItem(i).toString())) {
                    mSpForestFarm.setSelection(i, true);
                }
            }
        }
        //加载 林班 信息
        String forestClass = defaultData.getString("str_forest_class", "");
        mEtForestClass.setText(forestClass);
        //加载 小班 信息
        String smallClass = defaultData.getString("str_small_class", "");
        mEtSmallClass.setText(smallClass);
    }

    /**
     * 初始化容器
     */
    private void initCollections() {
        mImageViews = new ImageView[]{mIvPhotoFullView, mIvPhotoCleanliness, mIvPhotoCoverRoot, mIvPhotoAlternative};
        mPhotoMap = new HashMap<>();
    }

    /**
     * 初始化okhttp的客户端
     */
    private void initClient() {
        mClient = new OkHttpClient();
    }

    /**
     * 初始化事件
     */
    private void initEvent() {
        //为每一个ImageView注册监听器
        for (ImageView  iv : mImageViews) {
            iv.setOnClickListener(v -> {
                mClickedImageView = (ImageView) v;
                showPopupWindow();
            });
        }
        //给保存按钮添加点击事件
        mBtSave.setOnClickListener(v -> {
            boolean success = saveToSharedPreferences();
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("小提示");
            builder.setMessage(success ? "保存成功！" : "保存失败");
            builder.setPositiveButton("确定", (dialog, which) -> {});
            builder.create().show();
        });

        //给上传按钮添加点击事件
        mBtUpload.setOnClickListener(v -> {
            //弹出确认上传的对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("上传提示");
            builder.setMessage("是否确认上传？");
            builder.setPositiveButton("确定", (dialog, which) -> {
                //判断树木编号是否为null
                String treeId = String.valueOf(mEtTreeId.getText()).trim();
                if(treeId.length() == 0) {
                    //弹出提示对话框
                    showDialog();
                } else {
                    saveToSharedPreferences();
                    postMessageToServer();
                }
            });
            builder.setNegativeButton("取消", (dialog, which) -> {
                Toast.makeText(getContext(),"取消上传", Toast.LENGTH_SHORT).show();
            });
            builder.create().show();
        });

    }

    /**
     * 弹出警告对话框
     */
    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("警告");
        builder.setMessage("树木编号必填！");
        builder.setPositiveButton("确定", (dialog, which) -> {});
        builder.create().show();
    }

    /**
     * 保存到本地的SharedPreferences
     */
    private boolean saveToSharedPreferences() {
        SharedPreferences defaultData = getContext().getSharedPreferences("default_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = defaultData.edit();
        editor.putString("ground_diameter", mEtGroundDiameter.getText().toString());
        editor.putString("work_content", mSpWorkContent.getSelectedItem().toString());
        editor.putString("spot_operator", mEtSpotOperator.getText().toString());
        editor.putBoolean("whether_to_sample", mCbWhetherToSample.isChecked());
        editor.putString("str_processing_method", mSpProcessingMethod.getSelectedItem().toString());
        editor.putString("str_forest_farm", mSpForestFarm.getSelectedItem().toString());
        editor.putString("str_forest_class", mEtForestClass.getText().toString());
        editor.putString("str_small_class", mEtSmallClass.getText().toString());
        boolean success = editor.commit();

        return success;
    }

    /**
     * 把采集的数据发送到服务器
     */
    private void postMessageToServer() {



        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        //将采集的数据封装到请求体中
        //设置文件以及文件上传类型封装
        Set<String> keySet = mPhotoMap.keySet();
        for (String key : keySet) {
            File img = mPhotoMap.get(key);
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), img);
            builder.addFormDataPart(key, img != null ? img.getName() : "unnamed", requestBody);
        }

        builder.addFormDataPart(getResources().getString(R.string.str_tree_id), mEtTreeId.getText().toString().trim());
        builder.addFormDataPart(getResources().getString(R.string.str_ground_diameter), mEtGroundDiameter.getText().toString().trim());
        builder.addFormDataPart(getResources().getString(R.string.str_work_content), mSpWorkContent.getSelectedItem().toString());
        builder.addFormDataPart(getResources().getString(R.string.str_spot_operator), mEtSpotOperator.getText().toString().trim());
        builder.addFormDataPart(getResources().getString(R.string.str_whether_to_sample), mCbWhetherToSample.isChecked() ? "是" : "否");
        builder.addFormDataPart(getResources().getString(R.string.str_processing_method), mSpProcessingMethod.getSelectedItem().toString().trim());
        builder.addFormDataPart(getResources().getString(R.string.str_remarks), mEtRemarks.getText().toString().trim());
        builder.addFormDataPart(getResources().getString(R.string.str_forest_farm), mSpForestFarm.getSelectedItem().toString());
        builder.addFormDataPart(getResources().getString(R.string.str_forest_class), mEtForestClass.getText().toString().trim());
        builder.addFormDataPart(getResources().getString(R.string.str_small_class), mEtSmallClass.getText().toString().trim());
        builder.addFormDataPart("longitude", MapToolsFragment.LONGITUDE + "");
        builder.addFormDataPart("latitude", MapToolsFragment.LATITUDE + "");


        MultipartBody multipartBody = builder.build();
        System.out.println(multipartBody.toString());
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(multipartBody)
                .build();

        //显示进度条
        mLLUploading.setVisibility(View.VISIBLE);

        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("onFailure");
                e.printStackTrace();
                InvestFragment.this.getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "上传失败，请检查网络是否可用", Toast.LENGTH_SHORT).show();
//                    mEtRemarks.setText(e.getMessage());
                    mLLUploading.setVisibility(View.INVISIBLE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("onResponse");
                System.out.println(response.protocol() + " " +response.code() + " " + response.message());
                InvestFragment.this.getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "上传成功", Toast.LENGTH_SHORT).show();
                    mLLUploading.setVisibility(View.INVISIBLE);
                });
            }
        });
    }

    /**
     * 弹出Popup窗口，让用户选择从图库获取图片，还是拍照获取图片
     */
    private void showPopupWindow() {
        View contentView = LayoutInflater.from(getContext()).inflate(R.layout.layout_popup_window_photo, null);
        final PopupWindow popupWindow = new PopupWindow(contentView, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setContentView(contentView);
        LinearLayout llSelectPhoto = contentView.findViewById(R.id.ll_select_photo);
        //当用户点击从图库选择照片时
        llSelectPhoto.setOnClickListener(view -> {
            //跳转到选择图片界面
            Intent intent = new Intent( Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
            popupWindow.dismiss();
        });
        LinearLayout llTakePhoto = contentView.findViewById(R.id.ll_take_photo);
        //当用户点击拍照时
        llTakePhoto.setOnClickListener(v -> {
            //请求权限
            requestCameraPermission();
            popupWindow.dismiss();
        });

        View rootView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_invest, null);
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);
    }

    /**
     * 当用户点击打开摄像头拍照时，请求获得使用摄像头权限和读写SD卡权限
     */
    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, CAMERA_REQUEST_CODE);
        }
    }

    /**
     * 当用户选择是否授予权限后回调此方法
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //如果用户点击拍照， 跳转至拍照的Activity
        if(requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

                imgDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "photo");
                if(!imgDir.exists()) {
                    imgDir.mkdir();
                }

                String filename = createImageFile();
                mSavedImagePath = imgDir + File.separator + filename + ".jpg";

                File cameraSavePath = new File(mSavedImagePath);

                //如果版本大于安卓7.0
                Uri imageUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    imageUri = FileProvider.getUriForFile(getContext(), "com.mycz.tree.fileprovider", cameraSavePath);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    imageUri = Uri.fromFile(cameraSavePath);
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            } else {
                Toast.makeText(getContext(), R.string.camera_tip, Toast.LENGTH_SHORT).show();
            }
        }

    }


    /**
     * 根据被点击的ImageView创建对应的图像文件
     * @return 图像文件的名称
     */
    private String createImageFile() {
        String filename = null;
        switch (mClickedImageView.getId()) {
            case R.id.iv_photo_alternative:
                filename = "alternative";
                break;
            case R.id.iv_photo_cleanliness:
                filename = "cleanliness";
                break;
            case R.id.iv_photo_cover_root:
                filename = "cover_root";
                break;
            case R.id.iv_photo_full_view:
                filename = "full_view";
                break;
            default:
                break;
        }
        return filename;
    }

    /**
     * 当从别的Activity返回此Activity时回调此方法
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //如果从图库界面返回
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == this.getActivity().RESULT_OK) {
            //如果用户选择了相片
            if(data != null) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                //查询我们需要的数据
                Cursor cursor = this.getActivity().getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                //放入文件到map中
                putImageFileIntoMap(new File(picturePath));
                //imageview显示压缩过的图片
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(new File(picturePath)), null, options);
                    mClickedImageView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                //                mClickedImageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            }
        }

        //如果从摄像头界面返回
        if(requestCode == CAMERA_REQUEST_CODE && resultCode == this.getActivity().RESULT_OK) {
            try {
                //把文件放入到map中
                putImageFileIntoMap(new File(mSavedImagePath));
                //显示压缩过的bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(mSavedImagePath), null, options);
                mClickedImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将选择的图片文件放入Map中
     * @param file
     */
    private void putImageFileIntoMap(File file) {
        switch (mClickedImageView.getId()) {
            case R.id.iv_photo_alternative:
                mPhotoMap.put("备选照片", file);
                break;
            case R.id.iv_photo_cleanliness:
                mPhotoMap.put("照片(迹地清理)", file);
                break;
            case R.id.iv_photo_cover_root:
                mPhotoMap.put("照片(封根)", file);
                break;
            case R.id.iv_photo_full_view:
                mPhotoMap.put("照片(全貌)", file);
                break;
            default:
                break;
        }
    }


}
