#!/usr/bin/env bash
# ==============================================================================
# Gera certificados SSL no modelo produção:
#   1. CA (Certificate Authority) auto-assinada
#   2. Keystore do broker assinado pela CA
#   3. Keystore do cliente assinado pela CA
#   4. Truststore do cliente contendo apenas o certificado da CA
#
# Pre-requisito: JDK 21+ instalado (o comando keytool faz parte do JDK).
#
# Uso:
#   ./generate-certs.sh [OPCOES]
#
# Opções (todas opcionais):
#   --app-name  <valor>   Nome da aplicação cliente, usado como CN (default: kafka-client)
#   --org       <valor>   Nome da organização (default: MyOrg)
#   --location  <valor>   Cidade (default: City)
#   --state     <valor>   Estado (default: State)
#   --country   <valor>   Código do pais, 2 letras (default: BR)
#   --password  <valor>   Senha dos keystores (default: changeit)
#   --validity  <valor>   Validade em dias (default: 365)
#
# Exemplos:
#   ./generate-certs.sh
#   ./generate-certs.sh --app-name demo-kafka-skill
#   ./generate-certs.sh --app-name meu-servico --org MinhaEmpresa --location SaoPaulo --state SP
#   ./generate-certs.sh --password minha-senha --validity 730
#
# Os arquivos gerados ficam no diretorio certs/ na raiz do projeto.
# ==============================================================================

set -euo pipefail

# ==============================================================================
# Valores default (podem ser sobrescritos via parametros)
# ==============================================================================
APP_NAME="kafka-client"
ORG="MyOrg"
LOCATION="City"
STATE="State"
COUNTRY="BR"
STORE_PASS="changeit"
VALIDITY=365

# ==============================================================================
# Parse de argumentos
# ==============================================================================
while [[ $# -gt 0 ]]; do
  case "$1" in
    --app-name)  APP_NAME="$2";  shift 2 ;;
    --org)       ORG="$2";       shift 2 ;;
    --location)  LOCATION="$2";  shift 2 ;;
    --state)     STATE="$2";     shift 2 ;;
    --country)   COUNTRY="$2";   shift 2 ;;
    --password)  STORE_PASS="$2"; shift 2 ;;
    --validity)  VALIDITY="$2";  shift 2 ;;
    -h|--help)
      head -n 30 "$0" | tail -n +2 | sed 's/^# \?//'
      exit 0
      ;;
    *)
      echo "Opcao desconhecida: $1 (use --help para ver as opcoes)"
      exit 1
      ;;
  esac
done

# ==============================================================================
# Verificar se keytool esta disponivel
# ==============================================================================
if ! command -v keytool &>/dev/null; then
  echo "ERRO: 'keytool' nao encontrado. Instale o JDK 21+ e certifique-se de que esta no PATH."
  echo "  Download: https://jdk.java.net/"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CERTS_DIR="${SCRIPT_DIR}/certs"
mkdir -p "$CERTS_DIR"
cd "$CERTS_DIR"

# Configuracoes derivadas
CA_ALIAS="ca-root"
BROKER_ALIAS="kafka-broker"
CLIENT_ALIAS="${APP_NAME}"
KEY_PASS="$STORE_PASS"
DNAME_CA="CN=Kafka-CA,OU=Infra,O=${ORG},L=${LOCATION},ST=${STATE},C=${COUNTRY}"
DNAME_BROKER="CN=localhost,OU=Kafka,O=${ORG},L=${LOCATION},ST=${STATE},C=${COUNTRY}"
DNAME_CLIENT="CN=${APP_NAME},OU=App,O=${ORG},L=${LOCATION},ST=${STATE},C=${COUNTRY}"
SAN="SAN=DNS:localhost,DNS:kafka,IP:127.0.0.1"

echo ""
echo "Configuracao:"
echo "  App (CN):    $APP_NAME"
echo "  Organizacao: $ORG"
echo "  Local:       $LOCATION, $STATE, $COUNTRY"
echo "  Validade:    $VALIDITY dias"
echo "  Senha:       $STORE_PASS"
echo ""

echo "==> Limpando certificados anteriores..."
rm -f ca-root.p12 ca-root.crt ca-root.key \
      kafka.keystore.p12 broker.csr broker-signed.crt \
      kafka.client-keystore.p12 client.csr client-signed.crt \
      kafka.truststore.p12 ssl_credentials

# ==============================================================================
# 1. Gerar a CA (Certificate Authority)
# ==============================================================================
echo "==> [1/6] Gerando CA (Certificate Authority)..."
keytool -genkeypair -alias "$CA_ALIAS" -keyalg RSA -keysize 4096 -validity "$VALIDITY" \
  -dname "$DNAME_CA" \
  -keystore ca-root.p12 -storetype PKCS12 \
  -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
  -ext bc=ca:true

# Exportar o certificado da CA (sera usado no truststore do cliente)
keytool -exportcert -alias "$CA_ALIAS" -keystore ca-root.p12 \
  -storepass "$STORE_PASS" -file ca-root.crt -rfc

