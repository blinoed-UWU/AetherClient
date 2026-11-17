package dev.aetherclient.client.module.setting;

import dev.aetherclient.client.i18n.I18n;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String id;
    private final String name;
    private String nameEn;
    private T value;
    private final T defaultValue;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String id, String name, T defaultValue) {
        this.id = id;
        this.name = name;
        this.nameEn = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return I18n.tr(name, nameEn);
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S withEn(String nameEn) {
        this.nameEn = nameEn;
        return (S) this;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    public void resetToDefault() {
        setValue(defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleIf(BooleanSupplier supplier) {
        this.visibility = supplier;
        return (S) this;
    }
}
