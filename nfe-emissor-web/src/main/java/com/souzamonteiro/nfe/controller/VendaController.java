package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.*;
import com.souzamonteiro.nfe.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private boolean editando;
    
    public VendaController() {
        // Construtor vazio para permitir @PostConstruct
    }
    
    @PostConstruct
    public void init() {
        carregarVendas();
        novaVenda();
        carregarClientes();
        carregarProdutos();
    }
    
    public void carregarVendas() {
        vendas = vendaDAO.findEmitidas();
        editando = false;
    }
    
    public void novaVenda() {
        venda = new Venda();
        venda.setDataVenda(new Date()); // Corrigido: usando Date em vez de LocalDateTime
        venda.setStatus("PENDENTE");
        venda.setValorTotal(BigDecimal.ZERO);
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
            if (venda.getItemVendaCollection() != null) {
                for (ItemVenda item : venda.getItemVendaCollection()) {
                    if (item.getProdutoId() != null && 
                        item.getProdutoId().getId().equals(produtoSelecionado.getId())) {
                        FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, 
                            "Aviso", "Produto já adicionado à venda."));
                        return;
                    }
                }
            } else {
                venda.setItemVendaCollection(new ArrayList<>());
            }
            
            ItemVenda novoItem = new ItemVenda();
            novoItem.setProdutoId(produtoSelecionado); // Corrigido: setProdutoId em vez de setProduto
            novoItem.setQuantidade(itemVenda.getQuantidade());
            novoItem.setValorUnitario(produtoSelecionado.getVuncom());
            novoItem.setValorTotal(itemVenda.getQuantidade().multiply(produtoSelecionado.getVuncom()));
            
            venda.getItemVendaCollection().add(novoItem); // Corrigido: usando getItemVendaCollection()
            
            // Calcular valor total da venda
            calcularTotalVenda();
            
            // Limpar seleção
            produtoSelecionado = null;
            itemVenda = new ItemVenda();
            itemVenda.setQuantidade(BigDecimal.ONE);
            
            // Atualizar componente via AJAX
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:vendaItens");
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
        if (clienteSelecionado == null) {
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
            // Associar cliente à venda
            venda.setClienteId(clienteSelecionado); // Corrigido: setClienteId em vez de setCliente
            
            // Gerar número da NF-e
            Integer numeroNFe = vendaDAO.getProximoNumeroNFe();
            venda.setNumeroNfe(numeroNFe); // Corrigido: setNumeroNfe em vez de setNumeroNFe
            
            // Salvar venda
            vendaDAO.save(venda);
            
            // Emitir NF-e
            boolean sucesso = emitirNFe(venda);
            
            if (sucesso) {
                venda.setStatus("EMITIDA");
                vendaDAO.save(venda);
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Sucesso", "Venda finalizada e NF-e emitida com sucesso."));
                
                carregarVendas();
            } else {
                venda.setStatus("ERRO");
                vendaDAO.save(venda);
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao emitir NF-e. Venda salva como pendente."));
            }
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao finalizar venda: " + e.getMessage()));
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
                venda.setChaveNfe(respostaJson.getString("chave")); // Corrigido: setChaveNfe em vez de setChaveNFe
                venda.setProtocoloNfe(respostaJson.getString("nProt")); // Corrigido: setProtocoloNfe em vez de setProtocoloNFe
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
        
        // Identificação da NF-e
        JSONObject identNFe = new JSONObject();
        identNFe.put("cUF", getCodigoUF(config.getEmitenteUf())); // Corrigido: getEmitenteUf() em vez de getEmitenteUF()
        identNFe.put("cNF", String.format("%08d", venda.getNumeroNfe())); // Corrigido: getNumeroNfe() em vez de getNumeroNFe()
        identNFe.put("natOp", config.getNaturezaOperacao());
        identNFe.put("indPag", "0"); // 0=À vista, 1=A prazo, 2=Outros
        identNFe.put("mod", "55"); // Modelo NF-e
        identNFe.put("serie", config.getSerieNfe()); // Corrigido: getSerieNfe() em vez de getSerieNFe()
        identNFe.put("nNF", venda.getNumeroNfe()); // Corrigido: getNumeroNfe() em vez de getNumeroNFe()
        identNFe.put("dhEmi", new Date().toInstant().toString()); // Usando Date
        identNFe.put("tpNF", "1"); // 1=Saída
        identNFe.put("idDest", "1"); // 1=Operação interna
        identNFe.put("cMunFG", empresa.getCmun());
        identNFe.put("tpImp", "1"); // 1=Retrato
        identNFe.put("tpEmis", "1"); // 1=Normal
        identNFe.put("cDV", "1"); // Dígito verificador
        identNFe.put("tpAmb", config.getWebserviceAmbiente());
        identNFe.put("finNFe", config.getFinalidadeEmissao());
        identNFe.put("indFinal", config.getConsumidorFinal());
        identNFe.put("indPres", config.getPresencaComprador());
        identNFe.put("procEmi", "0"); // 0=Emissão própria
        identNFe.put("verProc", "1.0");
        
        // Emitente
        JSONObject emitente = new JSONObject();
        emitente.put("CNPJ", empresa.getCnpj());
        emitente.put("xNome", empresa.getXnome());
        emitente.put("xFant", empresa.getXfant());
        emitente.put("IE", empresa.getIe());
        emitente.put("CRT", empresa.getCrt());
        
        JSONObject enderEmit = new JSONObject();
        enderEmit.put("xLgr", empresa.getXlgr());
        enderEmit.put("nro", empresa.getNro());
        enderEmit.put("xCpl", empresa.getXcpl() != null ? empresa.getXcpl() : "");
        enderEmit.put("xBairro", empresa.getXbairro());
        enderEmit.put("cMun", empresa.getCmun());
        enderEmit.put("xMun", empresa.getXmun());
        enderEmit.put("UF", empresa.getUf());
        enderEmit.put("CEP", empresa.getCep());
        enderEmit.put("cPais", empresa.getCpais());
        enderEmit.put("xPais", empresa.getXpais());
        enderEmit.put("fone", empresa.getFone() != null ? empresa.getFone() : "");
        
        emitente.put("enderEmit", enderEmit);
        
        // Destinatário
        JSONObject destinatario = new JSONObject();
        Cliente cliente = venda.getClienteId(); // Corrigido: getClienteId() em vez de getCliente()
        if (cliente.getCpf() != null && !cliente.getCpf().isEmpty()) {
            destinatario.put("CPF", cliente.getCpf());
        } else {
            destinatario.put("CNPJ", cliente.getCnpj());
        }
        destinatario.put("xNome", cliente.getXnome());
        destinatario.put("indIEDest", cliente.getIndiedest()); // Corrigido: getIndiedest() em vez de getIndIEDest()
        
        JSONObject enderDest = new JSONObject();
        enderDest.put("xLgr", cliente.getXlgr());
        enderDest.put("nro", cliente.getNro());
        enderDest.put("xCpl", cliente.getXcpl() != null ? cliente.getXcpl() : "");
        enderDest.put("xBairro", cliente.getXbairro());
        enderDest.put("cMun", cliente.getCmun());
        enderDest.put("xMun", cliente.getXmun());
        enderDest.put("UF", cliente.getUf());
        enderDest.put("CEP", cliente.getCep());
        enderDest.put("cPais", "1058");
        enderDest.put("xPais", "Brasil");
        enderDest.put("fone", cliente.getFone() != null ? cliente.getFone() : "");
        
        destinatario.put("enderDest", enderDest);
        
        // Produtos
        JSONArray produtosArray = new JSONArray();
        for (ItemVenda item : venda.getItemVendaCollection()) { // Corrigido: getItemVendaCollection() em vez de getItens()
            Produto produto = item.getProdutoId(); // Corrigido: getProdutoId() em vez de getProduto()
            
            JSONObject prod = new JSONObject();
            prod.put("cProd", produto.getCprod());
            prod.put("cEAN", produto.getCean() != null ? produto.getCean() : "SEM GTIN");
            prod.put("xProd", produto.getXprod());
            prod.put("NCM", produto.getNcm());
            prod.put("CFOP", produto.getCfop());
            prod.put("uCom", produto.getUcom());
            prod.put("qCom", item.getQuantidade().toString());
            prod.put("vUnCom", item.getValorUnitario().toString());
            prod.put("vProd", item.getValorTotal().toString());
            prod.put("cEANTrib", produto.getCean() != null ? produto.getCean() : "SEM GTIN");
            prod.put("uTrib", produto.getUcom());
            prod.put("qTrib", item.getQuantidade().toString());
            prod.put("vUnTrib", item.getValorUnitario().toString());
            prod.put("indTot", "1");
            
            // Impostos
            JSONObject imposto = new JSONObject();
            JSONObject icms = new JSONObject();
            JSONObject icms00 = new JSONObject();
            icms00.put("orig", produto.getOrig());
            icms00.put("CST", produto.getCstIcms());
            icms00.put("modBC", produto.getModbcIcms());
            icms00.put("vBC", item.getValorTotal().toString());
            icms00.put("pICMS", produto.getPicms().toString());
            icms00.put("vICMS", item.getValorTotal().multiply(produto.getPicms().divide(new BigDecimal("100"))).toString());
            
            icms.put("ICMS00", icms00);
            
            JSONObject pis = new JSONObject();
            JSONObject pisAliq = new JSONObject();
            pisAliq.put("CST", produto.getCstPis());
            pisAliq.put("vBC", item.getValorTotal().toString());
            pisAliq.put("pPIS", produto.getPpis().toString());
            pisAliq.put("vPIS", item.getValorTotal().multiply(produto.getPpis().divide(new BigDecimal("100"))).toString());
            
            pis.put("PISAliq", pisAliq);
            
            JSONObject cofins = new JSONObject();
            JSONObject cofinsAliq = new JSONObject();
            cofinsAliq.put("CST", produto.getCstCofins());
            cofinsAliq.put("vBC", item.getValorTotal().toString());
            cofinsAliq.put("pCOFINS", produto.getPcofins().toString());
            cofinsAliq.put("vCOFINS", item.getValorTotal().multiply(produto.getPcofins().divide(new BigDecimal("100"))).toString());
            
            cofins.put("COFINSAliq", cofinsAliq);
            
            imposto.put("ICMS", icms);
            imposto.put("PIS", pis);
            imposto.put("COFINS", cofins);
            
            prod.put("imposto", imposto);
            
            produtosArray.put(prod);
        }
        
        // Total
        JSONObject total = new JSONObject();
        JSONObject icmsTot = new JSONObject();
        icmsTot.put("vBC", venda.getValorTotal().toString());
        icmsTot.put("vICMS", venda.getValorTotal().multiply(new BigDecimal("7.00")).divide(new BigDecimal("100")).toString());
        icmsTot.put("vBCST", "0.00");
        icmsTot.put("vST", "0.00");
        icmsTot.put("vProd", venda.getValorTotal().toString());
        icmsTot.put("vFrete", "0.00");
        icmsTot.put("vSeg", "0.00");
        icmsTot.put("vDesc", "0.00");
        icmsTot.put("vII", "0.00");
        icmsTot.put("vIPI", "0.00");
        icmsTot.put("vPIS", venda.getValorTotal().multiply(new BigDecimal("1.65")).divide(new BigDecimal("100")).toString());
        icmsTot.put("vCOFINS", venda.getValorTotal().multiply(new BigDecimal("7.60")).divide(new BigDecimal("100")).toString());
        icmsTot.put("vOutro", "0.00");
        icmsTot.put("vNF", venda.getValorTotal().toString());
        
        total.put("ICMSTot", icmsTot);
        
        // Montar NF-e completa
        nfeJson.put("identificacao", identNFe);
        nfeJson.put("emitente", emitente);
        nfeJson.put("destinatario", destinatario);
        nfeJson.put("produtos", produtosArray);
        nfeJson.put("total", total);
        
        return nfeJson;
    }
    
    private String enviarParaServidor(String urlString, String json) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
    
    private String getCodigoUF(String uf) {
        switch (uf.toUpperCase()) {
            case "AC": return "12";
            case "AL": return "27";
            case "AM": return "13";
            case "AP": return "16";
            case "BA": return "29";
            case "CE": return "23";
            case "DF": return "53";
            case "ES": return "32";
            case "GO": return "52";
            case "MA": return "21";
            case "MG": return "31";
            case "MS": return "50";
            case "MT": return "51";
            case "PA": return "15";
            case "PB": return "25";
            case "PE": return "26";
            case "PI": return "22";
            case "PR": return "41";
            case "RJ": return "33";
            case "RN": return "24";
            case "RO": return "11";
            case "RR": return "14";
            case "RS": return "43";
            case "SC": return "42";
            case "SE": return "28";
            case "SP": return "35";
            case "TO": return "17";
            default: return "29"; // BA como padrão
        }
    }
    
    // Getters e Setters
    public List<Venda> getVendas() { return vendas; }
    public void setVendas(List<Venda> vendas) { this.vendas = vendas; }
    
    public Venda getVenda() { return venda; }
    public void setVenda(Venda venda) { this.venda = venda; }
    
    public List<Cliente> getClientes() { return clientes; }
    public void setClientes(List<Cliente> clientes) { this.clientes = clientes; }
    
    public List<Produto> getProdutos() { return produtos; }
    public void setProdutos(List<Produto> produtos) { this.produtos = produtos; }
    
    public Cliente getClienteSelecionado() { 
        if (clienteSelecionado != null && venda != null) {
            venda.setClienteId(clienteSelecionado);
        }
        return clienteSelecionado; 
    }
    
    public void setClienteSelecionado(Cliente clienteSelecionado) { 
        this.clienteSelecionado = clienteSelecionado;
        if (venda != null) {
            venda.setClienteId(clienteSelecionado);
        }
    }
    
    public Produto getProdutoSelecionado() { return produtoSelecionado; }
    public void setProdutoSelecionado(Produto produtoSelecionado) { this.produtoSelecionado = produtoSelecionado; }
    
    public ItemVenda getItemVenda() { return itemVenda; }
    public void setItemVenda(ItemVenda itemVenda) { this.itemVenda = itemVenda; }
    
    public boolean isEditando() { return editando; }
    public void setEditando(boolean editando) { this.editando = editando; }
}