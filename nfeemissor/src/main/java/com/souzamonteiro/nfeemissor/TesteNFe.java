package com.souzamonteiro.nfeemissor;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Classe de teste para emissão de NF-e
 * Para fins didáticos
 */
public class TesteNFe {
    
    public static void main(String[] args) {
        try {
            // URL do serviço
            String url = "http://localhost:3435/NFeAutorizacao";
            
            // Carrega o JSON de teste
            String jsonTeste = Config.readFile("TesteNFe.json");
            JSONObject teste = new JSONObject(jsonTeste);
            
            // Cria a conexão HTTP
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Envia os dados
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = teste.getJSONObject("testeAutorizacao").toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // Lê a resposta
            try (InputStream is = connection.getInputStream();
                 Scanner scanner = new Scanner(is, "utf-8")) {
                String response = scanner.useDelimiter("\\A").next();
                System.out.println("Resposta do servidor:");
                System.out.println(response);
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            System.out.println("Erro no teste: " + e.getMessage());
            e.printStackTrace();
        }
    }
}