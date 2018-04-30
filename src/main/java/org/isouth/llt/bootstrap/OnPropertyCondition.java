package org.isouth.llt.bootstrap;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Test if the property value matched
 *
 * @author qiyi
 * @since 1.0
 */
public class OnPropertyCondition implements Condition {
    private final String property;
    private final boolean defaultValue;

    public OnPropertyCondition(String property, boolean defaultValue) {
        this.property = property;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        return environment.getProperty(property, boolean.class, defaultValue);
    }
}
