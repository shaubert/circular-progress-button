package com.dd;

import com.dd.circular.progress.button.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.StateSet;
import android.widget.Button;

import java.util.HashMap;
import java.util.Map;

public class CircularProgressButton extends Button {

    public static final int IDLE_STATE_PROGRESS = 0;
    public static final int ERROR_STATE_PROGRESS = -1;
    public static final int SUCCESS_STATE_PROGRESS = 100;

    private StrokeGradientDrawable sharedBackground;
    private CircularAnimatedDrawable mAnimatedDrawable;
    private CircularProgressDrawable mProgressDrawable;

    private StateManager mStateManager;
    private State mState;
    private Map<StateType, State> states = new HashMap<>(4);

    private int mColorIndicator;
    private boolean mIndeterminateProgressMode;
    private int mPaddingProgress;

    private int mStrokeWidth;
    private float mCornerRadius;

    private int mMaxProgress;
    private int mProgress;

    private State mDestState;
    private MorphingAnimation morphingAnimation;
    private boolean mMorphingInProgress;

    public CircularProgressButton(Context context) {
        super(context);
        init(context, null);
    }

    public CircularProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircularProgressButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {
        initStates(context, attributeSet);

        mMaxProgress = SUCCESS_STATE_PROGRESS;
        mState = states.get(StateType.IDLE);
        mStateManager = new StateManager(this);

        setIconOrText(mState);
        setBackgroundCompat(mState.background);
    }

    private void initStates(Context context, AttributeSet attributeSet) {
        int blue = getColor(R.color.cpb_blue);
        int white = getColor(R.color.cpb_white);
        int grey = getColor(R.color.cpb_grey);

        TypedArray attr = getTypedArray(context, attributeSet, R.styleable.CircularProgressButton);
        try {
            mColorIndicator = attr != null ? attr.getColor(R.styleable.CircularProgressButton_cpb_colorIndicator, blue) : blue;
            mStrokeWidth = getContext().getResources().getDimensionPixelSize(R.dimen.cpb_stroke_width);

            if (attr != null) {
                mCornerRadius = attr.getDimension(R.styleable.CircularProgressButton_cpb_cornerRadius, 0);
                mPaddingProgress = attr.getDimensionPixelSize(R.styleable.CircularProgressButton_cpb_paddingProgress, 0);
                mStrokeWidth = attr.getDimensionPixelSize(R.styleable.CircularProgressButton_cpb_strokeWidth, mStrokeWidth);
            }

            CharSequence idleText = attr != null ? attr.getString(R.styleable.CircularProgressButton_cpb_textIdle) : null;
            int idleIconResId = attr != null ? attr.getResourceId(R.styleable.CircularProgressButton_cpb_iconIdle, 0) : 0;
            int idleColorState = attr != null ? attr.getResourceId(R.styleable.CircularProgressButton_cpb_selectorIdle,
                    R.color.cpb_idle_state_selector) : R.color.cpb_idle_state_selector;
            State idle = new State(StateType.IDLE,
                    idleIconResId > 0 ? getDrawable(idleIconResId) : null,
                    idleText,
                    getResources().getColorStateList(idleColorState));
            initSharedBackground(idle);
            setupBackground(idle, true);

            CharSequence completeText = attr != null ? attr.getString(R.styleable.CircularProgressButton_cpb_textComplete) : null;
            int completeIconResId = attr != null ? attr.getResourceId(R.styleable.CircularProgressButton_cpb_iconComplete, 0) : 0;
            int completeColorState = attr != null ? attr.getResourceId(R.styleable.CircularProgressButton_cpb_selectorComplete,
                    R.color.cpb_complete_state_selector) : R.color.cpb_complete_state_selector;
            State complete = new State(StateType.COMPLETE,
                    completeIconResId > 0 ? getDrawable(completeIconResId) : null,
                    completeText,
                    getResources().getColorStateList(completeColorState));
            setupBackground(complete, true);

            CharSequence errorText = attr != null ? attr.getString(R.styleable.CircularProgressButton_cpb_textError) : null;
            int errorIconResId = attr != null ? attr.getResourceId(R.styleable.CircularProgressButton_cpb_iconError, 0) : 0;
            int errorColorState = attr != null ? attr.getResourceId(R.styleable.CircularProgressButton_cpb_selectorError,
                    R.color.cpb_error_state_selector) : R.color.cpb_error_state_selector;
            State error = new State(StateType.ERROR,
                    errorIconResId > 0 ? getDrawable(errorIconResId) : null,
                    errorText,
                    getResources().getColorStateList(errorColorState));
            setupBackground(error, true);

            int colorProgress = attr != null ? attr.getColor(R.styleable.CircularProgressButton_cpb_colorProgress, white) : white;
            CharSequence progressText = attr != null ? attr.getString(R.styleable.CircularProgressButton_cpb_textProgress) : null;
            int progressIconResId = attr != null ? attr.getResourceId(R.styleable.CircularProgressButton_cpb_iconProgress, 0) : 0;
            State progress = new State(StateType.PROGRESS,
                    progressIconResId > 0 ? getDrawable(progressIconResId) : null,
                    progressText,
                    ColorStateList.valueOf(colorProgress));
            progress.strokeColor = attr != null ? attr.getColor(R.styleable.CircularProgressButton_cpb_colorIndicatorBackground, grey) : grey;
            progress.background = sharedBackground.getGradientDrawable();

            states.put(StateType.IDLE, idle);
            states.put(StateType.PROGRESS, progress);
            states.put(StateType.COMPLETE, complete);
            states.put(StateType.ERROR, error);
        } finally {
            if (attr != null) {
                attr.recycle();
            }
        }
    }

