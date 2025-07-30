package org.lushplugins.lushrewards.utils.placeholderhandler;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.placeholderhandler.parameter.ParameterProvider;
import org.lushplugins.placeholderhandler.placeholder.PlaceholderContext;

public class RewardModuleParameterProvider<T extends RewardModule> implements ParameterProvider<T> {

    @Override
    public T collect(Class<T> type, String parameter, PlaceholderContext context) {
        return LushRewards.getInstance().getRewardModuleManager().getModule(parameter, type);
    }
}
