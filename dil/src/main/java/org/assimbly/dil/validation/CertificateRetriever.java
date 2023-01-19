package org.assimbly.dil.validation;

import java.util.List;

public interface CertificateRetriever {

    public void addHttpsCertificatesToTrustStore(List<String> urls) throws Exception;

}
