package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.*;
import com.souzamonteiro.nfe.model.*;
import com.souzamonteiro.nfe.service.PixService;
import com.souzamonteiro.nfe.util.GeradorQRCodePix;

import org.json.JSONArray;
import org.json.JSONObject;
import org.primefaces.PrimeFaces;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ManagedBean
@ViewScoped
public class VendaController implements Serializable {

    private transient VendaDAO vendaDAO = new VendaDAO();
    private transient ClienteDAO clienteDAO = new ClienteDAO();
    private transient ProdutoDAO produtoDAO = new ProdutoDAO();
    private transient ConfiguracaoDAO configuracaoDAO = new ConfiguracaoDAO();
    private transient EmpresaDAO empresaDAO = new EmpresaDAO();

    private List<Venda> vendas;
    private Venda venda;
    private List<Cliente> clientes;
    private List<Produto> produtos;
    private Cliente clienteSelecionado;
    private Produto produtoSelecionado;
    private ItemVenda itemVenda;
    private boolean editando = false; // Inicialize como false

    @PostConstruct
    public void init() {
        carregarVendas();
        carregarClientes();
        carregarProdutos();
    }

    public void carregarVendas() {
        vendas = vendaDAO.findEmitidas();
        editando = false;
    }

    public void novaVenda() {
        venda = new Venda();
        venda.setDataVenda(new Date());
        venda.setStatus("PENDENTE");
        venda.setValorTotal(BigDecimal.ZERO);
        venda.setItemVendaCollection(new ArrayList<>()); // Inicializar a coleção
        itemVenda = new ItemVenda();
        itemVenda.setQuantidade(BigDecimal.ONE);
        clienteSelecionado = null;
        produtoSelecionado = null;
        editando = true;
    }

    public void carregarClientes() {
        clientes = clienteDAO.findAtivos();
    }

    public void carregarProdutos() {
        produtos = produtoDAO.findAtivos();
    }

    public void adicionarItem() {
        if (produtoSelecionado != null && itemVenda.getQuantidade() != null &&
                itemVenda.getQuantidade().compareTo(BigDecimal.ZERO) > 0) {

            // Verificar se produto já está na venda
            for (ItemVenda item : venda.getItemVendaCollection()) {
                if (item.getProdutoId() != null &&
                        item.getProdutoId().getId().equals(produtoSelecionado.getId())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN,
                                    "Aviso", "Produto já adicionado à venda."));
                    return;
                }
            }

            ItemVenda novoItem = new ItemVenda();
            novoItem.setProdutoId(produtoSelecionado);
            novoItem.setQuantidade(itemVenda.getQuantidade());
            novoItem.setValorUnitario(produtoSelecionado.getVuncom());
            novoItem.setValorTotal(itemVenda.getQuantidade().multiply(produtoSelecionado.getVuncom()));
            novoItem.setDataCriacao(new Date()); // Adicionar data de criação

            // *** CORREÇÃO CRÍTICA: Associar a venda ao item ***
            novoItem.setVendaId(venda);

            venda.getItemVendaCollection().add(novoItem);

            // Calcular valor total da venda
            calcularTotalVenda();

            // Limpar seleção
            produtoSelecionado = null;
            itemVenda = new ItemVenda();
            itemVenda.setQuantidade(BigDecimal.ONE);

