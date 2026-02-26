package com.souzamonteiro.nfe.service;

import com.souzamonteiro.nfe.dao.*;
import com.souzamonteiro.nfe.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Service para emissão de NF-e
 * Centraliza toda a lógica de construção do JSON e comunicação com o servidor
 * NF-e
 */
public class NFeService implements Serializable {

  private static final long serialVersionUID = 1L;
  private transient ConfiguracaoDAO configuracaoDAO = new ConfiguracaoDAO();
  private transient EmpresaDAO empresaDAO = new EmpresaDAO();

  /**
   * Emite uma NF-e para uma venda
   * 
   * @param venda Venda para emitir NF-e
   * @return Resultado da emissão
   */
  public NFeEmissaoResult emitirNFe(Venda venda) {
    try {
      Configuracao config = configuracaoDAO.getConfiguracao();
      Empresa empresa = empresaDAO.getEmpresa();

      if (config == null || empresa == null) {
        return new NFeEmissaoResult(false, "Configure empresa e configurações antes de emitir NF-e.");
      }

      // Construir JSON para envio
      JSONObject nfeJson = construirJsonNFe(venda, empresa, config);

      // Enviar para servidor de NF-e
      String url = "http://localhost:" + config.getPortaServidor() + "/NFeAutorizacao";
      String resposta = enviarParaServidor(url, nfeJson.toString());

      // Processar resposta
      JSONObject respostaJson = new JSONObject(resposta);
      String cStat = respostaJson.getString("cStat");

      if ("100".equals(cStat)) {
        String chave = respostaJson.getString("chave");
        String protocolo = respostaJson.getString("nProt");
        venda.setChaveNfe(chave);
        venda.setProtocoloNfe(protocolo);
        return new NFeEmissaoResult(true, "NF-e emitida com sucesso!", chave, protocolo);
      } else {
        return new NFeEmissaoResult(false, respostaJson.getString("xMotivo"));
      }

    } catch (Exception e) {
      e.printStackTrace();
      return new NFeEmissaoResult(false, "Erro ao emitir NF-e: " + e.getMessage());
    }
  }

  /**
   * Constrói o JSON da NF-e conforme padrão SEFAZ
   */
  private JSONObject construirJsonNFe(Venda venda, Empresa empresa, Configuracao config) {
    JSONObject nfeJson = new JSONObject();
    JSONObject infNFe = new JSONObject();

    // 1. IDENTIFICAÇÃO
    addIdentificacao(infNFe, venda, empresa, config);

    // 2. EMITENTE
    addEmitente(infNFe, empresa, config);

    // 3. DESTINATÁRIO
    addDestinatario(infNFe, venda, config);

    // 4. AUTORIZACAO XML
    addAutorizacaoXML(infNFe);

    // 5. DETALHES (PRODUTOS)
    addDetalhes(infNFe, venda, config);

    // 6. TOTAL
    addTotal(infNFe, venda);

    // 7. TRANSPORTE
    addTransporte(infNFe);

    // 8. PAGAMENTO
    addPagamento(infNFe, venda);

    // 9. INFORMAÇÕES ADICIONAIS
    addInformacoesAdicionais(infNFe, venda);

    nfeJson.put("infNFe", infNFe);
    return nfeJson;
  }

  private void addIdentificacao(JSONObject infNFe, Venda venda, Empresa empresa, Configuracao config) {
    JSONObject ide = new JSONObject();
    ide.put("cUF", getCodigoUF(config.getEmitenteUf()));
    ide.put("cNF", String.format("%08d", venda.getNumeroNfe() + 1));
    ide.put("natOp", config.getNaturezaOperacao());
    ide.put("mod", "55");
    ide.put("serie", config.getSerieNfe().toString());
    ide.put("nNF", config.getNumeroNfe().toString());

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    ide.put("dhEmi", sdf.format(venda.getDataVenda()));
    ide.put("tpNF", "1");
    ide.put("idDest", "1");
    ide.put("cMunFG", empresa.getCmun());
    ide.put("tpImp", "1");
    ide.put("tpEmis", "1");
    ide.put("cDV", "6");
    ide.put("tpAmb", config.getWebserviceAmbiente());
    ide.put("finNFe", config.getFinalidadeEmissao());
    ide.put("indFinal", config.getConsumidorFinal());
    ide.put("indPres", config.getPresencaComprador());
    ide.put("procEmi", "0");
    ide.put("verProc", "1.0");

    infNFe.put("ide", ide);
  }

