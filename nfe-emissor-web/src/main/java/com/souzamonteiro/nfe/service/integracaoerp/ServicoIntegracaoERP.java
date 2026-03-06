package com.souzamonteiro.nfe.service.integracaoerp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author pedro
 */
public class ServicoIntegracaoERP implements Serializable, IntegravelComERP {

    private static final long serialVersionUID = 1L;
    private static final String API_BASE_URL = "http://localhost:3000/api/ecommerce/produtos";
    private static final String API_KEY = "xxxxxxxxxxxx";

    @Override
    public JSONArray buscarProdutos() {
        try {
            String endpoint = API_BASE_URL + "/consulta";
            URL url = new URL(endpoint);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "ApiKey " + API_KEY);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            int codigoResposta = conn.getResponseCode();

            if (codigoResposta < 200 || codigoResposta >= 300) {
                throw new RuntimeException("Erro na requisição: HTTP " + codigoResposta);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                System.out.println("Resposta completa: " + response.toString());

                // Parse a resposta que vem como: {status, data[], msg, total}
                JSONObject respostaObj = new JSONObject(response.toString());

                // Validar status da resposta
                int statusResposta = respostaObj.getInt("status");
                if (statusResposta < 200 || statusResposta >= 300) {
                    throw new RuntimeException("Erro na API: Status " + statusResposta + " - "
                            + respostaObj.optString("msg", "Erro desconhecido"));
                }

                // Extrair o array de produtos de dentro de 'data'
                JSONArray produtosArray = respostaObj.getJSONArray("data");
                System.out.println("Total de produtos retornados: " + produtosArray.length());

                return produtosArray;
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(ServicoIntegracaoERP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServicoIntegracaoERP.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public JSONObject registrarPedito(JSONObject pedido) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from
                                                                       // nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
