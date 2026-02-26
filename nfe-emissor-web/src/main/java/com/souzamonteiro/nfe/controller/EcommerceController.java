/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.*;
import com.souzamonteiro.nfe.model.*;
import com.souzamonteiro.nfe.service.PixService;
import com.souzamonteiro.nfe.service.NFeService;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

@ManagedBean(name = "ecommerceController")
@SessionScoped
public class EcommerceController implements Serializable {

  private static final long serialVersionUID = 1L;

  private transient ProdutoDAO produtoDAO = new ProdutoDAO();
  private transient VendaDAO vendaDAO = new VendaDAO();
  private transient ConfiguracaoDAO configuracaoDAO = new ConfiguracaoDAO();
  private transient EmpresaDAO empresaDAO = new EmpresaDAO();
  private transient NFeService nfeService = new NFeService();

  private Venda vendaEcommerce;

  private List<ItemCarrinho> carrinho;

  private Cliente clienteLogado;

  private List<Produto> produtosDisponiveis;

  private Map<Integer, BigDecimal> quantidadePorProduto = new HashMap<>();

  @PostConstruct
  public void init() {
    this.carrinho = new ArrayList<>();
    this.quantidadePorProduto = new HashMap<>();
    this.clienteLogado = recuperarClienteLogado();
    this.produtosDisponiveis = produtoDAO.findAtivos();

    // Inicializar quantidade padrão como 1 para cada produto
    if (produtosDisponiveis != null) {
      for (Produto produto : produtosDisponiveis) {
        quantidadePorProduto.put(produto.getId(), BigDecimal.ONE);
      }
    }

    // Sempre criar uma nova venda para o e-commerce (cliente será setado depois se
    // necessário)
    criarNovaVendaEcommerce();
  }

  /**
   * Recupera o cliente logado da sessão
   */
  private Cliente recuperarClienteLogado() {
    try {
      // Recuperar usuário da sessão
      FacesContext context = FacesContext.getCurrentInstance();
      if (context == null) {
        return null;
      }

      Usuario usuarioLogado = (Usuario) context.getExternalContext()
          .getSessionMap().get("usuarioLogado");

      if (usuarioLogado == null || usuarioLogado.getEmail() == null) {
        return null;
      }

      // Buscar cliente pelo email do usuário
      ClienteDAO clienteDAO = new ClienteDAO();
      return clienteDAO.findByEmail(usuarioLogado.getEmail());

    } catch (Exception e) {
      System.err.println("Erro ao recuperar cliente logado: " + e.getMessage());
      return null;
    }
  }

  /**
   * Cria uma nova venda vazia para o e-commerce
   */
  public void criarNovaVendaEcommerce() {
    vendaEcommerce = new Venda();
    vendaEcommerce.setDataVenda(new Date());
    vendaEcommerce.setStatus("PENDENTE");
    vendaEcommerce.setValorTotal(BigDecimal.ZERO);
    vendaEcommerce.setClienteId(clienteLogado);
    vendaEcommerce.setItemVendaCollection(new ArrayList<>());
    this.carrinho.clear();
  }

  /**
   * Navega para o carrinho
   */
  public String irParaCarrinho() {
    return "carrinho.xhtml?faces-redirect=true";
  }

  /**
   * Adicionar produto ao carrinho
   */
  public void adicionarAoCarrinho(Produto produto, Integer produtoId) {
    if (produto == null) {
      FacesContext.getCurrentInstance().addMessage(null,
          new FacesMessage(FacesMessage.SEVERITY_WARN,
              "Aviso", "Produto inválido"));
      return;
    }

    // Converter quantidade de forma segura (pode vir como String ou BigDecimal do
    // JSF)
    Object qtdObj = quantidadePorProduto.get(produtoId);
    BigDecimal quantidade = BigDecimal.ONE;

    if (qtdObj instanceof BigDecimal) {
      quantidade = (BigDecimal) qtdObj;
    } else if (qtdObj instanceof String) {
      try {
        quantidade = new BigDecimal((String) qtdObj);
      } catch (NumberFormatException | NullPointerException e) {
        quantidade = BigDecimal.ONE;
      }
    } else if (qtdObj == null) {
      quantidade = BigDecimal.ONE;
    }

    if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
      FacesContext.getCurrentInstance().addMessage(null,
          new FacesMessage(FacesMessage.SEVERITY_WARN,
              "Aviso", "Quantidade inválida"));
      return;
    }