  private void addEmitente(JSONObject infNFe, Empresa empresa, Configuracao config) {
    JSONObject emit = new JSONObject();
    emit.put("CNPJ", empresa.getCnpj());
    emit.put("xNome", empresa.getXnome());
    emit.put("IE", empresa.getIe());
    emit.put("CRT", empresa.getCrt());

    JSONObject enderEmit = new JSONObject();
    enderEmit.put("xLgr", empresa.getXlgr());
    enderEmit.put("nro", empresa.getNro());
    if (empresa.getXcpl() != null) {
      enderEmit.put("xCpl", empresa.getXcpl());
    }
    enderEmit.put("xBairro", empresa.getXbairro());
    enderEmit.put("cMun", empresa.getCmun());
    enderEmit.put("xMun", empresa.getXmun());
    enderEmit.put("UF", empresa.getUf());
    enderEmit.put("CEP", empresa.getCep());
    enderEmit.put("cPais", "1058");
    enderEmit.put("xPais", "Brasil");
    if (empresa.getFone() != null) {
      enderEmit.put("fone", empresa.getFone());
    }

    emit.put("enderEmit", enderEmit);
    infNFe.put("emit", emit);
  }

  private void addDestinatario(JSONObject infNFe, Venda venda, Configuracao config) {
    JSONObject dest = new JSONObject();
    Cliente cliente = venda.getClienteId();

    if (cliente.getCpf() != null && !cliente.getCpf().trim().isEmpty()) {
      dest.put("CPF", cliente.getCpf().replaceAll("\\D", ""));
      String xNome = cliente.getXnome();
      if ("2".equals(config.getWebserviceAmbiente())) {
        xNome = "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
      }
      dest.put("xNome", xNome);
    } else if (cliente.getCnpj() != null && !cliente.getCnpj().trim().isEmpty()) {
      dest.put("CNPJ", cliente.getCnpj().replaceAll("\\D", ""));
      String xNome = cliente.getXnome();
      if ("2".equals(config.getWebserviceAmbiente())) {
        xNome = "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
      }
      dest.put("xNome", xNome);
    } else {
      dest.put("CPF", "00000000000");
      dest.put("xNome", "CONSUMIDOR FINAL");
    }

    JSONObject enderDest = new JSONObject();
    enderDest.put("xLgr", cliente.getXlgr());
    enderDest.put("nro", cliente.getNro());
    if (cliente.getXcpl() != null) {
      enderDest.put("xCpl", cliente.getXcpl());
    }
    enderDest.put("xBairro", cliente.getXbairro());
    enderDest.put("cMun", cliente.getCmun());
    enderDest.put("xMun", cliente.getXmun());
    enderDest.put("UF", cliente.getUf());
    enderDest.put("CEP", cliente.getCep().replaceAll("\\D", ""));
    enderDest.put("cPais", "1058");
    enderDest.put("xPais", "Brasil");
    if (cliente.getFone() != null) {
      enderDest.put("fone", cliente.getFone());
    }

    dest.put("enderDest", enderDest);
    infNFe.put("dest", dest);
  }

  private void addAutorizacaoXML(JSONObject infNFe) {
    JSONObject autXML = new JSONObject();
    autXML.put("CNPJ", "13937073000156");
    infNFe.put("autXML", autXML);
  }

