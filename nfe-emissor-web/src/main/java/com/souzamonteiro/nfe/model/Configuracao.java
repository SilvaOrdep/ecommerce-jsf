/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.souzamonteiro.nfe.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author roberto
 */
@Entity
@Table(name = "configuracao")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Configuracao.findAll", query = "SELECT c FROM Configuracao c"),
    @NamedQuery(name = "Configuracao.findById", query = "SELECT c FROM Configuracao c WHERE c.id = :id"),
    @NamedQuery(name = "Configuracao.findByWebserviceUf", query = "SELECT c FROM Configuracao c WHERE c.webserviceUf = :webserviceUf"),
    @NamedQuery(name = "Configuracao.findByEmitenteUf", query = "SELECT c FROM Configuracao c WHERE c.emitenteUf = :emitenteUf"),
    @NamedQuery(name = "Configuracao.findByWebserviceAmbiente", query = "SELECT c FROM Configuracao c WHERE c.webserviceAmbiente = :webserviceAmbiente"),
    @NamedQuery(name = "Configuracao.findByCaminhoSchemas", query = "SELECT c FROM Configuracao c WHERE c.caminhoSchemas = :caminhoSchemas"),
    @NamedQuery(name = "Configuracao.findByCaminhoCertificado", query = "SELECT c FROM Configuracao c WHERE c.caminhoCertificado = :caminhoCertificado"),
    @NamedQuery(name = "Configuracao.findBySenhaCertificado", query = "SELECT c FROM Configuracao c WHERE c.senhaCertificado = :senhaCertificado"),
    @NamedQuery(name = "Configuracao.findByCaminhoXml", query = "SELECT c FROM Configuracao c WHERE c.caminhoXml = :caminhoXml"),
    @NamedQuery(name = "Configuracao.findByGerarPdf", query = "SELECT c FROM Configuracao c WHERE c.gerarPdf = :gerarPdf"),
    @NamedQuery(name = "Configuracao.findByPortaServidor", query = "SELECT c FROM Configuracao c WHERE c.portaServidor = :portaServidor"),
    @NamedQuery(name = "Configuracao.findBySerieNfe", query = "SELECT c FROM Configuracao c WHERE c.serieNfe = :serieNfe"),
    @NamedQuery(name = "Configuracao.findByNumeroNfe", query = "SELECT c FROM Configuracao c WHERE c.numeroNfe = :numeroNfe"),
    @NamedQuery(name = "Configuracao.findByNaturezaOperacao", query = "SELECT c FROM Configuracao c WHERE c.naturezaOperacao = :naturezaOperacao"),
    @NamedQuery(name = "Configuracao.findByFinalidadeEmissao", query = "SELECT c FROM Configuracao c WHERE c.finalidadeEmissao = :finalidadeEmissao"),
    @NamedQuery(name = "Configuracao.findByConsumidorFinal", query = "SELECT c FROM Configuracao c WHERE c.consumidorFinal = :consumidorFinal"),
    @NamedQuery(name = "Configuracao.findByPresencaComprador", query = "SELECT c FROM Configuracao c WHERE c.presencaComprador = :presencaComprador"),
    @NamedQuery(name = "Configuracao.findByDataCriacao", query = "SELECT c FROM Configuracao c WHERE c.dataCriacao = :dataCriacao"),
    @NamedQuery(name = "Configuracao.findByDataAtualizacao", query = "SELECT c FROM Configuracao c WHERE c.dataAtualizacao = :dataAtualizacao")})
