#!/bin/bash
set -e

VAULT_HOST=${VAULT_HOST:-vault}
VAULT_PORT=${VAULT_PORT:-8200}
TRUSTSTORE_PATH=/app/vault-truststore.jks
TRUSTSTORE_PASS=changeit

# Check if Vault URI is HTTP (no SSL)
if [[ "$SPRING_CLOUD_VAULT_URI" == http:* ]]; then
  echo "Vault is running over HTTP, skipping truststore generation."
  exec java -jar app.jar
else
  # Wait for Vault to be up (HTTPS)
  echo "Waiting for Vault at $VAULT_HOST:$VAULT_PORT..."
  until echo | openssl s_client -connect "$VAULT_HOST:$VAULT_PORT" 2>/dev/null | grep -q 'BEGIN CERTIFICATE'; do
    sleep 2
  done

  # Fetch Vault's cert
  echo "Fetching Vault cert from $VAULT_HOST:$VAULT_PORT..."
  echo -n | openssl s_client -connect "$VAULT_HOST:$VAULT_PORT" -servername "$VAULT_HOST" -showcerts 2>/dev/null | \
    sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /app/vault-cert.pem

  # Generate truststore
  echo "Generating truststore..."
  keytool -import -noprompt -alias vault -file /app/vault-cert.pem -keystore "$TRUSTSTORE_PATH" -storepass "$TRUSTSTORE_PASS"

  # Start the app with truststore
  exec java \
    -Dspring.cloud.vault.ssl.trust-store="$TRUSTSTORE_PATH" \
    -Dspring.cloud.vault.ssl.trust-store-password="$TRUSTSTORE_PASS" \
    -jar app.jar
fi
