package dev.aetherclient.client.module.setting;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    public float visualRatio = -1f;

    public NumberSetting(String id, String name, double defaultValue, double min, double max, double step) {
        super(id, name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public float asFloat() {
        return getValue().floatValue();
    }

    @Override
    public void setValue(Double raw) {
        if (raw == null || !Double.isFinite(raw)) return;
        double clamped = Math.max(min, Math.min(max, raw));
        if (step > 0) {
            // Snap ticks relative to min so that min itself is always reachable
            // even when it is not a multiple of step.
            long ticks = Math.round((clamped - min) / step);
            clamped = min + java.math.BigDecimal.valueOf(ticks)
                    .multiply(java.math.BigDecimal.valueOf(step))
                    .doubleValue();
            clamped = Math.max(min, Math.min(max, clamped));
        }
        super.setValue(clamped);
        cachedFormatted = null;
    }

    /** Cached display string, invalidated on setValue; rebuilt at most once per value change. */
    private String cachedFormatted;

    public String formatted() {
        String cached = cachedFormatted;
        if (cached != null) return cached;
        String out;
        if (step >= 1.0) {
            out = Long.toString(Math.round(getValue()));
        } else if (step >= 0.1) {
            out = String.format(java.util.Locale.ROOT, "%.1f", getValue());
        } else {
            out = String.format(java.util.Locale.ROOT, "%.2f", getValue());
        }
        cachedFormatted = out;
        return out;
    }
}