public class Configuracao implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @Column(name = "webservice_uf")
    private String webserviceUf;
    @Basic(optional = false)
    @Column(name = "emitente_uf")
    private String emitenteUf;
    @Basic(optional = false)
    @Column(name = "webservice_ambiente")
    private String webserviceAmbiente;
    @Basic(optional = false)
    @Column(name = "caminho_schemas")
    private String caminhoSchemas;
    @Basic(optional = false)
    @Column(name = "caminho_certificado")
    private String caminhoCertificado;
    @Basic(optional = false)
    @Column(name = "senha_certificado")
    private String senhaCertificado;
    @Basic(optional = false)
    @Column(name = "caminho_xml")
    private String caminhoXml;
    @Column(name = "gerar_pdf")
    private String gerarPdf;
    @Column(name = "porta_servidor")
    private String portaServidor;
    @Column(name = "serie_nfe")
    private Integer serieNfe;
    @Column(name = "numero_nfe")
    private Integer numeroNfe;
    @Column(name = "natureza_operacao")
    private String naturezaOperacao;
    @Column(name = "finalidade_emissao")
    private String finalidadeEmissao;
    @Column(name = "consumidor_final")
    private String consumidorFinal;
    @Column(name = "presenca_comprador")
    private String presencaComprador;
    @Column(name = "data_criacao")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCriacao;
    @Column(name = "data_atualizacao")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAtualizacao;

    public Configuracao() {
    }

    public Configuracao(Integer id) {
        this.id = id;
    }

    public Configuracao(Integer id, String webserviceUf, String emitenteUf, String webserviceAmbiente, String caminhoSchemas, String caminhoCertificado, String senhaCertificado, String caminhoXml) {
        this.id = id;
        this.webserviceUf = webserviceUf;
        this.emitenteUf = emitenteUf;
        this.webserviceAmbiente = webserviceAmbiente;
        this.caminhoSchemas = caminhoSchemas;
        this.caminhoCertificado = caminhoCertificado;
        this.senhaCertificado = senhaCertificado;
        this.caminhoXml = caminhoXml;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getWebserviceUf() {
        return webserviceUf;
    }

    public void setWebserviceUf(String webserviceUf) {
        this.webserviceUf = webserviceUf;
    }

    public String getEmitenteUf() {
        return emitenteUf;
    }

    public void setEmitenteUf(String emitenteUf) {
        this.emitenteUf = emitenteUf;
    }

    public String getWebserviceAmbiente() {
        return webserviceAmbiente;
    }

    public void setWebserviceAmbiente(String webserviceAmbiente) {
        this.webserviceAmbiente = webserviceAmbiente;
    }

    public String getCaminhoSchemas() {
        return caminhoSchemas;
    }

    public void setCaminhoSchemas(String caminhoSchemas) {
        this.caminhoSchemas = caminhoSchemas;
    }

    public String getCaminhoCertificado() {
        return caminhoCertificado;
    }

    public void setCaminhoCertificado(String caminhoCertificado) {
        this.caminhoCertificado = caminhoCertificado;
    }

    public String getSenhaCertificado() {
        return senhaCertificado;
    }

    public void setSenhaCertificado(String senhaCertificado) {
        this.senhaCertificado = senhaCertificado;
    }

    public String getCaminhoXml() {
        return caminhoXml;
    }

    public void setCaminhoXml(String caminhoXml) {
        this.caminhoXml = caminhoXml;
    }

    public String getGerarPdf() {
        return gerarPdf;
    }

    public void setGerarPdf(String gerarPdf) {
        this.gerarPdf = gerarPdf;
    }

    public String getPortaServidor() {
        return portaServidor;
    }

    public void setPortaServidor(String portaServidor) {
        this.portaServidor = portaServidor;
    }

    public Integer getSerieNfe() {
        return serieNfe;
    }

    public void setSerieNfe(Integer serieNfe) {
        this.serieNfe = serieNfe;
    }

    public Integer getNumeroNfe() {
        return numeroNfe;
    }

    public void setNumeroNfe(Integer numeroNfe) {
        this.numeroNfe = numeroNfe;
    }

    public String getNaturezaOperacao() {
        return naturezaOperacao;
    }

    public void setNaturezaOperacao(String naturezaOperacao) {
        this.naturezaOperacao = naturezaOperacao;
    }

    public String getFinalidadeEmissao() {
        return finalidadeEmissao;
    }

    public void setFinalidadeEmissao(String finalidadeEmissao) {
        this.finalidadeEmissao = finalidadeEmissao;
    }

    public String getConsumidorFinal() {
        return consumidorFinal;
    }

    public void setConsumidorFinal(String consumidorFinal) {
        this.consumidorFinal = consumidorFinal;
    }

    public String getPresencaComprador() {
        return presencaComprador;
    }

    public void setPresencaComprador(String presencaComprador) {
        this.presencaComprador = presencaComprador;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Date getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(Date dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Configuracao)) {
            return false;
        }
        Configuracao other = (Configuracao) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.souzamonteiro.nfe.model.Configuracao[ id=" + id + " ]";
    }
    
}
