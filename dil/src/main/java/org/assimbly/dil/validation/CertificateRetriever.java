package org.assimbly.dil.validation;

import java.util.List;

public interface CertificateRetriever {

    void addHttpsCertificatesToTrustStore(List<String> urls) throws Exception;

}
