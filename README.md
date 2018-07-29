# PopupWindowCompact
兼容android 7.x , 8.0 etc的Popwindow

##问题描述##
前段时间发现Popupwindow在8.0的手机上显示成全屏了，搜了下发现7.0以上就有这个问题了，好久没写Popwindow了，才知道（尴尬）。于是总结了在以下情况可能出问题：

1. 当设置PopupWindow 的高度为 MATCH_PARENT，调用 showAsDropDown(View anchor) 时，在 7.0 之前，会在 anchor 下边缘到屏幕底部之间显示 PopupWindow；而在 7.0系统上（包括7.1, 8.0）的 PopupWindow 会占据整个屏幕（除状态栏之外）。
2. 当设置PopupWindow 的高度为 WRAP_CONTENT，调用 showAsDropDown(View anchor) 时，没有兼容性问题。
3. 当设置PopupWindow 的高度为自定义的值height，调用 showAsDropDown(View anchor)时， 如果 height > anchor 下边缘与屏幕底部的距离， 则还是会出现7.0以上显示异常的问题；否则，不会出现该问题。

##问题解决##
好了，现在我们知道问题出现的情况了，上网搜了很多解决方案，发现都不能完美解决问题，如以下代码：

```
if (Build.VERSION.SDK_INT >= 24) {
     int[] location = new int[2];
     anchor.getLocationOnScreen(location);
     // 7.1 版本处理
     if (Build.VERSION.SDK_INT == 25) {
         WindowManager windowManager = (WindowManager) pw.getContentView().getContext().getSystemService(Context.WINDOW_SERVICE);
         if (windowManager != null) {
             int screenHeight = windowManager.getDefaultDisplay().getHeight();
             // PopupWindow height for match_parent, will occupy the entire screen, it needs to do special treatment in Android 7.1
             pw.setHeight(screenHeight - location[1] - anchor.getHeight() - yoff);
         }
     }
     pw.showAtLocation(anchor, Gravity.NO_GRAVITY, xoff, location[1] + anchor.getHeight() + yoff);

 } else {
     pw.showAsDropDown(anchor, xoff, yoff);
 }
```
或者这种代码：

```
public static void showAsDropDown(final PopupWindow pw, final View anchor, final int xoff, final int yoff) {
    if (Build.VERSION.SDK_INT >= 24) {
        Rect visibleFrame = new Rect();
        anchor.getGlobalVisibleRect(visibleFrame);
        int height = anchor.getResources().getDisplayMetrics().heightPixels - visibleFrame.bottom;
        pw.setHeight(height);
        pw.showAsDropDown(anchor, xoff, yoff);
    } else {
        pw.showAsDropDown(anchor, xoff, yoff);
    }
}
```
这两段代码的主要问题是，没有考虑虚拟按键的情况，屏幕的高度算的不一定对，或者有虚拟按键，虚拟按键是否显示都会影响屏幕的可用高度。所以问题的关键是算出屏幕真实的可用高度。

为了方便以后复用，我们重新定义一个PopupWindowCompact继承自PopupWindow，具体代码如下，主要是综合考虑以上三种情况，和算出屏幕的真实可用高度，注释已经写的很清楚了吧，我就省点字了（偷懒）。

获取屏幕真实总高度：

```
public static int getScreenHeight(Activity activity) {
        if (activity == null) {
            return 0;
        }
        Display display = activity.getWindowManager().getDefaultDisplay();
        int realHeight = 0;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            realHeight = metrics.heightPixels;
        } else {
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                realHeight = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return realHeight;
    }
```

判断虚拟按键是否显示：

```
	/**
     * 虚拟按键是否打开
     *
     * @param activity
     * @return
     */
    public static boolean isNavigationBarShown(Activity activity) {
        //虚拟键的view,为空或者不可见时是隐藏状态
        View view = activity.findViewById(android.R.id.navigationBarBackground);
        if (view == null) {
            return false;
        }
        int visible = view.getVisibility();
        if (visible == View.GONE || visible == View.INVISIBLE) {
            return false;
        } else {
            return true;
        }
    }
```

获取虚拟按键真实高度：

```
	/**
     * 获取当前虚拟键高度(隐藏后高度为0)
     *
     * @param activity
     * @return
     */
    public static int getCurrentNavigationBarHeight(Activity activity) {
        if (isNavigationBarShown(activity)) {
            return getNavigationBarHeight(activity);
        } else {
            return 0;
        }
    }
```

获取屏幕可用高度：

```
	/**
     * 获取可用屏幕高度，排除虚拟键
     *
     * @param context 上下文
     * @return 返回高度
     */
    public static int getContentHeight(Activity context) {
        int contentHeight = getScreenHeight(context) - getCurrentNavigationBarHeight(context);
        return contentHeight;
    }

```

自定义的PopupWindowCompact的主要代码如下：

```
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
```
