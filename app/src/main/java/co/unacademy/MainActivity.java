package co.unacademy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity {
    private static final long CLIENT_READ_TIME_OUT = 5L;
    private static final long CLIENT_WRITE_TIME_OUT = 5L;
    private static final long CLIENT_CACHE_SIZE = 5 * 1024 * 1024L; // 5 MiB
    private static final String CLIENT_CACHE_DIRECTORY = "http";
    public static final String IMAGE_1 = "https://miro.medium.com/max/1200/1*mk1-6aYaf_Bes1E3Imhc0A.jpeg";
    public static final String IMAGE_2 = "https://wowslider.com/sliders/demo-18/data1/images/hongkong1081704.jpg";
    private OkHttpClient okHttpClient = null;
    private AppCompatImageView imageView1, imageView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView1 = findViewById(R.id.imageView1);
        imageView2 = findViewById(R.id.imageView2);
        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        initOKHttp();
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchImage1AndSet();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchImage2AndSet();
            }
        });

    }

    private void initOKHttp() {
        if (okHttpClient == null) {
            Cache cache = new Cache(new File(this.getBaseContext().getCacheDir(), CLIENT_CACHE_DIRECTORY), CLIENT_CACHE_SIZE);
            OkHttpClient.Builder  okHttpClientBuilder = new OkHttpClient.Builder()
                    .cache(cache)
                    .connectTimeout(CLIENT_READ_TIME_OUT, TimeUnit.MINUTES)
                    .writeTimeout(CLIENT_WRITE_TIME_OUT, TimeUnit.MINUTES)
                    .readTimeout(CLIENT_READ_TIME_OUT, TimeUnit.MINUTES);
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                okHttpClientBuilder.addInterceptor(logging);
            }
            okHttpClient = okHttpClientBuilder.build();
        }
    }

    private void fetchImage1AndSet() {
        ServiceWorker<Bitmap> serviceWorker1 = new ServiceWorker<>("service_worker_1");
        serviceWorker1.addTask(new Task<Bitmap>() {
            @Override
            public Bitmap onExecuteTask() {
                Log.d("task", "onExecuteTask");
                //Fetching image1 through okhttp
                Request request = new Request.Builder().url(IMAGE_1).build();
                Response response = null;
                try {
                    response = okHttpClient.newCall(request).execute();
                    return BitmapFactory.decodeStream(response.body() != null ? response.body().byteStream() : null);
                } catch (IOException e) {
                    Log.d("task", "onExecuteTask:: IOException");
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void onTaskComplete(@Nullable Bitmap result) {
                Log.d("task", "onTaskComplete:: " + result);
                //Set bitmap to imageview
                if (result != null)
                    imageView1.setImageBitmap(result);
            }
        });
    }

    private void fetchImage2AndSet() {
        ServiceWorker<Bitmap> serviceWorker2 = new ServiceWorker<>("service_worker_2");
        serviceWorker2.addTask(new Task<Bitmap>() {
            @Override
            public Bitmap onExecuteTask() {
                //Fetching image1 through okhttp
                Request request = new Request.Builder().url(IMAGE_2).build();
                Response response = null;
                try {
                    response = okHttpClient.newCall(request).execute();
                    return BitmapFactory.decodeStream(response.body() != null ? response.body().byteStream() : null);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }

            }

            @Override
            public void onTaskComplete(@Nullable Bitmap result) {
                //Set bitmap to image 2
                if (result != null)
                    imageView2.setImageBitmap(result);
            }
        });
    }
}
