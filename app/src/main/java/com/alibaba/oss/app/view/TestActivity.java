package com.alibaba.oss.app.view;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.alibaba.oss.R;
import com.alibaba.oss.app.Config;
import com.alibaba.oss.app.myPackage.Constant;
import com.alibaba.oss.app.service.ImageService;
import com.alibaba.oss.app.service.OssService;
import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;

import java.io.File;
import java.io.IOException;


public class TestActivity extends AppCompatActivity {

    private String objectName="001.jpg";

//
//    private String mImgEndpoint = "http://img-cn-hangzhou.aliyuncs.com";
//    private final String mBucket = Config.BUCKET_NAME;
//    private String mRegion = "";//杭州
    //负责所有的界面更新
    private UIDisplayer mUIDisplayer;

    //OSS的上传下载
    private OssService mService;
//    private ImageService mIMGService;
    private String mPicturePath = "";

    private static final int RESULT_LOAD_IMAGE = 1;
    private static final String FILE_DIR = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "oss/";
//    private static final String FILE_PATH = FILE_DIR + "wangwang.zip";
//    private MaterialDialog mLoadingDialog;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        ProgressBar bar = (ProgressBar) findViewById(R.id.bar);
        TextView textView = (TextView) findViewById(R.id.output_info);
        mUIDisplayer = new UIDisplayer(imageView, bar, textView, this);
        mService = initOSS(Constant.OSS_ENDPOINT, Constant.BUCKET_NAME, mUIDisplayer);

        //从系统相册选择图片
        final Button select = (Button) findViewById(R.id.select);
        select.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });


        Button upload = (Button) findViewById(R.id.upload);
        upload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.edit_text);
                String objectName = editText.getText().toString();

                mService.asyncPutImage(objectName, mPicturePath);

            }
        });


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Config.REQUESTCODE_OPEN_DOCUMENT && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                mService.asyncMultipartUpload(objectName, uri);
            }
        }
        if (requestCode == Config.REQUESTCODE_AUTH && resultCode == RESULT_OK) {
            if (data != null) {
                String url = data.getStringExtra("url");
                String endpoint = data.getStringExtra("endpoint");
                String bucketName = data.getStringExtra("bucketName");
                OSSAuthCredentialsProvider provider = new OSSAuthCredentialsProvider(url);
                ClientConfiguration conf = new ClientConfiguration();
                conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
                conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
                conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
                conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次

            }
        }

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            mPicturePath = cursor.getString(columnIndex);
            Log.d("PickPicture", mPicturePath);
            cursor.close();

            try {
                Bitmap bm = mUIDisplayer.autoResizeFromLocalFile(mPicturePath);
                mUIDisplayer.displayImage(bm);
                /*
                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                imageView.setImageBitmap(bm);*/
                File file = new File(mPicturePath);

                mUIDisplayer.displayInfo("文件: " + mPicturePath + "\n大小: " + String.valueOf(file.length()));
            } catch (IOException e) {
                e.printStackTrace();
                mUIDisplayer.displayInfo(e.toString());
            }
        }

    }
    public OssService initOSS(String endpoint, String bucket, UIDisplayer displayer) {

//        移动端是不安全环境，不建议直接使用阿里云主账号ak，sk的方式。建议使用STS方式。具体参
//        https://help.aliyun.com/document_detail/31920.html
//        注意：SDK 提供的 PlainTextAKSKCredentialProvider 只建议在测试环境或者用户可以保证阿里云主账号AK，SK安全的前提下使用。具体使用如下
//        主账户使用方式
//        String AK = "******";
//        String SK = "******";
//        credentialProvider = new PlainTextAKSKCredentialProvider(AK,SK)
//        以下是使用STS Sever方式。
//        如果用STS鉴权模式，推荐使用OSSAuthCredentialProvider方式直接访问鉴权应用服务器，token过期后可以自动更新。
//        详见：https://help.aliyun.com/document_detail/31920.html
//        OSSClient的生命周期和应用程序的生命周期保持一致即可。在应用程序启动时创建一个ossClient，在应用程序结束时销毁即可。


        OSSCredentialProvider credentialProvider=new OSSAuthCredentialsProvider(Constant.STS_server);

        //使用自己的获取STSToken的类
//        String stsServer = ((EditText) findViewById(R.id.stsserver)).getText().toString();
//        if (TextUtils.isEmpty(stsServer)) {
//            credentialProvider = new OSSAuthCredentialsProvider(Config.STS_SERVER_URL);
//            ((EditText) findViewById(R.id.stsserver)).setText(Config.STS_SERVER_URL);
//        } else {
//            credentialProvider = new OSSAuthCredentialsProvider(stsServer);
//        }


        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        OSS oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider, conf);
        OSSLog.enableLog();
        return new OssService(oss, bucket, displayer);

    }


}
