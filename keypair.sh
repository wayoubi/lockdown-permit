keytool -genkeypair -alias tasree7KeyPair -keyalg RSA -keysize 2048 \
  -dname "CN=modee.gov.jo" -validity 365 -storetype PKCS12 \
  -keystore tasree7_keystore.p12 -storepass changeit