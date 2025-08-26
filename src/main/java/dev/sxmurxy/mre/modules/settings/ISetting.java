package dev.sxmurxy.mre.modules.settings;

import dev.sxmurxy.mre.modules.Module;

public interface ISetting<T> {
    T get();
    void set(T value);
    String getName();
    Module getModule();
}