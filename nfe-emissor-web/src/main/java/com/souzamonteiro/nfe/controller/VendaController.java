package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.*;
import com.souzamonteiro.nfe.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@ViewScoped
public class VendaController implements Serializable {
    
    private VendaDAO vendaDAO = new VendaDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();
    private ProdutoDAO produtoDAO = new ProdutoDAO();
    private ConfiguracaoDAO configuracaoDAO = new ConfiguracaoDAO();
    private EmpresaDAO empresaDAO = new EmpresaDAO();
    
    private List<Venda> vendas;
    private Venda venda;
    private List<Cliente> clientes;
    private List<Produto> produtos;
    private Cliente clienteSelecionado;
    private Produto produtoSelecionado;
    private ItemVenda itemVenda;
    private boolean editando;
    
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
        venda.setDataVenda(LocalDateTime.now());
        itemVenda = new ItemVenda();
        editando = true;
    }
    
    public void carregarClientes() {
        clientes = clienteDAO.findAtivos();
    }
    
    public void carregarProdutos() {
        produtos = produtoDAO.findAtivos();
    }
    
    public void adicionarItem() {
        if (produtoSelecionado != null && itemVenda.getQuantidade() != null) {
            // Verificar se produto já está na venda
            for (ItemVenda item : venda.getItens()) {
                if (item.getProduto().getId().equals(produtoSelecionado.getId())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, 
                        "Aviso", "Produto já adicionado à venda."));
                    return;
                }
            }
            
            ItemVenda novoItem = new ItemVenda();
            novoItem.setProduto(produtoSelecionado);
            novoItem.setQuantidade(itemVenda.getQuantidade());
            novoItem.setValorUnitario(produtoSelecionado.getVuncom());
            
            venda.adicionarItem(novoItem);
            
            // Limpar seleção
            produtoSelecionado = null;
            itemVenda = new ItemVenda();
            
            // Atualizar componente via AJAX
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:vendaItens");
        }
    }
    
    public void removerItem(ItemVenda item) {
        venda.removerItem(item);
        FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("form:vendaItens");
    }
    
    public void finalizarVenda() {
        if (venda.getCliente() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Selecione um cliente."));
            return;
        }
        
        if (venda.getItens().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Adicione pelo menos um item à venda."));
            return;
        }
        
        try {
            // Gerar número da NF-e
            Integer numeroNFe = vendaDAO.getProximoNumeroNFe();
            venda.setNumeroNFe(numeroNFe);
            
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
                venda.setChaveNFe(respostaJson.getString("chave"));
                venda.setProtocoloNFe(respostaJson.getString("nProt"));
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
        // Implementação do JSON da NF-e (mantida igual)
        JSONObject nfeJson = new JSONObject();
        // ... resto da implementação igual ao código anterior
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
        switch (uf) {
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
    
    public Cliente getClienteSelecionado() { return clienteSelecionado; }
    public void setClienteSelecionado(Cliente clienteSelecionado) { this.clienteSelecionado = clienteSelecionado; }
    
    public Produto getProdutoSelecionado() { return produtoSelecionado; }
    public void setProdutoSelecionado(Produto produtoSelecionado) { this.produtoSelecionado = produtoSelecionado; }
    
    public ItemVenda getItemVenda() { return itemVenda; }
    public void setItemVenda(ItemVenda itemVenda) { this.itemVenda = itemVenda; }
    
    public boolean isEditando() { return editando; }
    public void setEditando(boolean editando) { this.editando = editando; }
}