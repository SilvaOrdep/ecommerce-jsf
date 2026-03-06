#!/bin/bash

# Uso:
#   ./enviar_json.sh arquivo.json
#
# O script envia o JSON para:
#   http://localhost:3435/NFeAutorizacao
#
# Você pode alterar URL e porta conforme necessário.

URL="http://localhost:3435/NFeAutorizacao"

if [ -z "$1" ]; then
    echo "Uso: $0 arquivo.json"
    exit 1
fi

ARQUIVO="$1"

if [ ! -f "$ARQUIVO" ]; then
    echo "Erro: arquivo '$ARQUIVO' não encontrado."
    exit 1
fi

echo "Enviando $ARQUIVO para $URL..."
curl -X POST \
     -H "Content-Type: application/json" \
     --data @"$ARQUIVO" \
     "$URL"

echo -e "\n--- Fim da requisição ---"