    private void setupBackground(State state, boolean allStates) {
        StateListDrawable drawable = new StateListDrawable();
        if (allStates) {
            int colorDisabled = state.getDisabledColor();
            drawable.addState(new int[]{-android.R.attr.state_enabled}, createDrawable(colorDisabled).getGradientDrawable());
        }
        int colorPressed = state.getPressedColor();
        drawable.addState(new int[]{android.R.attr.state_pressed}, createDrawable(colorPressed).getGradientDrawable());
        if (allStates) {
            int colorFocused = state.getFocusedColor();
            drawable.addState(new int[]{android.R.attr.state_focused}, createDrawable(colorFocused).getGradientDrawable());
        }
        drawable.addState(StateSet.WILD_CARD, sharedBackground.getGradientDrawable());
        state.background = drawable;
    }

    private void initSharedBackground(State idleState) {
        if (sharedBackground == null) {
            int colorNormal = idleState.getNormalColor();
            sharedBackground = createDrawable(colorNormal);
        }
    }

    private StrokeGradientDrawable createDrawable(int color) {
        GradientDrawable drawable = (GradientDrawable) getDrawable(R.drawable.cpb_background).mutate();
        drawable.setColor(color);
        drawable.setCornerRadius(mCornerRadius);
        StrokeGradientDrawable strokeGradientDrawable = new StrokeGradientDrawable(drawable);
        strokeGradientDrawable.setStrokeColor(color);
        strokeGradientDrawable.setStrokeWidth(mStrokeWidth);
        return strokeGradientDrawable;
    }