  private void addDetalhes(JSONObject infNFe, Venda venda, Configuracao config) {
    JSONArray detArray = new JSONArray();

    for (ItemVenda item : venda.getItemVendaCollection()) {
      Produto produto = item.getProdutoId();
      JSONObject det = new JSONObject();
      JSONObject prod = new JSONObject();

      // Dados básicos do produto
      prod.put("cProd", produto.getCprod());

      String cean = produto.getCean();
      if (cean == null || cean.trim().isEmpty() || "SEM GTIN".equalsIgnoreCase(cean)) {
        prod.put("cEAN", "7898480650104");
      } else {
        prod.put("cEAN", cean);
      }

      String xProd = produto.getXprod();
      if ("2".equals(config.getWebserviceAmbiente())) {
        xProd = "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
      }
      prod.put("xProd", xProd);

      prod.put("NCM", produto.getNcm());
      if (produto.getCest() != null && !produto.getCest().trim().isEmpty()) {
        prod.put("CEST", produto.getCest());
      }
      prod.put("indEscala", "S");
      prod.put("CFOP", produto.getCfop());
      prod.put("uCom", produto.getUcom());
      prod.put("qCom", String.format(Locale.US, "%.4f", item.getQuantidade()));
      prod.put("vUnCom", String.format(Locale.US, "%.4f", item.getValorUnitario()));
      prod.put("vProd", String.format(Locale.US, "%.2f", item.getValorTotal()));
      prod.put("cEANTrib", prod.getString("cEAN"));
      prod.put("uTrib", produto.getUcom());
      prod.put("qTrib", String.format(Locale.US, "%.4f", item.getQuantidade()));
      prod.put("vUnTrib", String.format(Locale.US, "%.4f", item.getValorUnitario()));
      prod.put("indTot", "1");

      det.put("prod", prod);

      // IMPOSTOS
      JSONObject imposto = new JSONObject();
      addImpostos(imposto, item, produto);
      det.put("imposto", imposto);

      detArray.put(det);
    }

    infNFe.put("det", detArray);
  }

  private void addImpostos(JSONObject imposto, ItemVenda item, Produto produto) {
    // ICMS
    JSONObject icms = new JSONObject();
    JSONObject icms00 = new JSONObject();
    icms00.put("orig", produto.getOrig() != null ? produto.getOrig() : "0");
    icms00.put("CST", produto.getCstIcms() != null ? produto.getCstIcms() : "00");
    icms00.put("modBC", "0");
    icms00.put("vBC", String.format(Locale.US, "%.2f", item.getValorTotal()));

    String pICMS = produto.getPicms() != null ? String.format(Locale.US, "%.2f", produto.getPicms()) : "7.00";
    icms00.put("pICMS", pICMS);

    BigDecimal valorICMS = item.getValorTotal()
        .multiply(new BigDecimal(pICMS))
        .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
    icms00.put("vICMS", String.format(Locale.US, "%.2f", valorICMS));

    icms.put("ICMS00", icms00);
    imposto.put("ICMS", icms);

    // PIS
    JSONObject pis = new JSONObject();
    JSONObject pisAliq = new JSONObject();
    pisAliq.put("CST", produto.getCstPis() != null ? produto.getCstPis() : "01");
    pisAliq.put("vBC", String.format(Locale.US, "%.2f", item.getValorTotal()));

    String pPIS = produto.getPpis() != null ? String.format(Locale.US, "%.2f", produto.getPpis()) : "1.65";
    pisAliq.put("pPIS", pPIS);

    BigDecimal valorPIS = item.getValorTotal()
        .multiply(new BigDecimal(pPIS))
        .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
    pisAliq.put("vPIS", String.format(Locale.US, "%.2f", valorPIS));

    pis.put("PISAliq", pisAliq);
    imposto.put("PIS", pis);

    // COFINS
    JSONObject cofins = new JSONObject();
    JSONObject cofinsAliq = new JSONObject();
    cofinsAliq.put("CST", produto.getCstCofins() != null ? produto.getCstCofins() : "01");
    cofinsAliq.put("vBC", String.format(Locale.US, "%.2f", item.getValorTotal()));

    String pCOFINS = produto.getPcofins() != null ? String.format(Locale.US, "%.2f", produto.getPcofins()) : "7.60";
    cofinsAliq.put("pCOFINS", pCOFINS);

    BigDecimal valorCOFINS = item.getValorTotal()
        .multiply(new BigDecimal(pCOFINS))
        .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
    cofinsAliq.put("vCOFINS", String.format(Locale.US, "%.2f", valorCOFINS));

    cofins.put("COFINSAliq", cofinsAliq);
    imposto.put("COFINS", cofins);
  }