echo "    CA gerada: ca-root.p12 / ca-root.crt"

# ==============================================================================
# 2. Gerar keystore do broker e CSR (Certificate Signing Request)
# ==============================================================================
echo "==> [2/6] Gerando keystore do broker..."
keytool -genkeypair -alias "$BROKER_ALIAS" -keyalg RSA -keysize 2048 -validity "$VALIDITY" \
  -dname "$DNAME_BROKER" \
  -ext "$SAN" \
  -keystore kafka.keystore.p12 -storetype PKCS12 \
  -storepass "$STORE_PASS" -keypass "$KEY_PASS"

echo "==> [3/6] Gerando CSR do broker e assinando com a CA..."
# Gerar CSR
keytool -certreq -alias "$BROKER_ALIAS" -keystore kafka.keystore.p12 \
  -storepass "$STORE_PASS" -file broker.csr

# Assinar o CSR com a CA
keytool -gencert -alias "$CA_ALIAS" -keystore ca-root.p12 \
  -storepass "$STORE_PASS" \
  -infile broker.csr -outfile broker-signed.crt \
  -ext "$SAN" -ext ku=digitalSignature,keyEncipherment \
  -validity "$VALIDITY" -rfc

# Importar a CA e o certificado assinado no keystore do broker
keytool -importcert -alias "$CA_ALIAS" -keystore kafka.keystore.p12 \
  -storepass "$STORE_PASS" -file ca-root.crt -noprompt

keytool -importcert -alias "$BROKER_ALIAS" -keystore kafka.keystore.p12 \
  -storepass "$STORE_PASS" -file broker-signed.crt -noprompt

echo "    Keystore do broker: kafka.keystore.p12 (certificado assinado pela CA)"

# ==============================================================================
# 3. Gerar keystore do cliente e assinar com a CA
# ==============================================================================
echo "==> [4/6] Gerando keystore do cliente..."
keytool -genkeypair -alias "$CLIENT_ALIAS" -keyalg RSA -keysize 2048 -validity "$VALIDITY" \
  -dname "$DNAME_CLIENT" \
  -keystore kafka.client-keystore.p12 -storetype PKCS12 \
  -storepass "$STORE_PASS" -keypass "$KEY_PASS"

# Gerar CSR do cliente
keytool -certreq -alias "$CLIENT_ALIAS" -keystore kafka.client-keystore.p12 \
  -storepass "$STORE_PASS" -file client.csr

# Assinar o CSR com a CA
keytool -gencert -alias "$CA_ALIAS" -keystore ca-root.p12 \
  -storepass "$STORE_PASS" \
  -infile client.csr -outfile client-signed.crt \
  -ext ku=digitalSignature,keyEncipherment \
  -validity "$VALIDITY" -rfc

# Importar a CA e o certificado assinado no keystore do cliente
keytool -importcert -alias "$CA_ALIAS" -keystore kafka.client-keystore.p12 \
  -storepass "$STORE_PASS" -file ca-root.crt -noprompt

keytool -importcert -alias "$CLIENT_ALIAS" -keystore kafka.client-keystore.p12 \
  -storepass "$STORE_PASS" -file client-signed.crt -noprompt

echo "    Keystore do cliente: kafka.client-keystore.p12 (certificado assinado pela CA)"

# ==============================================================================
# 4. Gerar truststore do cliente (contem apenas a CA)
# ==============================================================================
echo "==> [5/6] Gerando truststore do cliente..."
keytool -importcert -alias "$CA_ALIAS" -file ca-root.crt \
  -keystore kafka.truststore.p12 -storetype PKCS12 \
  -storepass "$STORE_PASS" -noprompt

echo "    Truststore do cliente: kafka.truststore.p12 (contem apenas o certificado da CA)"

# ==============================================================================
# 5. Arquivo de credenciais para o broker (Confluent)
# ==============================================================================
echo "==> [6/6] Gerando arquivo de credenciais..."
echo "$STORE_PASS" > ssl_credentials

# ==============================================================================
# Limpeza de arquivos intermediários
# ==============================================================================
rm -f broker.csr broker-signed.crt client.csr client-signed.crt

echo ""
echo "Certificados gerados com sucesso em: $CERTS_DIR"
echo ""
echo "  Arquivos do broker (montar no container Kafka):"
echo "    - kafka.keystore.p12        (identidade do broker, assinada pela CA)"
echo "    - kafka.truststore.p12      (CA confiavel — necessario se mTLS)"
echo "    - ssl_credentials           (senha dos keystores)"
echo ""
echo "  Arquivos do cliente (copiar para o projeto da app):"
echo "    - kafka.client-keystore.p12 (identidade do cliente, assinada pela CA)"
echo "    - kafka.truststore.p12      (CA confiavel — para validar o broker)"
echo ""
echo "  Configuracao no cliente (application-ssl.properties):"
echo "    ssl-trust-store-location=classpath:ssl/kafka.truststore.p12"
echo "    ssl-key-store-location=classpath:ssl/kafka.client-keystore.p12"