    private Drawable getDrawable(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return getContext().getDrawable(resId);
        } else {
            return getResources().getDrawable(resId);
        }
    }

    protected int getColor(int id) {
        return getResources().getColor(id);
    }

    protected TypedArray getTypedArray(Context context, AttributeSet attributeSet, int[] attr) {
        return context.obtainStyledAttributes(attributeSet, attr, 0, 0);
    }

    private State getState(StateType stateType) {
        return states.get(stateType);
    }

    @Override
    protected void drawableStateChanged() {
        if (mState != null) {
            mState.background.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mState != null) {
            mState.background.jumpToCurrentState();
        }
        if (mAnimatedDrawable != null) {
            mAnimatedDrawable.jumpToCurrentState();
        }
        if (mProgressDrawable != null) {
            mProgressDrawable.jumpToCurrentState();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mProgress > 0 && mState.type == StateType.PROGRESS && !mMorphingInProgress) {
            if (mIndeterminateProgressMode) {
                drawIndeterminateProgress(canvas);
            } else {
                drawProgress(canvas);
            }
        }
    }

    private void drawIndeterminateProgress(Canvas canvas) {
        if (mAnimatedDrawable == null) {
            int offset = (getWidth() - getHeight()) / 2;
            mAnimatedDrawable = new CircularAnimatedDrawable(mColorIndicator, mStrokeWidth);
            int left = offset + mPaddingProgress;
            int right = getWidth() - offset - mPaddingProgress;
            int bottom = getHeight() - mPaddingProgress;
            int top = mPaddingProgress;
            mAnimatedDrawable.setBounds(left, top, right, bottom);
            mAnimatedDrawable.setCallback(this);
            mAnimatedDrawable.start();
        } else {
            mAnimatedDrawable.draw(canvas);
        }
    }

    private void drawProgress(Canvas canvas) {
        if (mProgressDrawable == null) {
            int offset = (getWidth() - getHeight()) / 2;
            int size = getHeight() - mPaddingProgress * 2;
            mProgressDrawable = new CircularProgressDrawable(size, mStrokeWidth, mColorIndicator);
            int left = offset + mPaddingProgress;
            mProgressDrawable.setBounds(left, mPaddingProgress, left, mPaddingProgress);
        }
        float sweepAngle = (360f / mMaxProgress) * mProgress;
        mProgressDrawable.setSweepAngle(sweepAngle);
        mProgressDrawable.draw(canvas);
    }

    public boolean isIndeterminateProgressMode() {
        return mIndeterminateProgressMode;
    }

    public void setIndeterminateProgressMode(boolean indeterminateProgressMode) {
        this.mIndeterminateProgressMode = indeterminateProgressMode;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        boolean superRes = super.verifyDrawable(who);
        if (!superRes) {
            if (mAnimatedDrawable != null) superRes |= who == mAnimatedDrawable;
            if (mProgressDrawable != null) superRes |= who == mProgressDrawable;
            if (sharedBackground != null) superRes |= who == sharedBackground.getGradientDrawable();
            if (mState != null && mState.background != null) superRes |= who == mState.background;
        }
        return superRes;
    }

    private MorphingAnimation createSquareMorphing(boolean instant) {
        return createMorphing(mCornerRadius, mCornerRadius, getWidth(), getWidth(), 0, instant);
    }

    private MorphingAnimation createMorphing(float fromCorner, float toCorner, int fromWidth, int toWidth, int padding, boolean instant) {
        MorphingAnimation animation = new MorphingAnimation(this, sharedBackground);
        animation.setFromCornerRadius(fromCorner);
        animation.setToCornerRadius(toCorner);
        animation.setPadding(padding);
        animation.setFromWidth(fromWidth);
        animation.setToWidth(toWidth);

        if (instant) {
            animation.setDuration(MorphingAnimation.DURATION_INSTANT);
        } else {
            animation.setDuration(MorphingAnimation.DURATION_NORMAL);
        }

        return animation;
    }

    private void morphTo(State destState, boolean instant) {
        if (this.mDestState == destState) {
            return;
        }
        this.mDestState = destState;

        if (destState == mState) {
            instant = true;
        }

        if (mMorphingInProgress && morphingAnimation != null) {
            morphingAnimation.cancel();
        }

        if (mState.type == StateType.PROGRESS) {
            setText(null);
            setIcon(destState.icon);
            morphingAnimation = createMorphing(getHeight(), mCornerRadius, getHeight(), getWidth(), mPaddingProgress, instant);
        } else if (destState.type == StateType.PROGRESS) {
            setWidth(getWidth());
            setIconOrText(destState);
            morphingAnimation = createMorphing(mCornerRadius, getHeight(), getWidth(), getHeight(), mPaddingProgress, instant);
        } else {
            setIconOrText(destState);
            morphingAnimation = createSquareMorphing(instant);
        }

        setBackgroundCompat(sharedBackground.getGradientDrawable());

        morphingAnimation.setFromColor(mState.getNormalColor());
        morphingAnimation.setToColor(destState.getNormalColor());

        morphingAnimation.setFromStrokeColor(mState.strokeColor);
        morphingAnimation.setToStrokeColor(destState.strokeColor);

        morphingAnimation.setListener(createOnAnimationEndListener(destState));

        mMorphingInProgress = true;
        morphingAnimation.start();
    }

    private OnAnimationEndListener createOnAnimationEndListener(final State destState) {
        return new OnAnimationEndListener() {
            @Override
            public void onAnimationEnd() {
                mState = destState;
                setIconOrText(mState);
                setBackgroundCompat(mState.background);
                mMorphingInProgress = false;
                mStateManager.checkState(CircularProgressButton.this);
            }
        };
    }

    private void setIconOrText(State state) {
        if (state.icon != null) {
            setIcon(state.icon);
            setText(null);
        } else {
            removeIcon();
            setText(state.text);
        }
    }

    private void setIcon(Drawable icon) {
        if (icon != null) {
            int padding = (getWidth() / 2) - (icon.getIntrinsicWidth() / 2);
            setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            setPadding(padding, 0, 0, 0);
        } else {
            removeIcon();
        }
    }

    protected void removeIcon() {
        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        setPadding(0, 0, 0, 0);
    }

    /**
     * Set the View's background. Masks the API changes made in Jelly Bean.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public void setBackgroundCompat(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
    }

    public void setProgress(int progress) {
        setProgress(progress, false);
    }

    public void setProgress(int progress, boolean instant) {
        mProgress = progress;

        if (mMorphingInProgress || getWidth() == 0) {
            return;
        }

        mStateManager.saveProgress(this);

        if (mProgress >= mMaxProgress) {
            morphTo(getState(StateType.COMPLETE), instant);
        } else if (mProgress > IDLE_STATE_PROGRESS) {
            morphTo(getState(StateType.PROGRESS), instant);
        } else if (mProgress == ERROR_STATE_PROGRESS) {
            morphTo(getState(StateType.ERROR), instant);
        } else if (mProgress == IDLE_STATE_PROGRESS) {
            morphTo(getState(StateType.IDLE), instant);
        }
    }

    public int getProgress() {
        return mProgress;
    }

    public void setBackgroundColor(int color) {
        sharedBackground.getGradientDrawable().setColor(color);
    }

    public void setStrokeColor(int color) {
        sharedBackground.setStrokeColor(color);
    }

    public CharSequence getText(StateType type) {
        return getState(type).text;
    }

    public void setText(StateType type, CharSequence text) {
        getState(type).text = text;
        invalidateTextAndIcon();
    }

    public Drawable getIcon(StateType type) {
        return getState(type).icon;
    }

    public void setIcon(StateType type, Drawable icon) {
        getState(type).icon = icon;
        invalidateTextAndIcon();
    }

    private void invalidateTextAndIcon() {
        if (!mMorphingInProgress) {
            setIconOrText(mState);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mProgressDrawable = null;
            mAnimatedDrawable = null;

            if (mMorphingInProgress) {
                mMorphingInProgress = false;
                if (morphingAnimation != null) {
                    morphingAnimation.cancel();
                }
            }

            setProgress(mProgress, true);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.mProgress = mProgress;
        savedState.mIndeterminateProgressMode = mIndeterminateProgressMode;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mProgress = savedState.mProgress;
            mIndeterminateProgressMode = savedState.mIndeterminateProgressMode;
            super.onRestoreInstanceState(savedState.getSuperState());
            setProgress(mProgress, true);
        } else {
            super.onRestoreInstanceState(state);
        }
    }


    static class SavedState extends BaseSavedState {

        private boolean mIndeterminateProgressMode;
        private int mProgress;

        public SavedState(Parcelable parcel) {
            super(parcel);
        }

        private SavedState(Parcel in) {
            super(in);
            mProgress = in.readInt();
            mIndeterminateProgressMode = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mProgress);
            out.writeInt(mIndeterminateProgressMode ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public enum StateType {
        PROGRESS, IDLE, COMPLETE, ERROR
    }

    private static class State {
        StateType type;
        Drawable icon;
        CharSequence text;
        ColorStateList colorStateList;

        Drawable background;
        int strokeColor;

        public State(StateType type, Drawable icon, CharSequence text, ColorStateList colorStateList) {
            this.type = type;
            this.icon = icon;
            this.text = text;
            this.colorStateList = colorStateList;

            strokeColor = getNormalColor();
        }

        private int getColor(int[] state) {
            return colorStateList.getColorForState(state, getNormalColor());
        }

        private int getNormalColor() {
            return colorStateList.getColorForState(new int[]{android.R.attr.state_enabled}, 0);
        }

        private int getPressedColor() {
            return colorStateList.getColorForState(new int[]{android.R.attr.state_pressed}, 0);
        }

        private int getFocusedColor() {
            return colorStateList.getColorForState(new int[]{android.R.attr.state_focused}, 0);
        }

        private int getDisabledColor() {
            return colorStateList.getColorForState(new int[]{-android.R.attr.state_enabled}, 0);
        }
    }
}