    // Verificar se produto já está no carrinho
    for (ItemCarrinho item : carrinho) {
      if (item.getProduto().getId().equals(produto.getId())) {
        item.setQuantidade(item.getQuantidade().add(quantidade));
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Sucesso", "Quantidade atualizada no carrinho"));
        return;
      }
    }

    // Adicionar novo item
    carrinho.add(new ItemCarrinho(produto, quantidade));
    quantidadePorProduto.put(produtoId, BigDecimal.ONE); // Reset para 1 após adicionar
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Sucesso", produto.getXprod() + " adicionado ao carrinho"));
  }

  /**
   * Remover produto do carrinho
   */
  public void removerDoCarrinho(Produto produto) {
    carrinho.removeIf(item -> item.getProduto().getId().equals(produto.getId()));
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Sucesso", "Produto removido do carrinho"));
  }

  /**
   * Atualizar quantidade no carrinho
   */
  public void atualizarQuantidadeCarrinho(Produto produto, BigDecimal quantidade) {
    for (ItemCarrinho item : carrinho) {
      if (item.getProduto().getId().equals(produto.getId())) {
        if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
          removerDoCarrinho(produto);
        } else {
          item.setQuantidade(quantidade);
        }
        return;
      }
    }
  }

  /**
   * Calcular total do carrinho
   */
  public BigDecimal calcularTotalCarrinho() {
    BigDecimal total = BigDecimal.ZERO;
    for (ItemCarrinho item : carrinho) {
      total = total.add(item.getSubtotal());
    }
    return total;
  }

  /**
   * Limpar carrinho
   */
  public void limparCarrinho() {
    carrinho.clear();
    criarNovaVendaEcommerce();
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Sucesso", "Carrinho limpo"));
  }

  /**
   * Preparar checkout - converter carrinho para itemVenda
   */
  public String irParaCheckout() {
    if (carrinho.isEmpty()) {
      FacesContext.getCurrentInstance().addMessage(null,
          new FacesMessage(FacesMessage.SEVERITY_WARN,
              "Aviso", "Carrinho vazio"));
      return null;
    }

    // Verificar se vendaEcommerce foi inicializada
    if (vendaEcommerce == null) {
      criarNovaVendaEcommerce();
    }

    // Converter items do carrinho para ItemVenda
    vendaEcommerce.getItemVendaCollection().clear();

    for (ItemCarrinho itemCarrinho : carrinho) {
      ItemVenda itemVenda = new ItemVenda();
      itemVenda.setProdutoId(itemCarrinho.getProduto());
      itemVenda.setQuantidade(itemCarrinho.getQuantidade());
      itemVenda.setValorUnitario(itemCarrinho.getProduto().getVuncom());
      itemVenda.setValorTotal(itemCarrinho.getSubtotal());
      itemVenda.setDataCriacao(new Date());
      itemVenda.setVendaId(vendaEcommerce);

      vendaEcommerce.getItemVendaCollection().add(itemVenda);
    }

    vendaEcommerce.setValorTotal(calcularTotalCarrinho());

    return "checkout.xhtml?faces-redirect=true";
  }

  /**
   * Finalizar compra e emitir NF-e
   */
  public String finalizarCompra() {
    try {
      if (vendaEcommerce.getClienteId() == null) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Erro", "Cliente não identificado"));
        return null;
      }

      if (vendaEcommerce.getItemVendaCollection().isEmpty()) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Erro", "Nenhum item na venda"));
        return null;
      }

      // Garantir referencia da venda nos itens e datas obrigatorias
      for (ItemVenda item : vendaEcommerce.getItemVendaCollection()) {
        if (item.getVendaId() == null) {
          item.setVendaId(vendaEcommerce);
        }
        if (item.getDataCriacao() == null) {
          item.setDataCriacao(new Date());
        }
      }

      vendaEcommerce.setClienteId(clienteLogado);
      if (vendaEcommerce.getDataVenda() == null) {
        vendaEcommerce.setDataVenda(new Date());
      }
      vendaEcommerce.setDataCriacao(new Date());

      Empresa empresa = empresaDAO.getEmpresa();
      Configuracao config = configuracaoDAO.getConfiguracao();

      if (config == null || empresa == null) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Erro", "Configure empresa e configurações antes de emitir NF-e."));
        return null;
      }

      Integer numeroNFe = config.getNumeroNfe();
      vendaEcommerce.setNumeroNfe(numeroNFe);

      // Salvar venda para persistir dados base
      vendaDAO.save(vendaEcommerce);

      NFeService.NFeEmissaoResult resultado = nfeService.emitirNFe(vendaEcommerce);

      if (resultado.isSucesso()) {
        vendaEcommerce.setStatus("EMITIDA");
        vendaEcommerce.setDataAtualizacao(new Date());

        String chavePix = PixService.gerarChavePixParaVenda(vendaEcommerce, empresa);
        if (PixService.isChavePixValida(chavePix)) {
          vendaEcommerce.setChavePix(chavePix);
        }

        vendaEcommerce.setChaveNfe(resultado.getChaveNfe());
        vendaEcommerce.setProtocoloNfe(resultado.getProtocoloNfe());

        vendaDAO.save(vendaEcommerce);

        config.setNumeroNfe(numeroNFe + 1);
        configuracaoDAO.save(config);

        // Limpar carrinho apos sucesso
        limparCarrinho();

        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Sucesso", "Compra finalizada e NF-e emitida com sucesso!"));

        return "index.xhtml?faces-redirect=true";
      }

      vendaEcommerce.setStatus("ERRO");
      vendaEcommerce.setDataAtualizacao(new Date());
      vendaDAO.save(vendaEcommerce);

      FacesContext.getCurrentInstance().addMessage(null,
          new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Erro", "Erro ao emitir NF-e. Venda salva como pendente."));

      return null;

    } catch (Exception e) {
      FacesContext.getCurrentInstance().addMessage(null,
          new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Erro", "Erro ao finalizar compra: " + e.getMessage()));
      return null;
    }
  }

  // Getters e Setters
  public Venda getVendaEcommerce() {
    return vendaEcommerce;
  }

  public void setVendaEcommerce(Venda vendaEcommerce) {
    this.vendaEcommerce = vendaEcommerce;
  }

  public List<ItemCarrinho> getCarrinho() {
    return carrinho;
  }

  public void setCarrinho(List<ItemCarrinho> carrinho) {
    this.carrinho = carrinho;
  }

  public Cliente getClienteLogado() {
    return clienteLogado;
  }

  public void setClienteLogado(Cliente clienteLogado) {
    this.clienteLogado = clienteLogado;
  }

  public List<Produto> getProdutosDisponiveis() {
    return produtosDisponiveis;
  }

  public void setProdutosDisponiveis(List<Produto> produtosDisponiveis) {
    this.produtosDisponiveis = produtosDisponiveis;
  }

  public int getQuantidadeItensCarrinho() {
    return carrinho.size();
  }

  public Map<Integer, BigDecimal> getQuantidadePorProduto() {
    return quantidadePorProduto;
  }

  public void setQuantidadePorProduto(Map<Integer, BigDecimal> quantidadePorProduto) {
    this.quantidadePorProduto = quantidadePorProduto;
  }
}
