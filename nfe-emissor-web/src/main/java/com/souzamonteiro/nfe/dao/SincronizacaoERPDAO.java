package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Produto;
import com.souzamonteiro.nfe.service.integracaoerp.IntegravelComERP;
import com.souzamonteiro.nfe.service.integracaoerp.ServicoIntegracaoERP;
import org.json.JSONArray;
import org.json.JSONObject;
import java.math.BigDecimal;

public class SincronizacaoERPDAO {

    private IntegravelComERP erpService = new ServicoIntegracaoERP();
    private ProdutoDAO produtoDAO = new ProdutoDAO();

    /**
     * Sincroniza produtos do ERP com o banco local
     */
    public boolean sincronizarProdutos() {
        try {
            JSONArray produtosERP = erpService.buscarProdutos();

            for (int i = 0; i < produtosERP.length(); i++) {
                JSONObject jsonProduto = produtosERP.getJSONObject(i);

                Produto produto = converterJsonParaProduto(jsonProduto);

                produtoDAO.save(produto);
            }

            System.out.println("Sincronização concluída: " + produtosERP.length() + " produtos");
            return true;

        } catch (Exception e) {
            System.err.println("Erro ao sincronizar produtos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Converte JSON da API do ERP para entidade Produto
     * 
     * Mapeamento dos campos da API:
     * - cdProduto → id
     * - codigoBarras → cprod e cean
     * - descricao → xprod
     * - vlrOferta (com desconto) ou vlrTabela → vuncom
     * - qtdEstoque → qcom
     * - inativo → ativo (invertido)
     */
    private Produto converterJsonParaProduto(JSONObject json) {
        // ===== CAMPOS OBRIGATÓRIOS DO CONSTRUTOR =====
        // Produto(Integer id, String cprod, String xprod, String ncm, String cfop,
        // String ucom, BigDecimal vuncom)

        // 1. ID do produto no ERP
        Integer id = json.optInt("cdProduto", 0);
        if (id == 0) {
            id = null; // Deixa JPA gerar ID automaticamente
        }

        // 2. Código do produto (usar código de barras)
        String cprod = json.optString("codigoBarras", "").trim();
        if (cprod.isEmpty()) {
            cprod = "PROD" + json.optInt("cdProduto");
        }

        // 3. Descrição do produto
        String xprod = json.optString("descricao", "Produto sem descrição");

        // 4. NCM - Nomenclatura Comum do Mercosul (a API não retorna, usar padrão para
        // MEDICAMENTOS)
        String ncm = "30049099"; // NCM genérico para medicamentos não especificados

        // 5. CFOP - Código Fiscal de Operações (venda dentro do estado)
        String cfop = "5102"; // 5102 = Venda de mercadoria adquirida de terceiros

        // 6. Unidade comercial
        String ucom = "UN"; // Unidade padrão

        // 7. Valor unitário comercial (priorizar preço com desconto)
        BigDecimal vuncom;
        double vlrOferta = json.optDouble("vlrOferta", 0);
        double vlrTabela = json.optDouble("vlrTabela", 0);

        if (vlrOferta > 0) {
            vuncom = new BigDecimal(String.valueOf(vlrOferta));
        } else if (vlrTabela > 0) {
            vuncom = new BigDecimal(String.valueOf(vlrTabela));
        } else {
            vuncom = BigDecimal.ZERO;
        }

        // Criar produto com construtor obrigatório
        Produto produto = new Produto(id, cprod, xprod, ncm, cfop, ucom, vuncom);

        // ===== CAMPOS ADICIONAIS =====

        // Código EAN (código de barras)
        produto.setCean(cprod);

        // Quantidade em estoque
        double qtdEstoque = json.optDouble("qtdEstoque", 1.0);
        produto.setQcom(new BigDecimal(String.valueOf(qtdEstoque)));

        // Valor do produto (preço * quantidade para 1 unidade)
        produto.setVprod(vuncom);

        // Campos tributáveis (espelhar os comerciais)
        produto.setCeantrib(cprod);
        produto.setUtrib(ucom);
        produto.setQtrib(produto.getQcom());
        produto.setVuntrib(vuncom);

        // Indicador de composição do valor total da NF-e
        produto.setIndtot("1"); // 1 = Valor do item compõe total da NF-e

        // ===== IMPOSTOS (valores padrão para medicamentos) =====

        // Origem da mercadoria
        produto.setOrig("0"); // 0 = Nacional

        // ICMS - Imposto sobre Circulação de Mercadorias
        produto.setCstIcms("00"); // 00 = Tributada integralmente
        produto.setModbcIcms("0"); // 0 = Margem Valor Agregado (MVA)
        produto.setPicms(new BigDecimal("7.00")); // Alíquota 7%

        // PIS - Programa de Integração Social
        produto.setCstPis("01"); // 01 = Operação Tributável com Alíquota Básica
        produto.setPpis(new BigDecimal("1.65")); // Alíquota 1.65%

        // COFINS - Contribuição para Financiamento da Seguridade Social
        produto.setCstCofins("01"); // 01 = Operação Tributável com Alíquota Básica
        produto.setPcofins(new BigDecimal("7.60")); // Alíquota 7.6%

        // Status do produto (inverter campo "inativo" da API)
        boolean inativo = json.optBoolean("inativo", false);
        produto.setAtivo(!inativo);

        return produto;
    }
}