  private void addTotal(JSONObject infNFe, Venda venda) {
    JSONObject total = new JSONObject();
    JSONObject icmsTot = new JSONObject();

    BigDecimal totalVenda = venda.getValorTotal();
    BigDecimal totalICMS = BigDecimal.ZERO;
    BigDecimal totalPIS = BigDecimal.ZERO;
    BigDecimal totalCOFINS = BigDecimal.ZERO;

    for (ItemVenda item : venda.getItemVendaCollection()) {
      Produto produto = item.getProdutoId();
      BigDecimal valorItem = item.getValorTotal();

      BigDecimal pICMS = produto.getPicms() != null ? produto.getPicms() : new BigDecimal("7.00");
      totalICMS = totalICMS.add(valorItem.multiply(pICMS)
          .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));

      BigDecimal pPIS = produto.getPpis() != null ? produto.getPpis() : new BigDecimal("1.65");
      totalPIS = totalPIS.add(valorItem.multiply(pPIS)
          .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));

      BigDecimal pCOFINS = produto.getPcofins() != null ? produto.getPcofins() : new BigDecimal("7.60");
      totalCOFINS = totalCOFINS.add(valorItem.multiply(pCOFINS)
          .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
    }

    icmsTot.put("vBC", String.format(Locale.US, "%.2f", totalVenda));
    icmsTot.put("vICMS", String.format(Locale.US, "%.2f", totalICMS));
    icmsTot.put("vICMSDeson", "0.00");
    icmsTot.put("vFCP", "0.00");
    icmsTot.put("vBCST", "0.00");
    icmsTot.put("vST", "0.00");
    icmsTot.put("vFCPST", "0.00");
    icmsTot.put("vFCPSTRet", "0.00");
    icmsTot.put("vProd", String.format(Locale.US, "%.2f", totalVenda));
    icmsTot.put("vFrete", "0.00");
    icmsTot.put("vSeg", "0.00");
    icmsTot.put("vDesc", "0.00");
    icmsTot.put("vII", "0.00");
    icmsTot.put("vIPI", "0.00");
    icmsTot.put("vIPIDevol", "0.00");
    icmsTot.put("vPIS", String.format(Locale.US, "%.2f", totalPIS));
    icmsTot.put("vCOFINS", String.format(Locale.US, "%.2f", totalCOFINS));
    icmsTot.put("vOutro", "0.00");
    icmsTot.put("vNF", String.format(Locale.US, "%.2f", totalVenda));

