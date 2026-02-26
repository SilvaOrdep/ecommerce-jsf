package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.*;
import com.souzamonteiro.nfe.model.*;
import com.souzamonteiro.nfe.service.NFeService;
import com.souzamonteiro.nfe.service.PixService;
import com.souzamonteiro.nfe.util.GeradorQRCodePix;

import org.primefaces.PrimeFaces;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ManagedBean
@ViewScoped
public class VendaController implements Serializable {

    private transient VendaDAO vendaDAO = new VendaDAO();
    private transient ClienteDAO clienteDAO = new ClienteDAO();
    private transient ProdutoDAO produtoDAO = new ProdutoDAO();
    private transient ConfiguracaoDAO configuracaoDAO = new ConfiguracaoDAO();
    private transient EmpresaDAO empresaDAO = new EmpresaDAO();
    private transient NFeService nfeService = new NFeService();

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
            novoItem.setDataCriacao(new Date());

            novoItem.setVendaId(venda);

            venda.getItemVendaCollection().add(novoItem);

            calcularTotalVenda();

            produtoSelecionado = null;
            itemVenda = new ItemVenda();
            itemVenda.setQuantidade(BigDecimal.ONE);

            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds()
                    .add("form:vendaItens");
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds()
                    .add("form:totalVenda");
        }
    }

    public void removerItem(ItemVenda item) {
        if (venda.getItemVendaCollection() != null) {
            venda.getItemVendaCollection().remove(item);
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

            // Emitir NF-e usando o serviço
            NFeService.NFeEmissaoResult resultado = nfeService.emitirNFe(venda);

            if (resultado.isSucesso()) {
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
                                "Sucesso", resultado.getMensagem()));

                carregarVendas();
            } else {
                venda.setStatus("ERRO");
                venda.setDataAtualizacao(new Date());
                vendaDAO.save(venda);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erro", resultado.getMensagem()));
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

    // Métodos de emissão de NF-e removidos - agora usam NFeService

    // Getters e Setters
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