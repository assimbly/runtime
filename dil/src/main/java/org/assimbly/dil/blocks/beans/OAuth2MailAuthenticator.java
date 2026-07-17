package org.assimbly.dil.blocks.beans;

import jakarta.mail.PasswordAuthentication;
import org.apache.camel.component.mail.MailAuthenticator;
import org.assimbly.tenantvariables.domain.TenantVariable;
import org.assimbly.tenantvariables.mongo.MongoDao;

public class OAuth2MailAuthenticator extends MailAuthenticator {

    private final String username;
    private final String accessToken;
    private final String tenant;
    private final boolean isConsumer;

    public OAuth2MailAuthenticator(String username, String accessToken, String tenant, boolean isConsumer) {
        this.username = username;
        this.accessToken = accessToken;
        this.tenant = tenant;
        this.isConsumer = isConsumer;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        TenantVariable.TenantVarType tenantVarType = isConsumer ? TenantVariable.TenantVarType.TENANT_VARIABLE : TenantVariable.TenantVarType.STATIC_TENANT_VARIABLE;
        String interpolatedAccessToken = MongoDao.interpolatePossibleTenantVariable(this.accessToken, this.tenant, tenantVarType);

        return new PasswordAuthentication(this.username, interpolatedAccessToken);
    }
}