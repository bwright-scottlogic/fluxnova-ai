package org.finos.fluxnova.ai.mcp.security;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OAuth2ClientsConfiguredCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        BeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory == null) {
            return ConditionOutcome.noMatch("No BeanFactory available");
        }

        OAuth2ClientProperties properties = beanFactory
                .getBeanProvider(OAuth2ClientProperties.class)
                .getIfAvailable();

        if (properties != null && !properties.getRegistration().isEmpty()) {
            return ConditionOutcome.match("OAuth2 client registrations configured");
        }

        return ConditionOutcome.noMatch("No OAuth2 client registrations configured");
    }
}