    total.put("ICMSTot", icmsTot);
    infNFe.put("total", total);
  }

  private void addTransporte(JSONObject infNFe) {
    JSONObject transp = new JSONObject();
    transp.put("modFrete", "9");
    infNFe.put("transp", transp);
  }

  private void addPagamento(JSONObject infNFe, Venda venda) {
    JSONObject pag = new JSONObject();
    JSONArray detPagArray = new JSONArray();

    JSONObject detPag = new JSONObject();
    detPag.put("tPag", "01");
    detPag.put("vPag", String.format(Locale.US, "%.2f", venda.getValorTotal()));

    detPagArray.put(detPag);
    pag.put("detPag", detPagArray);
    infNFe.put("pag", pag);
  }

  private void addInformacoesAdicionais(JSONObject infNFe, Venda venda) {
    JSONObject infAdic = new JSONObject();
    BigDecimal totalTributos = BigDecimal.ZERO;

    for (ItemVenda item : venda.getItemVendaCollection()) {
      Produto produto = item.getProdutoId();
      BigDecimal valorItem = item.getValorTotal();

      BigDecimal pICMS = produto.getPicms() != null ? produto.getPicms() : new BigDecimal("7.00");
      BigDecimal pPIS = produto.getPpis() != null ? produto.getPpis() : new BigDecimal("1.65");
      BigDecimal pCOFINS = produto.getPcofins() != null ? produto.getPcofins() : new BigDecimal("7.60");

      BigDecimal tributos = valorItem.multiply(pICMS.add(pPIS).add(pCOFINS))
          .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
      totalTributos = totalTributos.add(tributos);
    }

    String infCpl = String.format(Locale.US, "Tributos Totais Incidentes (Lei Federal 12.741/2012): R$%.2f",
        totalTributos);
    infAdic.put("infCpl", infCpl);
    infNFe.put("infAdic", infAdic);
  }

  /**
   * Envia o JSON para o servidor de NF-e
   */
  private String enviarParaServidor(String urlString, String json) throws Exception {
    System.out.println("=== ENVIANDO PARA SERVIDOR NF-e ===");
    System.out.println("URL: " + urlString);
    System.out.println("JSON: " + json);
    System.out.println("===================================");

    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Accept", "application/json");
    conn.setDoOutput(true);
    conn.setConnectTimeout(30000);
    conn.setReadTimeout(30000);

    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = json.getBytes("utf-8");
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();
    System.out.println("Código de resposta HTTP: " + responseCode);

    if (responseCode >= 400) {
      try (BufferedReader br = new BufferedReader(
          new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
        StringBuilder errorResponse = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
          errorResponse.append(responseLine.trim());
        }
        System.out.println("ERRO HTTP " + responseCode + ": " + errorResponse.toString());
        throw new RuntimeException("HTTP Error: " + responseCode + " - " + errorResponse.toString());
      }
    }

    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
      StringBuilder response = new StringBuilder();
      String responseLine;
      while ((responseLine = br.readLine()) != null) {
        response.append(responseLine.trim());
      }

      System.out.println("Resposta do servidor:");
      System.out.println(response.toString());
      System.out.println("===================================");

      return response.toString();
    }
  }

  /**
   * Retorna o código do UF pelo acrônimo
   */
  private String getCodigoUF(String uf) {
    switch (uf.toUpperCase()) {
      case "AC":
        return "12";
      case "AL":
        return "27";
      case "AM":
        return "13";
      case "AP":
        return "16";
      case "BA":
        return "29";
      case "CE":
        return "23";
      case "DF":
        return "53";
      case "ES":
        return "32";
      case "GO":
        return "52";
      case "MA":
        return "21";
      case "MG":
        return "31";
      case "MS":
        return "50";
      case "MT":
        return "51";
      case "PA":
        return "15";
      case "PB":
        return "25";
      case "PE":
        return "26";
      case "PI":
        return "22";
      case "PR":
        return "41";
      case "RJ":
        return "33";
      case "RN":
        return "24";
      case "RO":
        return "11";
      case "RR":
        return "14";
      case "RS":
        return "43";
      case "SC":
        return "42";
      case "SE":
        return "28";
      case "SP":
        return "35";
      case "TO":
        return "17";
      default:
        return "29";
    }
  }

  /**
   * Classe interna para resultado da emissão
   */
  public static class NFeEmissaoResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean sucesso;
    private String mensagem;
    private String chaveNfe;
    private String protocoloNfe;

    public NFeEmissaoResult(boolean sucesso, String mensagem) {
      this.sucesso = sucesso;
      this.mensagem = mensagem;
    }

    public NFeEmissaoResult(boolean sucesso, String mensagem, String chaveNfe, String protocoloNfe) {
      this.sucesso = sucesso;
      this.mensagem = mensagem;
      this.chaveNfe = chaveNfe;
      this.protocoloNfe = protocoloNfe;
    }

    public boolean isSucesso() {
      return sucesso;
    }

    public String getMensagem() {
      return mensagem;
    }

    public String getChaveNfe() {
      return chaveNfe;
    }

    public String getProtocoloNfe() {
      return protocoloNfe;
    }
  }
}
