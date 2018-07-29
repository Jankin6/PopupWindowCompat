package com.jankin.study.lib;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.jankin.study.lib.utli.OsUtils;

/**
 * 兼容android 7.x , 8.0 etc
 */

public class PopupWindowCompact extends PopupWindow {
    public PopupWindowCompact(Context context) {
        super(context);
    }

    public PopupWindowCompact(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PopupWindowCompact(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PopupWindowCompact(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PopupWindowCompact() {
    }

    public PopupWindowCompact(View contentView) {
        super(contentView);
    }

    public PopupWindowCompact(int width, int height) {
        super(width, height);
    }

    public PopupWindowCompact(View contentView, int width, int height) {
        super(contentView, width, height);
    }

    public PopupWindowCompact(View contentView, int width, int height, boolean focusable) {
        super(contentView, width, height, focusable);
    }

    @Override
    public void showAsDropDown(View anchor) {
        showAsDropDown(anchor, 0, 0);
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        showAsDropDown(anchor, xoff, yoff, Gravity.TOP | Gravity.START);
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {

        // 7.0 以下或者高度为WRAP_CONTENT, 默认显示
        if (Build.VERSION.SDK_INT < 24 || getHeight() == ViewGroup.LayoutParams.WRAP_CONTENT) {
            super.showAsDropDown(anchor, xoff, yoff, gravity);
        } else {
            if (getContentView().getContext() instanceof Activity) {
                Activity activity = (Activity) getContentView().getContext();
                int screenHeight;
                // 获取屏幕真实高度, 减掉虚拟按键的高度
                screenHeight = OsUtils.getContentHeight(activity);
                int[] location = new int[2];
                // 获取控件在屏幕的位置
                anchor.getLocationOnScreen(location);
                // 算出popwindow最大高度
                int maxHeight = screenHeight - location[1] - anchor.getHeight();
                // popupwindow  有具体的高度值，但是小于anchor下边缘与屏幕底部的距离， 正常显示
                if(getHeight() > 0 && getHeight() < maxHeight){
                    super.showAsDropDown(anchor, xoff, yoff, gravity);
                }else {
                    // match_parent 或者 popwinddow的具体高度值大于anchor下边缘与屏幕底部的距离， 都设置为最大可用高度
                    setHeight(maxHeight);
                    super.showAsDropDown(anchor, xoff, yoff, gravity);
                }

            }
        }


    }
}
