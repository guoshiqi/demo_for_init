package com.gs.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.gs.myapplication.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ImageView checkView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkView = findViewById(R.id.check);
        checkView.setBackgroundResource(R.drawable.abc);


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();


        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Drawable drawable = checkView.getBackground();
                Bitmap bitmap = getBitmap(drawable);
                Bitmap topCrop = cropBitmap(bitmap, true);
                Bitmap topResult = rsBlur(MainActivity.this, topCrop, 25);
                checkView.setBackgroundResource(0);
                checkView.setImageDrawable(new BitmapDrawable(getResources(), topResult));
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private byte[] generateChunkByte(Bitmap bitmap) {
        int[] xRegions = new int[]{bitmap.getWidth() / 2, bitmap.getWidth() / 2 + 1};
        int[] yRegions = new int[]{bitmap.getWidth() / 2, bitmap.getWidth() / 2 + 1};
        int NO_COLOR = 0x00000001;
        int colorSize = 9;
        int bufferSize = xRegions.length * 4 + yRegions.length * 4 + colorSize * 4 + 32;

        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.nativeOrder());
// 第一个byte，要不等于0
        byteBuffer.put((byte) 1);

//mDivX length
        byteBuffer.put((byte) 2);
//mDivY length
        byteBuffer.put((byte) 2);
//mColors length
        byteBuffer.put((byte) colorSize);

//skip
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);

//padding 先设为0
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);

//skip
        byteBuffer.putInt(0);

// mDivX
        byteBuffer.putInt(xRegions[0]);
        byteBuffer.putInt(xRegions[1]);

// mDivY
        byteBuffer.putInt(yRegions[0]);
        byteBuffer.putInt(yRegions[1]);

// mColors
        for (int i = 0; i < colorSize; i++) {
            byteBuffer.putInt(NO_COLOR);
        }

        return byteBuffer.array();
    }

    /**
     * 裁剪中间
     *
     * @param srcBmp 原图
     * @return 裁剪后的图像
     */
    public static Bitmap cropBitmap(Bitmap srcBmp, boolean top) {
        Bitmap dstBmp;
        dstBmp = Bitmap.createBitmap(
                srcBmp,
                0,
                top ? 0 : srcBmp.getHeight() - srcBmp.getWidth() / 18,
                srcBmp.getWidth(),
                srcBmp.getWidth() / 18
        );
        return dstBmp;
    }

    /**
     * 高斯模糊
     * @param context
     * @param source
     * @param radius
     * @return
     */
    public static Bitmap rsBlur(Context context, Bitmap source, int radius){
        Bitmap inputBmp = source;
        RenderScript renderScript =  RenderScript.create(context);
        // Allocate memory for Renderscript to work with
        final Allocation input = Allocation.createFromBitmap(renderScript,inputBmp);
        final Allocation output = Allocation.createTyped(renderScript,input.getType());
        // Load up an instance of the specific script that we want to use.
        ScriptIntrinsicBlur scriptIntrinsicBlur = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            scriptIntrinsicBlur.setInput(input);
            // Set the blur radius
            scriptIntrinsicBlur.setRadius(radius);
            // Start the ScriptIntrinisicBlur
            scriptIntrinsicBlur.forEach(output);
            // Copy the output to the blurred bitmap
        }
        output.copyTo(inputBmp);
        renderScript.destroy();
        return inputBmp;
    }

    private Bitmap getBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}