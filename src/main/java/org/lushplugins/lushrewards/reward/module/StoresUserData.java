package org.lushplugins.lushrewards.reward.module;

import org.lushplugins.lushrewards.user.ModuleUserData;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StoresUserData {
    Class<? extends ModuleUserData> value();
}
