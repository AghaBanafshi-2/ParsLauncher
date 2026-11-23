package com.kdt.mcgui;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import net.kdt.pojavlaunch.R;

import fr.spse.extended_view.ExtendedButton;

public class LauncherMenuButton extends ExtendedButton implements SharedPreferences.OnSharedPreferenceChangeListener {

    public LauncherMenuButton(@NonNull Context context) {
        super(context);
        setSettings();
    }
    public LauncherMenuButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setSettings();
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); android.content.SharedPreferences p = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext()); p.registerOnSharedPreferenceChangeListener(this); applyRoundedBackground(); if (getAlpha() == 1f) { setAlpha(0f); setTranslationY(12f); animate().alpha(1f).translationY(0f).setDuration(360).start(); } }
    @Override protected void onDetachedFromWindow() { android.preference.PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this); super.onDetachedFromWindow(); }
    @Override public void onSharedPreferenceChanged(SharedPreferences p, String key) { if ("pars_button_radius".equals(key)) applyRoundedBackground(); }

    private void applyRoundedBackground() {
        int radius = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("pars_button_radius", 22);
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(radius * density);
        background.setColor(Color.rgb(43, 27, 63));
        setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(65, 190, 150, 255)),
                background,
                null
        ));
    }

    /** Set style stuff */
    private void setSettings(){
        Resources resources = getContext().getResources();

        int padding = resources.getDimensionPixelSize(R.dimen._22sdp);
        setCompoundDrawablePadding(padding);
        setPaddingRelative(padding, 0, 0, 0);
        setGravity(Gravity.CENTER_VERTICAL);

        setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen._12ssp));

        // Set drawable size
        int[] sizes = getExtendedViewData().getSizeCompounds();
        sizes[0] = resources.getDimensionPixelSize(R.dimen._30sdp);
        getExtendedViewData().setSizeCompounds(sizes);
        postProcessDrawables();
        applyRoundedBackground();
    }
}
