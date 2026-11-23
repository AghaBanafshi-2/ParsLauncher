package com.kdt.mcgui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.core.content.res.ResourcesCompat;

import net.kdt.pojavlaunch.R;

public class MineButton extends androidx.appcompat.widget.AppCompatButton
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String PREF_BUTTON_RADIUS = "pars_button_radius";
    private static final int DEFAULT_BUTTON_RADIUS = 22;

    public MineButton(Context ctx) {
        this(ctx, null);
    }

    public MineButton(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        setTypeface(ResourcesCompat.getFont(getContext(), R.font.noto_sans_bold));
        setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen._13ssp));
        applyRoundedBackground();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getPreferences().registerOnSharedPreferenceChangeListener(this);
        applyRoundedBackground();
    }

    @Override
    protected void onDetachedFromWindow() {
        getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (PREF_BUTTON_RADIUS.equals(key)) {
            applyRoundedBackground();
        }
    }

    private SharedPreferences getPreferences() {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    private void applyRoundedBackground() {
        float density = getResources().getDisplayMetrics().density;
        int radius = getPreferences().getInt(PREF_BUTTON_RADIUS, DEFAULT_BUTTON_RADIUS);

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
}
