package org.assimbly.broker.decoder;

import org.apache.activemq.artemis.utils.SensitiveDataCodec;
import org.assimbly.util.EncryptionUtil;

import java.util.Map;

public class AssimblyCodec implements SensitiveDataCodec<String> {

   @Override
   public String decode(Object mask) throws Exception {
      // Decode the mask into clear text password.
      EncryptionUtil encryptionUtil = new EncryptionUtil(EncryptionUtil.key, EncryptionUtil.algorithm);

      return encryptionUtil.decrypt(mask.toString());

   }

   @Override
   public String encode(Object secret) throws Exception {
      // encoding is done by environment variables
      return secret.toString();
   }

   @Override
   public void init(Map<String, String> params) {
      // Initialization done here. It is called right after the codec has been created.
   }

}