            // Atualizar componente via AJAX
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds()
                    .add("form:vendaItens");
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds()
                    .add("form:totalVenda");
        }
    }

    public void removerItem(ItemVenda item) {
        if (venda.getItemVendaCollection() != null) {
            venda.getItemVendaCollection().remove(item); // Corrigido: usando getItemVendaCollection()
            calcularTotalVenda();
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:vendaItens");
        }
    }

    private void calcularTotalVenda() {
        BigDecimal total = BigDecimal.ZERO;
        if (venda.getItemVendaCollection() != null) {
            for (ItemVenda item : venda.getItemVendaCollection()) {
                if (item.getValorTotal() != null) {
                    total = total.add(item.getValorTotal());
                }
            }
        }
        venda.setValorTotal(total);
    }

    public void finalizarVenda() {
        if (venda.getClienteId() == null) {
            System.out.println("ERRO: Cliente é null");
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro", "Selecione um cliente."));
            return;
        }

        if (venda.getItemVendaCollection() == null || venda.getItemVendaCollection().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro", "Adicione pelo menos um item à venda."));
            return;
        }

        try {
            // *** CORREÇÃO CRÍTICA: Garantir que todos os itens tenham referência à venda
            // ***
            for (ItemVenda item : venda.getItemVendaCollection()) {
                if (item.getVendaId() == null) {
                    item.setVendaId(venda);
                }
                if (item.getDataCriacao() == null) {
                    item.setDataCriacao(new Date());
                }
            }

            // Associar cliente à venda
            venda.setClienteId(clienteSelecionado);

            // Preencher datas obrigatórias
            venda.setDataCriacao(new Date());

            Empresa empresa = empresaDAO.getEmpresa();
            Configuracao config = configuracaoDAO.getConfiguracao();

            // Gerar número da NF-e
            Integer numeroNFe = config.getNumeroNfe();
            venda.setNumeroNfe(numeroNFe);

            // Salvar venda
            vendaDAO.save(venda);

            // Emitir NF-e
            boolean sucesso = emitirNFe(venda);

            if (sucesso) {
                venda.setStatus("EMITIDA");
                venda.setDataAtualizacao(new Date());

                String chavePix = PixService.gerarChavePixParaVenda(venda, empresa);
                if (PixService.isChavePixValida(chavePix)) {
                    venda.setChavePix(chavePix);
                }

                vendaDAO.save(venda);

                config.setNumeroNfe(numeroNFe + 1);
                configuracaoDAO.save(config);

                abrirPDFAposEmissao();

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Sucesso", "Venda finalizada e NF-e emitida com sucesso."));

                carregarVendas();
            } else {
                venda.setStatus("ERRO");
                venda.setDataAtualizacao(new Date());
                vendaDAO.save(venda);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erro", "Erro ao emitir NF-e. Venda salva como pendente."));
            }

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro", "Erro ao finalizar venda: " + e.getMessage()));
            e.printStackTrace(); // Para debug
        }
    }

    public void cancelarVenda() {
        carregarVendas();
    }

    private boolean emitirNFe(Venda venda) {
        try {
            Configuracao config = configuracaoDAO.getConfiguracao();
            Empresa empresa = empresaDAO.getEmpresa();

            if (config == null || empresa == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erro", "Configure empresa e configurações antes de emitir NF-e."));
                return false;
            }

            // Construir JSON para envio ao servidor de NF-e
            JSONObject nfeJson = construirJsonNFe(venda, empresa, config);

            // Enviar para servidor de NF-e
            String url = "http://localhost:" + config.getPortaServidor() + "/NFeAutorizacao";
            String resposta = enviarParaServidor(url, nfeJson.toString());

            // Processar resposta
            JSONObject respostaJson = new JSONObject(resposta);
            String cStat = respostaJson.getString("cStat");

            if ("100".equals(cStat)) {
                venda.setChaveNfe(respostaJson.getString("chave"));
                venda.setProtocoloNfe(respostaJson.getString("nProt"));
                return true;
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erro NF-e", respostaJson.getString("xMotivo")));
                return false;
            }

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro", "Erro ao emitir NF-e: " + e.getMessage()));
            return false;
        }
    }

    private JSONObject construirJsonNFe(Venda venda, Empresa empresa, Configuracao config) {
        JSONObject nfeJson = new JSONObject();
        JSONObject infNFe = new JSONObject();

        // 1. IDE (Identificação da NF-e)
        JSONObject ide = new JSONObject();
        ide.put("cUF", getCodigoUF(config.getEmitenteUf()));

        // cNF deve ser string com 8 dígitos
        String cnfStr = String.format("%08d", venda.getNumeroNfe() + 1);
        ide.put("cNF", cnfStr);

        ide.put("natOp", config.getNaturezaOperacao());
        ide.put("mod", "55");

        // serie deve ser string
        ide.put("serie", config.getSerieNfe().toString());

        // nNF deve ser string - CORREÇÃO AQUI
        String nNFStr = config.getNumeroNfe().toString();
        ide.put("nNF", nNFStr);

        // Formatar data no formato correto
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ide.put("dhEmi", sdf.format(venda.getDataVenda()));

        ide.put("tpNF", "1");
        ide.put("idDest", "1");
        ide.put("cMunFG", empresa.getCmun());
        ide.put("tpImp", "1");
        ide.put("tpEmis", "1");
        ide.put("cDV", "6"); // Para homologação
        ide.put("tpAmb", config.getWebserviceAmbiente());
        ide.put("finNFe", config.getFinalidadeEmissao());
        ide.put("indFinal", config.getConsumidorFinal());
        ide.put("indPres", config.getPresencaComprador());
        ide.put("procEmi", "0");
        ide.put("verProc", "1.0");

        infNFe.put("ide", ide);

        // 2. EMITENTE
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
        enderEmit.put("fone", empresa.getFone() != null ? empresa.getFone() : "");
        if (empresa.getFone() != null) {
            enderEmit.put("fone", empresa.getFone());
        }

        emit.put("enderEmit", enderEmit);
        infNFe.put("emit", emit);

        // 3. DESTINATÁRIO
        JSONObject dest = new JSONObject();
        Cliente cliente = venda.getClienteId();

        if (cliente.getCpf() != null && !cliente.getCpf().trim().isEmpty()) {
            dest.put("CPF", cliente.getCpf().replaceAll("\\D", ""));
            dest.put("xNome", cliente.getXnome());
            String xNome = cliente.getXnome();
            if ("2".equals(config.getWebserviceAmbiente())) { // Se for homologação
                xNome = "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
            }
            dest.put("xNome", xNome);
        } else if (cliente.getCnpj() != null && !cliente.getCnpj().trim().isEmpty()) {
            dest.put("CNPJ", cliente.getCnpj().replaceAll("\\D", ""));
            String xNome = cliente.getXnome();
            if ("2".equals(config.getWebserviceAmbiente())) { // Se for homologação
                xNome = "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
            }
            dest.put("xNome", xNome);
        } else {
            // Consumidor não identificado
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

        // 4. AUTORIZAÇÃO XML (para homologação)
        JSONObject autXML = new JSONObject();
        autXML.put("CNPJ", "13937073000156"); // CNPJ da SEFAZ de homologação
        infNFe.put("autXML", autXML);

        // 5. DETALHES (produtos)
        JSONArray detArray = new JSONArray();

        for (ItemVenda item : venda.getItemVendaCollection()) {
            Produto produto = item.getProdutoId();

            JSONObject det = new JSONObject();

            JSONObject prod = new JSONObject();
            prod.put("cProd", produto.getCprod());

            // Código EAN
            String cean = produto.getCean();
            if (cean == null || cean.trim().isEmpty() || "SEM GTIN".equalsIgnoreCase(cean)) {
                prod.put("cEAN", "7898480650104"); // Código genérico para homologação
            } else {
                prod.put("cEAN", cean);
            }

            // **APENAS para homologação: usar mensagem obrigatória**
            // Mantenha a descrição real do produto, mas adicione a mensagem de homologação
            String xProd = produto.getXprod();
            if ("2".equals(config.getWebserviceAmbiente())) { // Se for homologação
                xProd = "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
            }
            prod.put("xProd", xProd);

            prod.put("NCM", produto.getNcm());

            // CEST (se disponível)
            if (produto.getCest() != null && !produto.getCest().trim().isEmpty()) {
                prod.put("CEST", produto.getCest());
            }

            prod.put("indEscala", "S"); // Produzido em escala relevante
            prod.put("CFOP", produto.getCfop());
            prod.put("uCom", produto.getUcom());

            // Formatar números com casas decimais corretas
            prod.put("qCom", String.format(Locale.US, "%.4f", item.getQuantidade()));
            prod.put("vUnCom", String.format(Locale.US, "%.4f", item.getValorUnitario()));
            prod.put("vProd", String.format(Locale.US, "%.2f", item.getValorTotal()));

            // Código EAN tributário
            prod.put("cEANTrib", prod.getString("cEAN"));
            prod.put("uTrib", produto.getUcom());
            prod.put("qTrib", String.format(Locale.US, "%.4f", item.getQuantidade()));
            prod.put("vUnTrib", String.format(Locale.US, "%.4f", item.getValorUnitario()));
            prod.put("indTot", "1");

            det.put("prod", prod);

            // IMPOSTOS
            JSONObject imposto = new JSONObject();

            // ICMS
            JSONObject icms = new JSONObject();
            JSONObject icms00 = new JSONObject();
            icms00.put("orig", produto.getOrig() != null ? produto.getOrig() : "0");
            icms00.put("CST", produto.getCstIcms() != null ? produto.getCstIcms() : "00");
            icms00.put("modBC", "0"); // Margem de valor agregado (%)
            icms00.put("vBC", String.format(Locale.US, "%.2f", item.getValorTotal()));

            // Usar percentual do produto ou padrão para homologação
            String pICMS;
            if (produto.getPicms() != null) {
                pICMS = String.format(Locale.US, "%.2f", produto.getPicms());
            } else {
                pICMS = "7.00"; // Valor padrão para homologação
            }
            icms00.put("pICMS", pICMS);

            // Calcular valor do ICMS
            BigDecimal valorICMS = item.getValorTotal()
                    .multiply(new BigDecimal(pICMS))
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            icms00.put("vICMS", String.format(Locale.US, "%.2f", valorICMS));

            icms.put("ICMS00", icms00);
            imposto.put("ICMS", icms);

            // PIS
            JSONObject pis = new JSONObject();
            JSONObject pisAliq = new JSONObject();
            pisAliq.put("CST", produto.getCstPis() != null ? produto.getCstPis() : "01");
            pisAliq.put("vBC", String.format(Locale.US, "%.2f", item.getValorTotal()));

            String pPIS;
            if (produto.getPpis() != null) {
                pPIS = String.format(Locale.US, "%.2f", produto.getPpis());
            } else {
                pPIS = "1.65"; // Valor padrão para homologação
            }
            pisAliq.put("pPIS", pPIS);

            BigDecimal valorPIS = item.getValorTotal()
                    .multiply(new BigDecimal(pPIS))
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            pisAliq.put("vPIS", String.format(Locale.US, "%.2f", valorPIS));

            pis.put("PISAliq", pisAliq);
            imposto.put("PIS", pis);

            // COFINS
            JSONObject cofins = new JSONObject();
            JSONObject cofinsAliq = new JSONObject();
            cofinsAliq.put("CST", produto.getCstCofins() != null ? produto.getCstCofins() : "01");
            cofinsAliq.put("vBC", String.format(Locale.US, "%.2f", item.getValorTotal()));

            String pCOFINS;
            if (produto.getPcofins() != null) {
                pCOFINS = String.format(Locale.US, "%.2f", produto.getPcofins());
            } else {
                pCOFINS = "7.60"; // Valor padrão para homologação
            }
            cofinsAliq.put("pCOFINS", pCOFINS);

            BigDecimal valorCOFINS = item.getValorTotal()
                    .multiply(new BigDecimal(pCOFINS))
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            cofinsAliq.put("vCOFINS", String.format(Locale.US, "%.2f", valorCOFINS));

            cofins.put("COFINSAliq", cofinsAliq);
            imposto.put("COFINS", cofins);

            det.put("imposto", imposto);
            detArray.put(det);
        }

        infNFe.put("det", detArray);

        // 6. TOTAL
        JSONObject total = new JSONObject();
        JSONObject icmsTot = new JSONObject();

        // Calcular totais
        BigDecimal totalVenda = venda.getValorTotal();
        BigDecimal totalICMS = BigDecimal.ZERO;
        BigDecimal totalPIS = BigDecimal.ZERO;
        BigDecimal totalCOFINS = BigDecimal.ZERO;

        for (ItemVenda item : venda.getItemVendaCollection()) {
            Produto produto = item.getProdutoId();
            BigDecimal valorItem = item.getValorTotal();

            // ICMS
            BigDecimal pICMS = produto.getPicms() != null ? produto.getPicms() : new BigDecimal("7.00");
            totalICMS = totalICMS
                    .add(valorItem.multiply(pICMS).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));

            // PIS
            BigDecimal pPIS = produto.getPpis() != null ? produto.getPpis() : new BigDecimal("1.65");
            totalPIS = totalPIS
                    .add(valorItem.multiply(pPIS).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));

            // COFINS
            BigDecimal pCOFINS = produto.getPcofins() != null ? produto.getPcofins() : new BigDecimal("7.60");
            totalCOFINS = totalCOFINS
                    .add(valorItem.multiply(pCOFINS).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));
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

        // 7. TRANSPORTE
        JSONObject transp = new JSONObject();
        transp.put("modFrete", "9"); // 9=Sem frete
        infNFe.put("transp", transp);

        // 8. PAGAMENTO
        JSONObject pag = new JSONObject();
        JSONArray detPagArray = new JSONArray();

        JSONObject detPag = new JSONObject();
        detPag.put("tPag", "01"); // 01=Dinheiro
        detPag.put("vPag", String.format(Locale.US, "%.2f", venda.getValorTotal()));

        detPagArray.put(detPag);
        pag.put("detPag", detPagArray);
        infNFe.put("pag", pag);

        // 9. INFORMAÇÕES ADICIONAIS
        JSONObject infAdic = new JSONObject();

        // Calcular total de tributos (Lei Federal 12.741/2012)
        BigDecimal totalTributos = totalICMS.add(totalPIS).add(totalCOFINS);
        String infCpl = String.format(Locale.US, "Tributos Totais Incidentes (Lei Federal 12.741/2012): R$%.2f",
                totalTributos);

        infAdic.put("infCpl", infCpl);
        infNFe.put("infAdic", infAdic);

        // Montar JSON final
        nfeJson.put("infNFe", infNFe);

        return nfeJson;
    }

    private String enviarParaServidor(String urlString, String json) throws Exception {
        System.out.println("=== ENVIANDO PARA SERVIDOR NF-e ===");
        System.out.println("URL: " + urlString);
        System.out.println("JSON Enviado:");
        System.out.println(json);
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
            // Ler erro
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

    // *** CORREÇÃO: Getters e Setters simplificados ***
    public List<Venda> getVendas() {
        return vendas;
    }

    public void setVendas(List<Venda> vendas) {
        this.vendas = vendas;
    }

    public Venda getVenda() {
        return venda;
    }

    public void setVenda(Venda venda) {
        this.venda = venda;
    }

    public List<Cliente> getClientes() {
        return clientes;
    }

    public void setClientes(List<Cliente> clientes) {
        this.clientes = clientes;
    }

    public List<Produto> getProdutos() {
        return produtos;
    }

    public void setProdutos(List<Produto> produtos) {
        this.produtos = produtos;
    }

    // *** CORREÇÃO: Usar apenas uma propriedade para o cliente ***
    public Cliente getClienteSelecionado() {
        // Se já temos cliente na venda, retorna ele
        if (venda != null && venda.getClienteId() != null) {
            return venda.getClienteId();
        }
        // Caso contrário, retorna o selecionado temporariamente
        return clienteSelecionado;
    }

    public void setClienteSelecionado(Cliente clienteSelecionado) {
        this.clienteSelecionado = clienteSelecionado;
        // Atualiza também na venda
        if (venda != null) {
            venda.setClienteId(clienteSelecionado);
        }
    }

    public Produto getProdutoSelecionado() {
        return produtoSelecionado;
    }

    public void setProdutoSelecionado(Produto produtoSelecionado) {
        this.produtoSelecionado = produtoSelecionado;
    }

    public ItemVenda getItemVenda() {
        return itemVenda;
    }

    public void setItemVenda(ItemVenda itemVenda) {
        this.itemVenda = itemVenda;
    }

    public boolean isEditando() {
        return editando;
    }

    public void setEditando(boolean editando) {
        this.editando = editando;
    }

    // *** MANTER os métodos auxiliares para compatibilidade com a página ***
    public Cliente getClienteSelecionadoParaVenda() {
        if (venda != null && venda.getClienteId() != null) {
            return venda.getClienteId();
        }
        return null;
    }

    public void setClienteSelecionadoParaVenda(Cliente cliente) {
        if (venda != null) {
            venda.setClienteId(cliente);
            this.clienteSelecionado = cliente;
        }
    }

    public List<ItemVenda> getItensVenda() {
        if (venda != null && venda.getItemVendaCollection() != null) {
            return new ArrayList<>(venda.getItemVendaCollection());
        }
        return new ArrayList<>();
    }

    public String getChavePixFormatada() {
        if (venda != null && venda.getChavePix() != null) {
            return venda.getChavePix();
        }
        return null;
    }

    public String getQrCodePixBase64() {
        try {
            if (venda != null && venda.getChavePix() != null && !venda.getChavePix().isEmpty()) {
                byte[] qrCodeBytes = GeradorQRCodePix.gerarQRCodeBytes(venda.getChavePix(), 250);
                String base64 = Base64.getEncoder().encodeToString(qrCodeBytes);
                return "data:image/png;base64," + base64;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback para Google Charts se der erro
            try {
                String chavePix = venda != null ? venda.getChavePix() : "";
                if (chavePix != null && !chavePix.isEmpty()) {
                    String encoded = java.net.URLEncoder.encode(chavePix, "UTF-8");
                    return "https://chart.googleapis.com/chart?cht=qr&chs=250x250&chl=" + encoded + "&chld=L|1";
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public void downloadPDF() {
        if (venda != null && venda.getChaveNfe() != null) {
            try {
                // Armazenar chave na sessão temporariamente
                Map<String, Object> sessionMap = FacesContext.getCurrentInstance()
                        .getExternalContext().getSessionMap();
                sessionMap.put("pdfDownloadChave", venda.getChaveNfe());

                // Fechar o dialog primeiro
                PrimeFaces.current().executeScript("PF('pdfDialog').hide();");

                // Redirecionar para o servlet de download (fora do AJAX)
                String contextPath = FacesContext.getCurrentInstance()
                        .getExternalContext().getRequestContextPath();
                String downloadUrl = contextPath + "/pdf/download?chave=" + venda.getChaveNfe();

                // Usar redirect não-AJAX
                FacesContext.getCurrentInstance().getExternalContext()
                        .redirect(downloadUrl);

            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erro", "Erro ao preparar download: " + e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    public void abrirPDF() {
        if (venda != null && venda.getChaveNfe() != null) {
            try {
                String contextPath = FacesContext.getCurrentInstance()
                        .getExternalContext().getRequestContextPath();
                String pdfUrl = contextPath + "/pdf/view?chave=" + venda.getChaveNfe();

                // Abrir em nova aba
                String script = "window.open('" + pdfUrl + "', '_blank');";
                PrimeFaces.current().executeScript(script);

                // Fechar dialog
                PrimeFaces.current().executeScript("PF('pdfDialog').hide();");

            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erro", "Erro ao abrir PDF: " + e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    // Método para preparar download (será chamado pelo h:commandLink)
    public String prepararDownload() {
        String chave = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get("chave");

        if (chave != null && !chave.isEmpty()) {
            // Redirecionar para o servlet de download
            return "/pdf/download?chave=" + chave + "&faces-redirect=true";
        }

        return null;
    }

    // Método para abrir automaticamente após emissão
    private void abrirPDFAposEmissao() {
        // Primeiro atualiza o componente para garantir que os dados estejam carregados
        PrimeFaces.current().ajax().update("form:pdfDialog");

        // Depois abre o diálogo
        PrimeFaces.current().executeScript(
                "setTimeout(function() { " +
                        "  if (PF('pdfDialog')) { " +
                        "    PF('pdfDialog').show(); " +
                        "  } " +
                        "}, 1000);");
    }

}